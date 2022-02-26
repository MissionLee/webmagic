package pers.missionlee.chan.spider.parent;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.pojo.ParentInfo;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.AbstractPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-22 23:20
 */
public class ParentListPageProcessor extends AbstractPageProcessor {
    static Logger logger = LoggerFactory.getLogger(ParentListPageProcessor.class);
    // 从数据库 找到 parent id  = -1 未知  -2 某个parent的 作品的 sancode
    // 访问页面
//    List<String> parentIdStoredAll; // 已经保存完整的 parentId
    List<String> parentIdStoredPart;
    List<String> parentIdDone; // 本次解析完成 需要下载或整理的 parent id
    public List<String> sanCodeToCheck; // 当前不知道 是否属于parent的 sanCode    -1的
    public List<String> sanCodeChecked;// 属于某个 parent id 但是没有被处理的
    String artistName;
    boolean single;
    int downloaded;
    public static String PARENT_PREFIX = "https://chan.sankakucomplex.com/?tags=parent%3A";
    public static String PARENT_PREFIX_2 = "https://chan.sankakucomplex.com/?next=";
    public Map<String, String> storedMdePath;
    public String thisParentPath;
    List<String> relatedArtist;
    List<String> skipParentIdList;
    boolean startPage = true;
    String startPageSanCode;

    public void reset() {
        this.parentId = null;
        this.parentShowPageDealt = false;
        this.parentInfo = null;
        this.md5UrlPairs = null;
        this.md5Sequence = null;
        this.thisParentPath = null;
        this.startPageSanCode = null;
        this.startPage = true;
    }

    public ParentListPageProcessor(List<String> skipParentIdList, boolean single, String artistName, DataBaseService dataBaseService, DiskService diskService, SpiderSetting spiderSetting) {
        super(dataBaseService, diskService);
        this.artistName = artistName;
        this.single = single;
        if (single) {
            this.sanCodeToCheck = new ArrayList<>();
            this.parentIdStoredPart = new ArrayList<>();
        } else {
//            this.storedMdePath = diskService.getArtistFileMd5Path(artistName);
            this.storedMdePath = spiderSetting.initAllRelatedStoredFilesMd5(diskService, artistName);
            this.relatedArtist = new ArrayList<>();
            this.relatedArtist.add(artistName);
            init();
        }
        this.skipParentIdList = skipParentIdList;
        this.reset();
    }

    public int start;
    public int end;
    public CountDownLatch latch;

    public ParentListPageProcessor(List<String> skipParentIdList, boolean single, String artistName, DataBaseService dataBaseService, DiskService diskService, int start, int end, CountDownLatch latch, Logger log, SpiderSetting spiderSetting) {
        this(skipParentIdList, single, artistName, dataBaseService, diskService, spiderSetting);
        this.start = start;
        this.end = end;
        this.latch = latch;
        logger = log;

    }

    public void init() {
        // 作品parent 状态为
        // 查询这个 作者所有的 状态为  -1 未知 或 -2
        logger.info("初始化：查找 parent_id 为 null（空）  -1（未知）-2（属于某个作品集）的作品的sanCode");
        if (null == sanCodeToCheck)
            sanCodeToCheck = dataBaseService.getArtistParentSanCodeToCheck(this.artistName);
        if (null == sanCodeChecked) {
            sanCodeChecked = new ArrayList<>();
        }
        System.out.println(sanCodeToCheck);
        logger.info("初始化：查找未能存储完整的的 parent id  ： 状态 0 存储情况未知 状态 2 仅仅保存了信息  另外：状态1 表示能下载到的都下载了");
        this.parentIdStoredPart = dataBaseService.getArtistParentIdStoredPart(this.artistName);
        if (null == this.parentIdStoredPart) {
            this.parentIdStoredPart = new ArrayList<>();
        }
    }

    public String[] getCheckUrlsOfPartlyStored() {
        // TODO: 4/26/2021  此处没有考虑 https://chan.sankakucomplex.com/?next=  这个 前缀 
        if (null != parentIdStoredPart && parentIdStoredPart.size() > 0) {
            String[] urls = new String[parentIdStoredPart.size()];
            int size = parentIdStoredPart.size();
            for (int i = 0; i < size; i++) {
                urls[i] = PARENT_PREFIX + parentIdStoredPart.get(i);
            }
            return urls;
        } else {
            return null;
        }

    }

