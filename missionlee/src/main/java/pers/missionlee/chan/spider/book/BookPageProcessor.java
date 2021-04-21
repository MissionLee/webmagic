package pers.missionlee.chan.spider.book;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.pojo.BookInfo;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.AbstractPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-15 19:18
 */
public class BookPageProcessor extends AbstractPageProcessor {
    static Logger logger = LoggerFactory.getLogger(BookPageProcessor.class);
    String BOOK_PAGE_PREFIX = "https://beta.sankakucomplex.com/books/";
    String BOOK_INFO_RESTFUL_PREFIX = "https://capi-v2.sankakucomplex.com/pools/";
    String BOOK_DETAIL_RESTFUL_PREFIX = "https://capi-v2.sankakucomplex.com/posts/keyset";
    String SHOW_PAGE_PREFIX = "https://chan.sankakucomplex.com/post/show/";
    String bookId = "-1";
    String aimArtistName;
    boolean single;
    public Map<String, String> sanCodeSequence = new HashMap<>();
    public Map<String, String> filenameSequence = new HashMap<>();
    BookInfo bookInfo;

    public Map<String, Integer> fileSubfix = new HashMap<>();

    // 文件名-真实路径  其中文件名是不带前缀的， 真实路径是带序号前缀的
    public Map<String, String> filePath;
    String artistPicPath;
    String artistVidPath;
    String artistBookedPath;
    String thisBookPath;
    public int downloaded = 0;
    public void initFileStoreSituation() {
        this.filePath = new HashMap<>();
        File picbase = new File(artistPicPath);
        File[] picFiles = picbase.listFiles();
        if (null != picFiles)
            for (int i = 0; i < picFiles.length; i++) {
                if (picFiles[i].isDirectory()) {// 如果是目录
                    String[] artworks = picFiles[i].list();
                    for (int j = 0; j < artworks.length; j++) {
                        String fullPath = artistPicPath + picFiles[i].getName() + "/" + artworks[j];
                        String fileName = artworks[j].substring(5);
                        this.filePath.put(fileName, fullPath);
                    }
                } else {
                    this.filePath.put(picFiles[i].getName(), artistPicPath + picFiles[i].getName());
                }
            }
        File vidbase = new File(artistVidPath);
        File[] vidFiles = vidbase.listFiles();
        if (null != vidFiles)
            for (int i = 0; i < vidFiles.length; i++) {
                if (vidFiles[i].isFile()) {
                    this.filePath.put(vidFiles[i].getName(), artistVidPath + vidFiles[i].getName());
                }
            }
        System.out.println(this.filePath);
    }

    public BookPageProcessor(String aimArtistName, boolean single, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        this.single = single;
        this.aimArtistName = aimArtistName;
        ArtworkInfo artworkInfo = new ArtworkInfo();
        artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
        artworkInfo.aimName = aimArtistName;
        artworkInfo.fileName = "1.jpg";
        this.artistPicPath = diskService.getParentPath(artworkInfo, "", ArtworkInfo.STORE_PLACE.ARTIST.storePlace);
        artworkInfo.fileName = "1.mp4";
        this.artistVidPath = diskService.getParentPath(artworkInfo, "", ArtworkInfo.STORE_PLACE.ARTIST.storePlace);
        this.artistBookedPath = this.artistPicPath + "booked/";
        initFileStoreSituation();
        // 这里没有初始化 BookInfo 是因为 getThisBookPath 方法必须有bookInfo，而“正确的”bookInfo必须才能使用
        logger.info("重要提示：一个book作品里面，有可能 两个不同排序的作品，实际上是同一个文件，所以BookPageProcessor有这方面的考虑");
        logger.info("主要策略是： 通过 序号:作品  键值对，保存了一个作品后，删除“一个”这个作品的序号，这样第二次下载的时候，会存为另一个序号");
        logger.warn("XXXXXXX  连续调用前 需要使用 reset 以免之前Book的列表信息 影响当前内容  XXXXXX");
        logger.info("图片路径：" + this.artistPicPath);
        logger.info("视频路径：" + this.artistVidPath);
        logger.info("Booked路径：" + this.artistBookedPath);
    }


    public void reset(String artistName, boolean single) {
        this.single = single;
        this.fileSubfix = new HashMap<>();
        this.bookInfo = null;
        this.aimArtistName = artistName;
        this.bookId = "-1";
        this.sanCodeSequence = new HashMap<>();
        this.filenameSequence = new HashMap<>();
        this.artistPicPath = null;
        this.artistVidPath = null;
        this.artistBookedPath = null;
        this.thisBookPath = null;

    }

    public String getThisBookPath() {
        if (null != this.thisBookPath) {
            return this.thisBookPath;
        } else {
            ArtworkInfo info = new ArtworkInfo();
            info.fileName = "1.jpg";
            info.aimName = aimArtistName;
            formatArtworkInfoForSave(info);
            this.thisBookPath = diskService.getParentPath(info, "B", info.storePlace);
            return this.thisBookPath;
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
        // 主要思路：希望能够先保存book基础信息，再下载保存作品信息，防止作品保存的时候，book信息在数据库中还不存在
        String url = page.getUrl().toString();
        if (url.startsWith(BOOK_PAGE_PREFIX)) { // 如果是books 页面，提取books信息，放到 task里面
            logger.info("检测到Book页面，提取BookId，并访问 book information（pools）接口");
            if (url.contains("?")) {
                bookId = url.substring(url.indexOf("books/") + 6, url.indexOf("?"));
            } else {
                bookId = url.substring(url.indexOf("books/") + 6, url.length());
            }
            page.addTargetRequest(BOOK_INFO_RESTFUL_PREFIX + bookId + "?lang=en");
        } else if (url.startsWith(BOOK_INFO_RESTFUL_PREFIX)) {
            logger.info("检测到 pools 接口返回的作品信息,提取book information");
            processPoolPageRestful(page);
            logger.info("访问Book pool接口");
            page.addTargetRequest(BOOK_DETAIL_RESTFUL_PREFIX + "?lang=en&default_threshold=1&hide_posts_in_books=in-larger-tags&limit=40&tags=pool:" + bookId);
        } else if (url.startsWith(BOOK_DETAIL_RESTFUL_PREFIX)) {// pool 页面 获取作品顺序列表
            logger.info("检测到Book pool 接口返回数据，解析其中作品信息");
            processPoolDetailPageRestful(page);
        } else if (url.startsWith(SHOW_PAGE_PREFIX)) {
            processShowPage(page);
        }
    }

    public void processShowPage(Page page) {
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 1.提取target信息
        AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
        // 2.提取 ArtworkInfo 信息
        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        // 3.处理ArtworkInfo 信息
        formatArtworkInfoForSaveAndRemoveFromSeq(artworkInfo);
        // 5.下载文件
        boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
        artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
        artworkInfo.isSingle = false;
        if (download) {
            dataBaseService.saveArtworkInfo(artworkInfo);
            this.filePath.put(artworkInfo.fileName, diskService.getParentPath(artworkInfo, "B", artworkInfo.storePlace));
            this.downloaded++;
        }
    }

    public String getSequencePrefixByFileName(String fileName) {
        AtomicReference<String> seq = new AtomicReference<>("");
        filenameSequence.forEach((String iSeq, String iFileName) -> {
            if (iFileName.equals(fileName)) {
                seq.set(iSeq);
            }
        });
        int x = 4 - seq.get().length();
        for (int j = 0; j < x; j++) {
            seq.set("0" + seq);
        }
        return seq + "_";
    }

    public String getSequencePrefixByStringValue(String seq) {
        String iStr = seq;
        int x = 4 - iStr.length();
        for (int j = 0; j < x; j++) {
            iStr = "0" + iStr;
        }
        return iStr;
    }

    public String getSequencePrefixByIntValue(int i) {
        String iStr = "" + i;
        int x = 4 - iStr.length();
        for (int j = 0; j < x; j++) {
            iStr = "0" + iStr;
        }
        return iStr;
    }

    public String getSequencePrefixBySanCode(String sanCode) {
        AtomicReference<String> seq = new AtomicReference<>("");
        sanCodeSequence.forEach((String iSeq, String iSancode) -> {
            if (iSancode.equals(sanCode)) {
                seq.set(iSeq);
            }
        });
        int x = 4 - seq.get().length();
        for (int j = 0; j < x; j++) {
            seq.set("0" + seq);
        }
        return seq + "_";
    }

    synchronized public String getSequencePrefixBySanCodeAndRemoveFromSeq(String sanCode) {
        AtomicReference<String> seq = new AtomicReference<>("");
        //  这里有一点小问题，但是没有影响，  一个作品在一个book里面出现两次，有两个 iSeq，下面这个循环没法 break
        //   但是这个问题实际上不影响 城需正常运行，因为删除操作在后面，反正最后删除了一个就可以了，另一次就删除另一个
        sanCodeSequence.forEach((String iSeq, String iCode) -> {
            if (iCode.equals(sanCode)) {
                seq.set(iSeq);
            }
        });
        this.filenameSequence.remove(seq.get());
        this.sanCodeSequence.remove(seq.get());
//        解释为什么不能用下面这个遍历： 因为虽然 初始数据是 0-xxx 1-xx 2-xx 但是当 执行了 remove 之后
//        再使用 get(0) 的时候， key = 0 的键值对 可能已经被删除了
//        for (int i = 0; i < sanCodeSequence.size(); i++) {
//            if(sanCodeSequence.get(""+i).equals(sanCode)){
//                seq.set("" + i);
//                this.filenameSequence.remove(""+i);
//                this.sanCodeSequence.remove(""+i);
//            }
//        }
        int x = 4 - seq.get().length();
        for (int j = 0; j < x; j++) {
            seq.set("0" + seq);
        }

        return seq + "_";
    }

    protected void processPoolPageRestful(Page page) {
        String html = page.getHtml().$("body").all().get(0).replaceAll("\n", "").replaceAll(" ", "");
        String data = SpiderUtils.extractTag(html);
        Map<String, Object> bookInfoMap = (Map<String, Object>) JSON.parse(data);
        bookInfo = new BookInfo();
        if (!this.single) {
            bookInfo.aimArtist = this.aimArtistName;
        }
        bookInfo.id = (Integer) bookInfoMap.get("id");
        bookInfo.name = (String) bookInfoMap.get("name");
        bookInfo.createdAt = (String) bookInfoMap.get("created_at");
        bookInfo.updatedAt = bookInfo.createdAt;
        bookInfo.artistId = (Integer) ((Map<String, Object>) (bookInfoMap.get("author"))).get("id");
        bookInfo.postCount = (int) bookInfoMap.get("post_count");
        bookInfo.visiblePostCount = (int) bookInfoMap.get("visible_post_count");
        bookInfo.rating = (String) bookInfoMap.get("rating");
        bookInfo.copyrights = new ArrayList<>();
        bookInfo.artistTags = new ArrayList<>();
        List<Map<String, Object>> tags = (List<Map<String, Object>>) bookInfoMap.get("tags");
        for (Map<String, Object> tag : tags
        ) { // 类型为 3 的tag 是 copyright
            if (tag.containsKey("type") && tag.get("type").toString().equals("3")) {
                bookInfo.copyrights.add(tag.get("name").toString().replaceAll("_", " "));
            }
        }
        List<Map<String, Object>> artistTags = (List<Map<String, Object>>) bookInfoMap.get("artist_tags");
        for (Map<String, Object> artTag : artistTags
        ) {
            bookInfo.artistTags.add(artTag.get("name").toString().replaceAll("_", " "));
        }

//        List<Map<String, Object>> artists = (List<Map<String, Object>>) bookInfoMap.get("artist_tags");
//        List<String> artistNames = new ArrayList<>();
//        for (int i = 0; i < artists.size(); i++) {
//            artistNames.add(((String) artists.get(0).get("name")).replaceAll("_"," "));
//        }
//        bookInfo.artistTags = artistNames;

        // bookInfoMap里面内容比较多，减少一点
        List<Map<String, Object>> posts = (List<Map<String, Object>>) bookInfoMap.get("posts");
        List<Map<String, Object>> simplePosts = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", posts.get(i).get("id"));
            info.put("file_url", posts.get(i).get("file_url"));
            simplePosts.add(info);
        }
        bookInfoMap.put("posts", simplePosts);
        bookInfo.information = JSON.toJSONString(bookInfoMap);
        dataBaseService.saveBookInfo(bookInfo);
        logger.info("保存Book基础信息：" + bookInfo.information);
    }