    public String getStartSanCodeUrl() {
        if (null != sanCodeToCheck && sanCodeToCheck.size() > 0) {
            return "https://chan.sankakucomplex.com/post/show/" + sanCodeToCheck.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void doProcess(Page page) {
        if (startPage) { // 此处用于处理访问到希望跳过的parentPool的情况：必须把最起始页面的sancode记录下来,进行makeAsSkip 操作
            startPage = false;
            String startUrl = page.getUrl().toString();
            this.startPageSanCode = startUrl.substring(startUrl.lastIndexOf("/") + 1);
        }
        // 情况1 自动从
        System.out.println(page.getUrl() + "_ParentDeal:" + parentShowPageDealt);
        String url = page.getUrl().toString();
        String pageString = page.getHtml().toString();
        if (pageString.contains("This post was deleted")) {
            // TODO: 6/10/2021 说明：此处对应一种情况，作品属于某个parent 后来被删除了，但是页面仍能找到parent 但是 parent的child里面 没有这个作品，导致这个作品每次作者更新都会被再查找到 
            logger.info("已经被删除的文件！！！his post was deleted ");
            String urllll = page.getUrl().toString();
            // https://chan.sankakucomplex.com/post/show/6138306
            String sanCodee = urllll.substring(url.lastIndexOf("/") + 1);
            dataBaseService.makeSanCodeDeleted(sanCodee);
            return;
        }
        if (pageString.contains("You lack the access rights required to view this content")) {
            logger.info("Vip的文件！！！his post was deleted ");
            String urllll = page.getUrl().toString();
            String sanCodee = urllll.substring(url.lastIndexOf("/") + 1);
            dataBaseService.makeSanCodeVip(sanCodee, artistName);
            return;
        }
        if (url.contains(PARENT_PREFIX) || url.contains(PARENT_PREFIX_2)) {
            processListPage(page);
        } else if (!parentShowPageDealt && url.contains("/show/")) {
            logger.info("检测到Show页面，此时Parent 信息还未处理，进行parent解析");
            String pageStringg = page.getHtml().toString();
            if (pageString.contains("This post belongs to") && pageString.contains("a parent post")) {
                // 如果当前页面是Parent中的子页面，将母页面加入下载列表
                logger.info("从当前页面解析ParentPage（为了递归找父级 找parent放在前面）:" + page.getUrl());
//            System.out.println("当前页面是某个Parent的子页面，跳转Parent页面");
                List<String> href = page.getHtml().$("#parent-preview + div").$("a", "href").all();
                page.addTargetRequest("https://chan.sankakucomplex.com" + href.get(0));
//            page.addTargetRequest("https://chan.sankakucomplex.com" + href.get(0));
            } else if (pageStringg.contains("This post has") && pageString.contains("child post")) {
                // 如果当前页面是Parent中的母页面，直接加入带下载列表
                logger.info("从当前页面解析Child :" + page.getUrl());
//            System.out.println("“有些”子作品没有记录作品信息（例如作者），所以交给ParentSpider的页面是“母作品”页面，而不是作品列表页面");
                String subFix = "";
                try {
                    subFix = processParentPage(page);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 有的作品
                    if (e instanceof IndexOutOfBoundsException) {
                        logger.info("检测到了 Vip 或者 删除内容");
                        String urlll = page.getUrl().toString();
                        // https://chan.sankakucomplex.com/post/show/6138306
                        String sanCode = urlll.substring(url.lastIndexOf("/") + 1);
                        dataBaseService.makeSanCodeVip(sanCode, artistName);
                    }
                    return;
                }
                if (skipParentIdList.contains(this.parentId)) {
                    logger.info("特殊作品，跳过：parentId:" + this.parentId + "___sanCode:" + startPageSanCode);
                    dataBaseService.makeSanCodeSkip(startPageSanCode, this.parentId);
                } else {
                    page.addTargetRequest("https://chan.sankakucomplex.com" + subFix);

                }

            } else {
                String sanCode = url.substring(url.lastIndexOf("/") + 1);
                this.sanCodeChecked.add(sanCode);
//                this.sanCodeChecked.add(sanCode);
                dataBaseService.updateParentId(sanCode, -3);
            }
        } else {
            logger.info("遇到需要补充下载的页面了！！！！！！！！！！！！！");
            processShowPage(page);
        }
    }

    public void processShowPage(Page page) {
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        try {
            // 1.提取target信息
            AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
            // 2.提取 ArtworkInfo 信息
            ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
            // 3.处理ArtworkInfo 信息
            formatArtworkInfoForSave(artworkInfo);

            List<String> artistTags = artworkInfo.getTagArtist();
            if (null != artistTags && artistTags.size() > 0) {
                for (int i = 0; i < artistTags.size(); i++) {
                    String iTagName = artistTags.get(i);
                    if (!this.relatedArtist.contains(iTagName)) {
                        Map<String, String> relatedArtworks = diskService.getArtistFileMd5Path(iTagName);
                        this.storedMdePath.putAll(relatedArtworks);
                    }
                }
            }

            // 5.下载文件
            boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
            if (download) {
                this.sanCodeChecked.add(artworkInfo.sanCode);
//            this.sanCodeChecked.add(artworkInfo.sanCode);
                logger.info("SanCode:" + artworkInfo.sanCode + " 下载完成，加入 checked目录");
                dataBaseService.saveArtworkInfo(artworkInfo);
                this.storedMdePath.put(
                        artworkInfo.fileName.substring(0, artworkInfo.fileName.indexOf(".")),
                        diskService.getParentPath(artworkInfo, artworkInfo.PBPrefix, artworkInfo.storePlace) + artworkInfo.fileSaveName);
                this.downloaded++;
            }
        } catch (Exception e) {
            // 有的作品
            if (e instanceof IndexOutOfBoundsException) {
                logger.info("检测到了 Vip 或者 删除内容");
                String url = page.getUrl().toString();
                // https://chan.sankakucomplex.com/post/show/6138306
                String sanCode = url.substring(url.lastIndexOf("/") + 1);
                dataBaseService.makeSanCodeVip(sanCode, artistName);
            }
        }


    }

    private void processListPage(Page page) {
        if (page.getUrl().toString().contains("20762077")) {
            return;
        }
        if (null == md5UrlPairs) {
            // TODO: 4/23/2021  ArrayList 是有顺序的，所以能够自动解决顺序问题
            this.md5UrlPairs = new HashMap<>();
            this.md5Sequence = new ArrayList<>();
        }

        List<String> hrefs = page.getHtml().$("#post-list").$(".content").$("span.thumb").$("a", "href").all();
        System.out.println(hrefs);
        List<String> fileNames = page.getHtml().$("#post-list").$(".content").$("span.thumb").$("a").$(".preview", "src").all();
        System.out.println(fileNames);
        for (int i = 0; i < hrefs.size(); i++) {
//            String sanCode = hrefs.get(i).substring(11);
            String fileName = fileNames.get(i).substring(fileNames.get(i).lastIndexOf("/") + 1);
            String md5 = fileName.substring(0, fileName.indexOf("."));
            this.md5UrlPairs.put(md5, hrefs.get(i));
            this.md5Sequence.add(md5);
        }
        ArtworkInfo artworkInfo = new ArtworkInfo();
        artworkInfo.fileName = "1.jpg";
        formatArtworkInfoForSave(artworkInfo);
        System.out.println(this.md5UrlPairs);
        System.out.println(this.md5Sequence);
//        System.out.println(taskController.codeNamePairs);
        //
//        for (int i = 0; i < hrefs.size(); i++) {
//            System.out.println("根据页面结构， href 与 fileNames 获得的 List： hrefs 和 fileNames 顺序一致，所以直接取巧处理");
//            String fileName = fileNames.get(i).substring(fileNames.get(i).lastIndexOf("/")+1);
//            taskController.bookParentInfo.getArtworkInfo().setFileName(fileName);
//        }
        List<String> nextPage = page.getHtml().$("#paginator").$(".pagination", "next-page-url").all();
        System.out.println("nexPage:" + nextPage + "  // " + nextPage.size());
        // !!!! 必须先处理完所有页面，不然这里没法定位文件顺序
        if (null != nextPage && !nextPage.isEmpty() && !StringUtils.isEmpty(nextPage.get(0))) { // 如果有下一页，先处理下一页
            String subFix = nextPage.get(0); // /?next=24650900&amp;tags=parent%3A24636293&amp;page=2
            page.addTargetRequest(SpiderUtils.BASE_URL + subFix);
        } else { // 如果没有下一页了，转移已有页面或加入下载列表
            this.thisParentPath = diskService.getParentPath(artworkInfo, artworkInfo.PBPrefix, artworkInfo.storePlace);
            System.out.println("生成的thisParentPath:" + this.thisParentPath);
            // TODO: 3/27/2021  根据已经完成的  taskController.codeNamePairs  转移文件，或新增下载页面
            this.parentInfo.poolNum = md5UrlPairs.size();
            this.parentInfo.storedNum = 0;
            Set<String> keyset = this.md5UrlPairs.keySet();
            List<String> md5list = new ArrayList<>();
            md5list.addAll(keyset);
            int size = md5list.size();
            List<String> tobeDownloadded = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String md5 = md5list.get(i);
                String url = this.md5UrlPairs.get(md5);
                if (!single && saveIfExists(md5)) {
//                    this.sanCodeChecked.remove(url.lastIndexOf("/")+1);
                    String sanCode = url.substring(url.lastIndexOf("/") + 1);
                    logger.info("SanCode:" + sanCode + " 因为已经存在并存储，加入 checked目录 parentId:" + parentId);
                    dataBaseService.updateParentId(sanCode, Integer.valueOf(parentId));
                    this.sanCodeChecked.add(sanCode);
//                    this.sanCodeChecked.add(sanCode);
                } else {
                    logger.info("(代码已经改动不添加这个页面了)添加页面：https://chan.sankakucomplex.com" + url);
                    tobeDownloadded.add("https://chan.sankakucomplex.com" + url);
                    // TODO: 4/25/2021 暂时不知道 什么原因 page.add 这一句没法正常执行，调用之后并不启动下载 
//                    page.addTargetRequest("https://chan.sankakucomplex.com"+url);
//                    Spider.create(this).addUrl("https://chan.sankakucomplex.com" + url).thread(1).run();
                }
            }
            if (tobeDownloadded.size() > 0) {
                String[] urls = new String[tobeDownloadded.size()];
                for (int i = 0; i < tobeDownloadded.size(); i++) {
                    urls[i] = tobeDownloadded.get(i);
                }
                Spider.create(this).addUrl(urls).thread(4).run();
            }
            // TODO: 4/25/2021 重要知识点， Map.foreach 方法 之中 page.addTargetRequest 方法不起效果
//            this.md5UrlPairs.forEach((String md5, String url) -> {
//                System.out.println("检查；"+md5+"ur;");
//                // 第一步， 已经放在正确的parent 未知的 md5 跳过
//                // 第二步， 已经存在的文件 转义未知
//                // 第三步， 剩余的文件 加入下载队列
//
//            });
        }

    }


    public boolean saveIfExists(String md5) {
        if (storedMdePath.containsKey(md5)) {
            System.out.println("saveIfExists: md5；" + md5);
            String storedPath = storedMdePath.get(md5);
            System.out.println(storedPath);
            if (storedPath.startsWith(thisParentPath)) {
                logger.info("此文件：" + md5 + " 已经存储在目标位置");
                return true;
            } else {
                String nowName = storedPath.substring(storedPath.lastIndexOf("/") + 1);
                if (nowName.contains("_")) {
                    nowName = nowName.substring(nowName.indexOf("_") + 1, nowName.length());
                }
                ArtworkInfo artworkInfo = new ArtworkInfo();
                artworkInfo.fileName = nowName;
                formatArtworkInfoForSave(artworkInfo);
                String parentPath = diskService.getParentPath(artworkInfo, artworkInfo.PBPrefix, artworkInfo.storePlace);
                try {
                    if (storedPath.contains("[")) {
                        FileUtils.copyFile(new File(storedPath), new File(parentPath + artworkInfo.fileSaveName));
                    } else {
                        FileUtils.moveFile(new File(storedPath), new File(parentPath + artworkInfo.fileSaveName));
                        this.storedMdePath.put(md5, parentPath + artworkInfo.fileSaveName);
                    }
                    logger.info("此文件：" + md5 + " 在作者的其他目录中,已经根据实际情况 移动或复制文件");
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        }
        return false;
    }

    public void formatArtworkInfoForSave(ArtworkInfo artworkInfo) {
        logger.info("保存信息前处理数据： parentId:" + this.parentId);
        artworkInfo.parentId = Integer.valueOf(this.parentId);
        artworkInfo.aimName = this.artistName;
        if (this.single) {
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.SINGLE_PARENT_BOOK.storePlace;
        } else {
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST_PARENT_BOOK.storePlace;
        }
        artworkInfo.PBPrefix = "P";
        String prefix = getSequencePrefixByMd5(artworkInfo.fileName.substring(0, artworkInfo.fileName.indexOf(".")));
        artworkInfo.fileSaveName = prefix + artworkInfo.fileName;
        if (null != parentInfo.parentArtworkInfo.getTagArtist() && parentInfo.parentArtworkInfo.getTagArtist().size() > 0) {
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
            if (null == artworkInfo.getTagArtist()) {
                artworkInfo.setTagArtist(parentInfo.parentArtworkInfo.getTagArtist());
            } else {
                artworkInfo.getTagArtist().addAll(parentInfo.parentArtworkInfo.getTagArtist());
            }
        } else {
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.UNKNOWN.artistType;
        }

    }

    public String getSequencePrefixByMd5(String md5) {
        // 0 1 2  size =3
        // 2 1 0
        int seq = -1;
        for (int i = 0; i < md5Sequence.size(); i++) {
            if (md5Sequence.get(i).equals(md5)) {
                seq = md5Sequence.size() - 1 - i;
            }
        }
        String prefix = "" + seq;
        int x = 4 - prefix.length();
        for (int i = 0; i < x; i++) {
            prefix = "0" + prefix;
        }
        return prefix + "_";
    }

    public String processParentPage(Page page) {
        parentShowPageDealt = true;
        this.parentInfo = new ParentInfo();
        Html html = page.getHtml();

        Target target = extractDownloadTargetInfoFromDetailPage(html);


        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        parentInfo.id = artworkInfo.sanCode;
        parentInfo.parentArtworkInfo = artworkInfo;
        parentInfo.single = false;
        parentInfo.storedArtist = this.artistName;
//        parentInfo.storedArtistId = diskService.getArtistIdByName(this.artistName);

        List<String> href = page.getHtml().$("#child-preview + div").$("a", "href").all();
        String subFix = href.get(0); // 例如： /?tags=parent%3A24787325
        String id = subFix.substring(16);
        this.parentId = id;

//        taskController.bookParentInfo.setId(Integer.valueOf(id));
        System.out.println("subFix / id:" + subFix + "_" + id);
        return subFix;
    }

    String parentId;
    boolean parentShowPageDealt = false;
    ParentInfo parentInfo;
    Map<String, String> md5UrlPairs;
    List<String> md5Sequence;

//    public void startMultiThread(int threadNum) throws InterruptedException {
//        CountDownLatch latch = new CountDownLatch(3);
//        threadNum = 3;
//        int all = sanCodeToCheck.size();
//        System.out.println("需要验证的总数为：" + all);
//        int size = new Double(Math.ceil(all * 1.0 / threadNum)).intValue();
//        int i = 0;
//        for (; i < all; i += size) {
//            int start = i;
//            int end = start + size - 1;
//            if (end >= all) {
//                end = all - 1;
//            }
//            startWithNewThread(start, end, latch);
//        }
//        latch.await();
//        System.out.println("ParentPageProcessorCountDownLatch:完成");
//
//
//    }

    public static ExecutorService executorService = Executors.newFixedThreadPool(3);

//    public void startWithNewThread(int start, int end, CountDownLatch latch) {
//        ParentListPageProcessor parentListPageProcessor = new ParentListPageProcessor(single, artistName, dataBaseService, diskService, start, end, latch, logger);
//        logger.info("submit");
//        new Thread(parentListPageProcessor).start();
////        executorService.submit(parentListPageProcessor);
//        logger.info("submit done");
//    }

    public void start() {
//
//        try {
//            this.startMultiThread(3);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        try {
            if (null != sanCodeToCheck && sanCodeToCheck.size() > 0) {

                for (int i = 0; i < sanCodeToCheck.size(); i++) {
                    String sanCode = sanCodeToCheck.get(i);
                    if (!sanCodeChecked.contains(sanCode)) {
                        logger.info(" " + this.artistName + "[" + i + "/" + sanCodeToCheck.size() + "]");
                        this.reset();
                        Spider.create(this).addUrl("https://chan.sankakucomplex.com/post/show/" + sanCode).thread(3).run();
                    } else {
                        logger.info("xxxxxxxxxxxxxxx 这个sancode 应该是被别的页面派出了：" + sanCode);
                    }
                }
            }
        } catch (Exception e) {

        } finally {
//            try {
//                Thread.sleep(30000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    public static String lock = "lock";

//    public void addToChecked(String sanCode) {
//        synchronized (lock) {
//            sanCodeChecked.add(sanCode);
//        }
//    }
//
//    public boolean checkContains(String sanCode) {
//        synchronized (lock) {
//            return sanCodeChecked.contains(sanCode);
//        }
//    }

    //    @Override
//    public void run() {
////        int start = this.start.get();
////        int end = this.end.get();
//        logger.info("aaaaaaaa:" + "_start:" + start + " end:" + end);
//
//        for (int i = start; i <= end; i++) {
//            try {
//                String sanCode = sanCodeToCheck.get(i);
//                if (!checkContains(sanCode)) {
//                    Spider.create(this).addUrl("https://chan.sankakucomplex.com/post/show/" + sanCode).thread(1).run();
//                } else {
//                    logger.info("xxxxxxxxxxxxxxx 这个sancode 应该是被别的页面派出了：" + sanCode);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//
//            }
//        }
//
//        latch.countDown();
//
//    }
    public static void main(String[] args) {
        String url = "https://chan.sankakucomplex.com/post/show/6138306";
        String sanCode = url.substring(url.lastIndexOf("/") + 1);
        System.out.println(sanCode);
    }


}