    public static class FileSubfix {
        public String subfix;
    }

    private synchronized boolean saveFileIfExists(String filename) throws IOException {
        String fileSaveName = getSequencePrefixByFileName(filename) + filename;
        String bookPath = getThisBookPath();
        if (new File(bookPath + fileSaveName).exists()) {
            // 如果book里面有这个文件，跳过下载
            logger.info("文件已经在作者当前Book目录中，跳过下载：  " + filename);
            return true;
        }

        if (this.filePath.containsKey(filename)) {
            String nowPath = this.filePath.get(filename);
            String nowName = nowPath.substring(nowPath.lastIndexOf("/")+1);
            if (nowName.contains("_")) {
                FileUtils.copyFile(new File(nowPath), new File(bookPath + fileSaveName));
            } else {
                FileUtils.moveFile(new File(nowPath), new File(bookPath + fileSaveName));
                // 移动过的文件，需要改变 path值
                this.filePath.put(filename,bookPath+fileSaveName);
            }
            logger.info("当前目标在其他作品（或pic/vid）中出现，已经复制完成 "+filename);
            return true;
        } else {
            return false;
        }


//        logger.info("验证文件:" + this.artistPicPath + filename);
//        logger.info("验证文件:" + this.artistVidPath + filename);
//        logger.info("验证文件:" + this.artistBookedPath + filename);
//        if (new File(this.artistPicPath + filename).exists()) {
//            // 判断作者pic path里面有这个文件，复制这个文件，并且将源文件放入 booked
//            logger.info("当前文件在作者PIC目录中，复制文件进行保存，并且将现有文件放入booked   " + filename);
//            FileUtils.copyFile(new File(this.artistPicPath + filename), new File(bookPath + fileSaveName));
//            FileUtils.moveFile(new File(this.artistPicPath + filename), new File(this.artistBookedPath + filename));
//            return true;
//        } else if (new File(this.artistBookedPath + filename).exists()) {
//            logger.info("当前文件在Booked中，复制文件到 book   " + filename);
//            FileUtils.copyFile(new File(this.artistBookedPath + filename), new File(bookPath + fileSaveName));
//            // 判断  booked 里面有这个文件，复制这个文件
//            return true;
//        } else if (new File(this.artistVidPath + filename).exists()) {
//            logger.info("当前文件在VID中,复制文件进行保存，并将现有文件放入booked   " + filename);
//            // 判断 vid path有这个文件，复制这个文件，并源文件放入Booked
//            FileUtils.copyFile(new File(this.artistVidPath + filename), new File(bookPath + fileSaveName));
//            FileUtils.moveFile(new File(this.artistVidPath + filename), new File(this.artistBookedPath + filename));
//            return true;
//        } else {
//            return false;
//        }
    }

    protected void processPoolDetailPageRestful(Page page) {
        // 通过page房问ajax接口会倍基础 <body>标签包裹，这里去除内容
        Map<String, Object> json = getJsonStringFromRestPage(page);
        // 处理 返回数据中的 data字段 ===================================================================================
        List<Map<String, Object>> detailData = (List<Map<String, Object>>) json.get("data");
        logger.info("解析接口返回data内容");
        for (int i = 0; i < detailData.size(); i++) { //保存 文件排序  key : sanCode / value :sequence
            Map<String, Object> data = detailData.get(i);
            sanCodeSequence.put(data.get("sequence").toString(), data.get("id").toString());
            String fileName = "";
            if (data.containsKey("file_url") && null != data.get("file_url") && !StringUtils.isEmpty(data.get("file_url").toString())) {
                fileName = data.get("file_url")
                        .toString().substring(
                                data.get("file_url").toString().lastIndexOf("/") + 1,
                                data.get("file_url").toString().indexOf("?"));
            } else {
                String subFix = data.get("file_type").toString().substring(data.get("file_type").toString().indexOf("/") + 1);
                if ("jpeg".equals(subFix)) {
                    subFix = "jpg";
                }
                fileName = data.get("md5").toString() + "." + subFix;
            }
            filenameSequence.put(data.get("sequence").toString(), fileName);
            logger.info("第 " + i + " 条： [" + data.get("sequence") + "][" + data.get("id") + "][" + fileName + "]   " + data);
        }
        // 处理返回数据中的 meta 字段 ====================================================================================
        Map<String, Object> meta = (Map<String, Object>) json.get("meta");
        if (meta.containsKey("next") && !"null".equals(meta.get("next")) && !(null == meta.get("next"))) { // 如果有下一页，解析下一页
            logger.info("发现下页next信息，访问接口");
            page.addTargetRequest(BOOK_DETAIL_RESTFUL_PREFIX + "?lang=en&next=" + meta.get("next") + "&default_threshold=1&hide_posts_in_books=in-larger-tags&limit=40&tags=pool:" + bookId);
        } else {
            // 处理Book信息，已有的复制并且改名，没有的进行下载=============================================================
            logger.info("列表信息请求结束");
            // x-1 或者这个 book的母文件夹，然后遍历里面的文件，然后 获取已有的序号
            ArtworkInfo artworkInfo = new ArtworkInfo();
            for (int i = 0; i < sanCodeSequence.size(); i++) {
                if(sanCodeSequence.containsKey(""+i)){
                    artworkInfo.fileName = sanCodeSequence.get(""+1);
                }
            }
            formatArtworkInfoForSave(artworkInfo);
            logger.info("解析当前Book中已经保存的作品序号，将已有作品序号从 等待下载列表中删除");
            String parentPath = diskService.getParentPath(artworkInfo, artworkInfo.PBPrefix, artworkInfo.storePlace);
            // 获取已经存储的当前book列表，提取序号
            String[] storedNames = new File(parentPath).list();
            if (null == storedNames) {
                storedNames = new String[0];
            }
            List<String> storedPrefix = new ArrayList<>();
            for (int i = 0; i < storedNames.length; i++) {
                storedPrefix.add(storedNames[i].substring(0, 4));
            }
            logger.info("当前一存储的文件前缀：" + storedPrefix);
            Map<String, String> cleanedSanCodeSeq = new HashMap<>();
            Map<String, String> cleanedFilenameSeq = new HashMap<>();
            // 将序号 转为缩略形式 0000->0   0123-> 123
            // 遍历 0-n ，如果，将没存储的信息放入新的 Map 里面，然后用新的Map 替换 原本的信息map
            // !!!! 注意 出现了问题，有的时候  book 里面 会天然缺少一些序号
            this.sanCodeSequence.forEach((String iSeq, String code) -> {
                String prefix = getSequencePrefixByStringValue(iSeq);
                if (storedPrefix.contains(prefix)) {

                } else {
                    cleanedFilenameSeq.put(iSeq, filenameSequence.get(iSeq));
                    cleanedSanCodeSeq.put(iSeq, sanCodeSequence.get(iSeq));
                }
            });
            // TODO: 4/18/2021  BUG-01 产生位置
//            for (int i = 0; i < sanCodeSequence.size(); i++) {
//                String thePrefix = getSequencePrefixByIntValue(i);
//                if (storedPrefix.contains(thePrefix)) {
//
//                } else {
//                    cleanedFilenameSeq.put("" + i, filenameSequence.get("" + i));
//                    cleanedSanCodeSeq.put("" + i, sanCodeSequence.get("" + i));
//                }
//            }
            this.sanCodeSequence = cleanedSanCodeSeq;
            this.filenameSequence = cleanedFilenameSeq;
            logger.info("处理后需要下载的SanCode信息：" + this.sanCodeSequence);
            logger.info("处理后需要下载的FileName信息：" + this.filenameSequence);
            // 遍历等待下载的作品名称 如果已经保存在 作者名下，作者booked名下，那么复制这份文件，否则创建下载链接
            Object[] fileNames = this.filenameSequence.values().toArray();
            for (int i = 0; i < fileNames.length; i++) {
                String filename = fileNames[i].toString();
                Set<String> keys = this.filenameSequence.keySet();
                AtomicReference<String> seq = new AtomicReference<>("");

                keys.forEach((String iSeq) -> {
                    // TODO: 4/18/2021 BUG-01 触发位置
                    if (this.filenameSequence.get(iSeq).equals(filename)) {
                        seq.set(iSeq);
                    }
                });
                try {
                    if (saveFileIfExists(filename)) {
                        // 如果通过现有文件保存成功，那么当前文件的等下载
                        // 调用这个方法只是因为这个方法能够协助删除数据，并不是需要
                        String sanCode = this.sanCodeSequence.get(seq.get());
                        getSequencePrefixBySanCodeAndRemoveFromSeq(sanCode);
                        logger.info("文件处理完成：" + filename);
                    } else {

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // TODO: 4/17/2021  两个站点
            //  创建爬虫，下载文件=======================================================================================
            BookPageProcessor bookPageProcessor = new BookPageProcessor(aimArtistName, single, dataBaseService, diskService);
            bookPageProcessor.flexSite = SpiderUtils.site;
            bookPageProcessor.sanCodeSequence = this.sanCodeSequence;
            bookPageProcessor.filenameSequence = this.filenameSequence;
            bookPageProcessor.bookId = this.bookId;
            bookPageProcessor.bookInfo = this.bookInfo;
            bookPageProcessor.aimArtistName = this.aimArtistName;
            String[] urls = new String[this.sanCodeSequence.size()];
            Object[] collections = this.sanCodeSequence.values().toArray();
            for (int i = 0; i < urls.length; i++) {
                urls[i] = "https://chan.sankakucomplex.com/post/show/" + collections[i].toString();
            }
            Spider.create(bookPageProcessor).addUrl(urls).thread(4).run();
        }
    }

    public void formatArtworkInfoForSave(ArtworkInfo artworkInfo) {
        // 1.写入 book信息
        artworkInfo.bookId = bookInfo.id;
        artworkInfo.bookName = bookInfo.name;
        artworkInfo.setTagCopyright(bookInfo.copyrights);
        artworkInfo.setTagArtist(bookInfo.artistTags);
        // 2. 写入 aimName
        artworkInfo.aimName = aimArtistName;
        // 3. 前缀 B  book
        artworkInfo.PBPrefix = "B";
        // 4. 文件保存名为  序号前缀_文件原名
        String prefix = getSequencePrefixBySanCode(artworkInfo.sanCode);
        String saveName = prefix + artworkInfo.fileName;
        artworkInfo.fileSaveName = saveName;
        // 5.根据是否有artist识别 ArtistType
        if (null != artworkInfo.getTagArtist() && artworkInfo.getTagArtist().size() > 0) {
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
        } else {
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.UNKNOWN.artistType;
        }
        // 6.根据 下载目标是否是 Single 判断 Store Place
        if (single) {
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.SINGLE_PARENT_BOOK.storePlace;
        } else {
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST_PARENT_BOOK.storePlace;
        }
    }

    synchronized public void formatArtworkInfoForSaveAndRemoveFromSeq(ArtworkInfo artworkInfo) {
        logger.info("Book下载，将必要的Book信息写入ArtworkInfo");
        // 1.写入 book信息
        artworkInfo.bookId = bookInfo.id;
        artworkInfo.bookName = bookInfo.name;
        artworkInfo.setTagCopyright(bookInfo.copyrights);
        artworkInfo.setTagArtist(bookInfo.artistTags);
        // 2. 写入 aimName
        artworkInfo.aimName = aimArtistName;
        // 3. 前缀 B  book
        artworkInfo.PBPrefix = "B";
        // 4. 文件保存名为  序号前缀_文件原名
        String prefix = getSequencePrefixBySanCodeAndRemoveFromSeq(artworkInfo.sanCode);
        String saveName = prefix + artworkInfo.fileName;
        artworkInfo.fileSaveName = saveName;
        // 5.根据是否有artist识别 ArtistType
        if (null != artworkInfo.getTagArtist() && artworkInfo.getTagArtist().size() > 0) {
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
        } else {
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.UNKNOWN.artistType;
        }
        // 6.根据 下载目标是否是 Single 判断 Store Place
        if (single) {
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.SINGLE_PARENT_BOOK.storePlace;
        } else {
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST_PARENT_BOOK.storePlace;
        }
        logger.info("作品放置Artist 还是 Single 并非由Book作者信息决定，而是下载策略决定（部分作者作品放在Single部分）");
    }

    @Override
    public Site getSite() {

        return this.flexSite;
    }

    public Site flexSite;
    public static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .addHeader("accept-encoding", "gzip, deflate, br")
            .addHeader("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
            .addHeader("cache-control", "no-cache")
//            .addHeader("cookie", "_pk_ref.8.be6c=[\"\",\"\",1606524802,\"https://chan.sankakucomplex.com/\"]; _sankakucomplex_session=BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiVlZThmNzE0NWE1YzY5N2M4Mjc4Y2VkZThhMTA0N2Q5NQ==--87ff44ddccdb93eb6958bb61b61bb3856b8632ba; _pk_id.8.be6c=b412a086f317b99d.1617467043.0.1618496716..")
            .addHeader("pragma", "no-cache")
            .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"89\", \"Chromium\";v=\"89\", \";Not A Brand\";v=\"99\"")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-fetch-dest", "empty")
            .addHeader("sec-fetch-mode", "same-origin")
            .addHeader("sec-fetch-site", "same-origin")
            .addHeader("upgrade-insecure-requests", "1")
            .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36")
            .addCookie("_pk_ref.8.be6c", "%5B%22%22%2C%22%22%2C1606524802%2C%22https%3A%2F%2Fchan.sankakucomplex.com%2F%22%5D")
            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiVlZThmNzE0NWE1YzY5N2M4Mjc4Y2VkZThhMTA0N2Q5NQ")
            .addCookie("_pk_id.8.be6c", "b412a086f317b99d.1617467043.0.1618496716..");
    // 以下是 老站点


    public static void main(String[] args) {


        BookPageProcessor bookPageProcessor = new BookPageProcessor("xxoom", false, new DataBaseService(), new DiskService(SpiderSetting.buildSetting()));
//        bookPageProcessor.flexSite = BookPageProcessor.site;
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/13012?tags=order%3Apopularity%20yang-do").thread(1).run();
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/371810?tags=order%3Apopularity%20roke").thread(3).run();
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/387181").thread(3).run();
//        bookPageProcessor = new BookPageProcessor("", true, new DataBaseService(), new DiskService(SpiderSetting.buildSetting()));
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/380024").thread(3).run();
//        bookPageProcessor = new BookPageProcessor("", true, new DataBaseService(), new DiskService(SpiderSetting.buildSetting()));
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/153569").thread(3).run();
//        bookPageProcessor = new BookPageProcessor("", true, new DataBaseService(), new DiskService(SpiderSetting.buildSetting()));
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/310237").thread(3).run();
//        bookPageProcessor = new BookPageProcessor("", true, new DataBaseService(), new DiskService(SpiderSetting.buildSetting()));
//        Spider.create(bookPageProcessor).addUrl("https://beta.sankakucomplex.com/books/337919").thread(3).run();

        //  https://beta.sankakucomplex.com/books/326450
        //  https://beta.sankakucomplex.com/books/325766

        // https://beta.sankakucomplex.com/books/378765 ningguang
        // https://beta.sankakucomplex.com/books/370274  mona
        // https://beta.sankakucomplex.com/books/380777 beidou
        // https://beta.sankakucomplex.com/books/373369 babala
        // https://beta.sankakucomplex.com/books/377641  qin
        // https://beta.sankakucomplex.com/books/374836 mona
        // https://beta.sankakucomplex.com/books/386082 ganyu
        // https://beta.sankakucomplex.com/books/377180 feixieer
        // https://beta.sankakucomplex.com/books/373196  keqing
        // https://beta.sankakucomplex.com/books/369782  xinren
        // https://beta.sankakucomplex.com/books/376154 qin
        // https://beta.sankakucomplex.com/books/368765
        // https://beta.sankakucomplex.com/books/381379
        // https://beta.sankakucomplex.com/books/385642

// TODO: 4/18/2021 BUG-01 处理方法：把所有子文件 删除前缀，还原到  初始位置，重新下载
        //

    }
}