package pers.missionlee.chan.starter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.downloader.CallableHttpRangeDownloader;
import pers.missionlee.chan.downloader.FileDownloader;
import pers.missionlee.chan.downloader.HCaptchaConnectionFormat;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.*;
import pers.missionlee.chan.spider.book.ArtistBookListProcessor;
import pers.missionlee.chan.spider.book.BookPageProcessor;
import pers.missionlee.chan.spider.parent.ParentListPageProcessor;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-09 22:27
 */
public class SpiderStarter {
    Logger logger = LoggerFactory.getLogger(SpiderStarter.class);
    public List<String> skipNames;

    public SpiderStarter(String settingPath) throws IOException {
        File settingsFile = new File(settingPath);
        String settingString = FileUtils.readFileToString(settingsFile, "UTF8");
        // 初始化配置信息
        this.spiderSetting = JSON.parseObject(settingString, SpiderSetting.class);
//        HCaptchaConnectionFormat.refreshCookie(spiderSetting.__cfduid,spiderSetting.cf_chl_2,spiderSetting.cf_chl_prog,spiderSetting.cf_clearance);
        HCaptchaConnectionFormat.cookieString = spiderSetting.cookieString[0];
        HCaptchaConnectionFormat.cookieStrings = Arrays.asList(spiderSetting.cookieString);
        HCaptchaConnectionFormat.settingPathString = settingPath;
        HCaptchaConnectionFormat.User_agent = spiderSetting.userAgent;
        CallableHttpRangeDownloader.retryLimit = spiderSetting.retryTime;
        FileDownloader.retryLimit = spiderSetting.retryTime;
        FileDownloader.smartShutDown = spiderSetting.smartShutDown;
        SpiderUtils.site.setRetryTimes(30);
        SpiderUtils.site.setCycleRetryTimes(99);
        SpiderUtils.site.setRetrySleepTime(5000);
        spiderSetting.siteCookie.forEach((String key,String value)->{
            SpiderUtils.site.addCookie(key,value);
        });
        logger.info("配置信息：" + JSON.toJSONString(this.spiderSetting));
        // 初始化ChromeBookMark 解析
        this.reader = new ChromeBookmarksReader(this.spiderSetting.chromePath);
        // 初始化 data base service
        this.dataBaseService = new DataBaseService();
        // 初始化 作者名称映射
        initSpecialNames();
        // 初始化 Disk Service
        diskService = new DiskService(this.spiderSetting);
        //
        this.skipNames = new ArrayList<>();
        this.skipNames.addAll(Arrays.asList(spiderSetting.voiceActor));
        this.skipNames.addAll(Arrays.asList(spiderSetting.audioDesign));
        logger.info("根据配置文件，需要跳过的“声优作者 或 声音设计作者有：”");
        logger.info("" + skipNames);

    }

    public void parentBookByLevel(String level) {
        voiceList.addAll(Arrays.asList(spiderSetting.voiceActor));
        voiceList.addAll(Arrays.asList(spiderSetting.audioDesign));
        // TODO: 4/27/2021  下载文件 403 没想出来怎么办 先让代码跑下面的
        int lv = Integer.valueOf(level);
        List<String> artist = dataBaseService.getArtistListByLevel(lv, false);
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException ex) {
//            ex.printStackTrace();
//        }
//        System.out.println("" + 1111111111);
        for (int i = 0; i < artist.size(); i++) {

            String artName = artist.get(i);
//            System.out.println("222");
//            if (dataBaseService.needBook(artName)) {
//                downloadArtistBook(artName);
//                dataBaseService.updateBookDone(artName);
//            }
            if ((true || dataBaseService.needParent(artName)) && !voiceList.contains(artName)) {
                logger.info("注意，代码里面的是否需要 进行parent 现在返回true 实际逻辑需要重新设计");
                downloadArtistParent(artName);
//                dataBaseService.updateParentDone(artName);
            }
        }
    }

    SimpleDateFormat dateFormater = new SimpleDateFormat("HHmm");

    public int getSpecialIntTime() {
        String date = dateFormater.format(new Date());
        return Integer.parseInt(date);
    }

    public void autoRun() {
        while (true) {
            // 1. 自动更新 只更新 level 1 -2 -3 三个等级
            // 2. 每天凌晨 2 点 重新load任务信息
            int time = getSpecialIntTime();
            if ((time > 2300 && time <= 2400) || (time >= 0 && time <= 200)) {
                // 晚上11点到2点之间，
            } else if (time > 200 && time < 1000) {
                if (updateOne(0)) {

                } else if (updateOne(1)) {

                } else if (updateOne(2)) {

                } else {

                }
            } else if (time >= 1000 && time <= 2300) {

            }
        }
    }

    private boolean updateOne(int level) {
        String name = dataBaseService.getArtistNeedUpdateByLevelRandom(level);
        if (null == name) {
            return false; //  返回false表示 当前等级没有需要更新的
        } else {
            downloadArtist(name, true);
            return true;
        }
    }

    public void downloadSingleFromChromePath(String pathName) {
        // 1------获取想要下载的 url 例如  san10
        List<Map> urls = reader.getBookMarkListByDirName(pathName);

        // 2------首先区分出 single页面  还是  tags = #{artistName} 页面
        List<String> singleUrlTobeClassify = new ArrayList<>(); // 等待区分的single url

        HashSet<String> singleUrls = new HashSet<>();// 纯single url
        HashSet<String> parentUrls = new HashSet<>();// parent url
        HashSet<String> bookUrls = new HashSet<>(); // book url

        for (Map bookmark : urls
        ) {
            String url = bookmark.get("url").toString();
            if (url.contains("/show/") && url.contains("https://chan.sankakucomplex.com/")) {
                singleUrlTobeClassify.add(url);
                logger.info("独立页面：" + url);
            }
        }

        SinglePageClassifyPageProcessor singlePageClassifyPageProcessor =
                new SinglePageClassifyPageProcessor(singleUrls, parentUrls, bookUrls, dataBaseService, diskService);

        for (int i = 0; i < singleUrlTobeClassify.size(); i++) {
            String url = singleUrlTobeClassify.get(i);
            String sanCode = url.substring(url.lastIndexOf("/") + 1);
            // 查询有没有
            if (dataBaseService.sanCodeExist(sanCode)) { // 如果这个 sancode 作品已经下载
                logger.info("单独页面识别：此页面已经存在： " + sanCode);
            } else {
                Spider.create(singlePageClassifyPageProcessor).addUrl(url).thread(1).run();
            }
        }
//        Iterator<String> iterator1 = singleUrls.iterator();
//        while (iterator1.hasNext()) {
//            String url = iterator1.next();
//            logger.info("启动下载[Single]：" + url);
//            downloadSingle(url);
//        }
        Iterator<String> iterator2 = parentUrls.iterator();
        while (iterator2.hasNext()) {
            String url = iterator2.next();
            logger.info("启动下载[SingleParent]：" + url);
            downloadSingleParentByParentChildPageUrl(url);
        }
//        Iterator<String> iterator3 = bookUrls.iterator();
//        while (iterator3.hasNext()) {
//            String url = iterator3.next();
//            logger.info("启动下载[Book]：" + url);
//            downloadBookByBookChildPageUrl(url);
//        }
    }

    public Map<String, String> isTargetRelatedArtist = new HashMap<>();// key 为作者名称 value 为路径
    public Map<String, String> noTargetRelatedArtist = new HashMap<>(); // key 为作者名称 value 为路径
    public List<String> voiceList = new ArrayList<>();

    public void moveVoiceActorToOriginArtist() {
        voiceList.addAll(Arrays.asList(spiderSetting.voiceActor));
        voiceList.addAll(Arrays.asList(spiderSetting.audioDesign));

        for (int i = 0; i < voiceList.size(); i++) {

            String name = voiceList.get(i);
            System.out.println("_______________________" + name + "______________________");
//            Map<String,Integer> relatedArtist = new HashMap<>();
            Map<String, String> filePath = diskService.getArtistFilePath(name);
            filePath.forEach((String fileName, String path) -> {
                // 根据 fileName 获取 作者列表
                List<String> relatedArtists = dataBaseService.getArtistByFileName(fileName);

                // 找作者列表中 不是 声优，并且 isTarget 的作者
                if (relatedArtists.size() <= 1) {
                    logger.info("这个作品无处移动：" + fileName + "    " + relatedArtists.toString());
                } else {
                    String perferName = getPreferArtistName(relatedArtists);
                    if (StringUtils.isEmpty(perferName)) {
                        logger.info("这个作品没有找到合适的位置：" + fileName + "  " + relatedArtists.toString());
                    } else {
                        if (isTargetRelatedArtist.containsKey(perferName)) {
                            logger.info("这个作品的目标路径为(已有)：" + fileName + " " + isTargetRelatedArtist.get(perferName));
                            try {
                                if (diskService.fileExistsUnderArtist(fileName, perferName)) {
                                    logger.info("已有这个文件(执行删除)：" + fileName);
                                    new File(path).delete();
                                } else
                                    FileUtils.moveFileToDirectory(new File(path), new File(isTargetRelatedArtist.get(perferName)), false);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            logger.info("这个作品的目标路径为（新增）：" + fileName + " " + noTargetRelatedArtist.get(perferName));
                            try {
                                if (diskService.fileExistsUnderArtist(fileName, perferName)) {
                                    new File(path).delete();
                                } else
                                    FileUtils.moveFileToDirectory(new File(path), new File(noTargetRelatedArtist.get(perferName)), true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }

                // 把文件移动到这个作者名下
            });
//            System.out.println(name);
//            System.out.println("isTarget    " + isTargetRelatedArtist);
//            System.out.println("noTarget    " + noTargetRelatedArtist);
//            List<String> sanCodes = dataBaseService.getSanCodeByArtistName(name);
//            sanCodes.forEach((String sanCode)->{
//                List<String> artist = dataBaseService.getArtistBySanCode(sanCode);
//            });
            String pathName = diskService.transformArtistNameToPath(name);
            String srcPath = diskService.getCommonArtistParentPath(name, "1.jpg");
            if (!srcPath.contains("V-8-VOICE")) {
                try {
                    diskService.moveAllSubFiles(srcPath, "H://CHAN-ARTIST-3D/V-8-VOICE/" + pathName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public String getPreferArtistName(List<String> relatedNames) {
        String prefer = "";
        int code = 0; // code =0 未找到  1 非target  2 target
        for (int i = 0; i < relatedNames.size(); i++) {
            String iName = relatedNames.get(i);
            if (voiceList.contains(iName)) {

            } else if (isTargetRelatedArtist.containsKey(iName)) {
                if (code < 2) {
                    prefer = iName;
                    code = 2;
                } else if (code == 2) {
                    logger.info("发现一个作品，适用于两个isTarget 目标：" + prefer + "  " + iName);
                    prefer = iName;
                }
            } else if (noTargetRelatedArtist.containsKey(iName)) {
                if (code < 1) {
                    prefer = iName;
                    code = 1;
                    continue;
                } else if (code == 1) {
                    logger.info("发现一个作品，适用于两个noTarget 目标" + prefer + "  " + iName);
                    prefer = iName;
                }
            } else {
                boolean isTarget = dataBaseService.checkArtistIsTarget(iName);
                String iNamePath = diskService.getCommonArtistParentPath(iName, "1.jpg");
                if (isTarget) {
                    isTargetRelatedArtist.put(iName, iNamePath);
                    if (code < 2) {
                        prefer = iName;
                        code = 2;
                    } else if (code == 2) {
                        logger.info("发现一个作品，适用于两个isTarget 目标：" + prefer + "  " + iName);
                        prefer = iName;
                    }
                } else {
                    iNamePath = iNamePath.replace("/pic/", "/图-9-声优分离/");
                    noTargetRelatedArtist.put(iName, iNamePath);
                    if (code < 1) {
                        prefer = iName;
                        code = 1;
                        continue;
                    } else if (code == 1) {
                        logger.info("发现一个作品，适用于两个noTarget 目标" + prefer + "  " + iName);
                        prefer = iName;
                    }
                }
            }
        }
        return prefer;
    }

    public void dealDel(String artistName) {
        List<String> delMd5s = dataBaseService.getDelMarkFiles(artistName);
//        diskService.moveToDel(artistName,delMd5s);
    }

    public void start() throws UnsupportedEncodingException {
        String[] works = this.spiderSetting.works;
        for (int i = 0; i < works.length; i++) {
            if (works[i].contains("-") && !works[i].endsWith("-")) {
                String workType = works[i].substring(0, works[i].indexOf("-"));
                String workParam = works[i].substring(works[i].indexOf("-") + 1);
                switch (workType) {
                    case "1": // 下载 chrome 收藏夹
                        downloadArtistWithChromeBookmarks(workParam);
                        break;
                    case "2": // 下载 copyright official
                        downloadCopyrightOfficial(workParam);
                        break;
                    case "3":// 下载指定作者
                        downloadArtist(workParam, false);
                        break;
                    case "4": // 更新 copyright
                        updateCopyrightOfficial(workParam);
                        break;
                    case "5": // 按等级更新作者
                        updateArtistByLevel(workParam);
                        break;
                    case "6":// 更新单个作者
                        downloadArtist(workParam, true);
                        break;
                    case "7": {
                        try {
                            downloadArtistBook(workParam);
                            String realName = spiderSetting.getRelationName(workParam);
                            diskService.cleanArtistBookParentBases(realName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                    case "8": {
                        downloadArtistParent(workParam);
                        diskService.cleanArtistBookParentBases(workParam);
                    }
                    break;
                    case "9": {
                        int mode = Integer.valueOf(workParam);
                        if (1 == mode) {  // 更新作者目录
                            diskService.updateArtistLevel(dataBaseService);
                            this.dataBaseService = new DataBaseService();
                        } else if (2 == mode) { // 净化 artist book base 下面的文件，删除重复文件
//                            diskService.cleanArtistBookParentBases(spiderSetting.tips[0]);
//                            try {
//                                Thread.sleep(3000000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
                        } else if (3 == mode) {
                            diskService.checkBookArtistPath(dataBaseService);
                        } else if (4 == mode) {
                            diskService.mergePicVid();
                            System.out.println("8888888888888888888888 end 888888888888888888888");
                        } else if (5 == mode) {
                            diskService.findPathError();
                        } else if (6 == mode) {
                            moveVoiceActorToOriginArtist();
                            logger.info("完毕");
                            sleep();
                        } else if (7 == mode) {
                            diskService.findArtistUnTargeted(dataBaseService);
                            logger.info("完毕");
                            sleep();
                        }
                    }
                    break;
                    case "a": {
                        logger.info("更改了几个地方，取消了AbstractProcessor 下载 bookProcessor下载 parnent下载");
                        parentBookByLevel(workParam);
                    }
                    case "b": {
                        downloadSingleFromChromePath(workParam);
                    }
                    case "c": {
                        dealDel(workParam);
                    }
                    case "d":{
                        updateBookByLevel(workParam);
                    }
                    case "auto": {
                        autoRun();
                    }
                }
            }
        }
    }

    public void sleep() {
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void initSpecialNames() throws IOException {
        File nameFile = new File(this.spiderSetting.namePairs);
        String nameString = FileUtils.readFileToString(nameFile, "UTF8");
        String[] pairs = nameString.split("\\r\\n");
        for (int i = 0; i < pairs.length; i++) {
            if (!pairs[i].startsWith("#")) {
                specialName.put(pairs[i].split("~")[0], pairs[i].split("~")[1]);
            }
        }
    }

    public ChromeBookmarksReader reader;
    public SpiderSetting spiderSetting;
    public DataBaseService dataBaseService;
    public Map<String, String> specialName = new HashMap<>();
    public DiskService diskService;

    /**
     * 以下是各种 下载逻辑 -----------------
     */
    // 综合下载 通过ChromeBookmark 综合下载
    public void downloadArtistWithChromeBookmarks(String dir) throws UnsupportedEncodingException {
        // 1------获取想要下载的 url 例如  san10
        List<Map> urls = reader.getBookMarkListByDirName(dir);
        // 2------首先区分出 single页面  还是  tags = #{artistName} 页面
        List<String> singleUrlTobeClassify = new ArrayList<>(); // 等待区分的single url
        List<String> tagsUrlTobeClassify = new ArrayList<>(); // 等待区分的 tag url

        HashSet<String> singleUrls = new HashSet<>();// 纯single url
        HashSet<String> parentUrls = new HashSet<>();// parent url
        HashSet<String> bookUrls = new HashSet<>(); // book url
        HashSet<String> artistNames = new HashSet<>(); // artist name
        // 2.1 ---------- 先区分 show 和 tags 两大类
        for (Map bookmark : urls
        ) {
            String url = bookmark.get("url").toString();
            if (url.contains("/show/") && url.contains("https://chan.sankakucomplex.com/")) {
                singleUrlTobeClassify.add(url);
                logger.info("独立页面：" + url);
            } else if (url.contains("https://chan.sankakucomplex.com/") && url.contains("tags=")) {
                // TODO: 4/10/2021 现在先把 所有 带有 tags=XX 的都当作作者来看
                String artistName = "";
                if (url.contains("&")) {
                    artistName = SpiderUtils.urlDeFormater(url.split("tags=")[1].split("&")[0]);
                } else {
                    artistName = SpiderUtils.urlDeFormater(url.split("tags=")[1]);
                }
                artistNames.add(artistName);
                logger.info("发现作者：" + artistName);
            }
        }
        Iterator<String> iterator = artistNames.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            String artistName = iterator.next();
            logger.info(artistName + " [" + (++index) + "/" + artistNames.size() + "]");
            ArtworkInfo artworkInfo = new ArtworkInfo();
            artworkInfo.aimName = artistName;
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
            artworkInfo.fileName = "1.jpg";
            String picPath = diskService.getParentPath(artworkInfo, "", artworkInfo.storePlace);
            artworkInfo.fileName = "1.mp4";
            String vidPath = diskService.getParentPath(artworkInfo, "", artworkInfo.storePlace);
            if (new File(picPath).exists() || new File(vidPath).exists()) {
                logger.info("作者：" + artistName + " 已经下载过了");
                dataBaseService.touchArtist(artistName, System.currentTimeMillis());
            } else {
                logger.info("启动下载[Artist]：" + artistName);
                // 遍历现有文件目录，如果没有这个作者，那么下载这个作者
                downloadArtist(artistName, false);
                dataBaseService.touchArtist(artistName, System.currentTimeMillis());
            }
        }

//        SinglePageClassifyPageProcessor singlePageClassifyPageProcessor =
//                new SinglePageClassifyPageProcessor(singleUrls, parentUrls, bookUrls);
//        for (int i = 0; i < singleUrlTobeClassify.size(); i++) {
//            String url = singleUrlTobeClassify.get(i);
//            String sanCode = url.substring(url.lastIndexOf("/") + 1);
//            // 查询有没有
//            if (dataBaseService.sanCodeExist(sanCode)) { // 如果这个 sancode 作品已经下载
//                    logger.info("单独页面识别：此页面已经存在： "+sanCode);
//            } else {
//                Spider.create(singlePageClassifyPageProcessor).addUrl(url).thread(1).run();
//            }
//        }
//        if (spiderSetting.onlyArtist) {
//
//        } else {
//            Iterator<String> iterator1 = singleUrls.iterator();
//            while (iterator1.hasNext()) {
//                String url = iterator1.next();
//                logger.info("启动下载[Single]：" + url);
//                downloadSingle(url);
//            }
//            Iterator<String> iterator2 = parentUrls.iterator();
//            while (iterator2.hasNext()) {
//                String url = iterator2.next();
//                logger.info("启动下载[SingleParent]：" + url);
//                downloadSingleParentByParentChildPageUrl(url);
//            }
//            Iterator<String> iterator3 = bookUrls.iterator();
//            while (iterator3.hasNext()) {
//                String url = iterator3.next();
//                logger.info("启动下载[Book]：" + url);
//                downloadBookByBookChildPageUrl(url);
//            }
//        }

    }

    // 作者部分 =============================================
    // 下载单个作者  返回下载作品数量
    public int downloadArtist(String artistName, boolean update) {
        logger.info("==================== 作者："+artistName+" =================");
        if (skipNames.contains(artistName)) {
            logger.info("这个作者因为被标记为 voice actor 或者 audio design 所以跳过更新   " + artistName);
            return 0;
        }
        String realName = spiderSetting.getRelationName(artistName);
        String[] keys = new String[1];
        keys[0] = artistName;
        if (update && !spiderSetting.forceNew) { // 只有更新模式，并且不强制以新作模式运行的时候，才更新
            // 以下逻辑 用于 更新内容
            // TODO: 6/19/2021  key用原始key  但是 savename(realName) 是处理后的
            String url = SpiderUtils.getUpdateStartUrl(keys);
            ArtistPageProcessor artistPageProcessor =
                    new ArtistPageProcessor(
                            false, true, artistName,
                            dataBaseService, diskService,realName,spiderSetting);
            Spider.create(artistPageProcessor).addUrl(url).thread(spiderSetting.threadNum).run();
            // 以下逻辑 用于 判断是否自动 book 一个作品
            ArtworkInfo artworkInfo = new ArtworkInfo();
            artworkInfo.fileName = "1.jpg";
            artworkInfo.aimName = artistName;
            artworkInfo.PBPrefix = "B";
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
            if (artistPageProcessor.downloaded > 0
                    && spiderSetting.autoBook  //
                    && diskService.getParentPath(artworkInfo, artworkInfo.PBPrefix, artworkInfo.storePlace).contains(spiderSetting.bookParentArtistBase.substring(spiderSetting.bookParentArtistBase.lastIndexOf("/") + 1))
            ) {// 1.下载了新内容 2。启动了自动 book 3. 这个作者存储位置位于 “bookParentArtistBase” 中
                String prefix = "https://beta.sankakucomplex.com/wiki/en/";
                String urlName = artistName.replaceAll(" ", "_");
                String searchUrl = prefix + urlName;
                List<String> bookUrl = new ArrayList<>();
                ArtistBookListProcessor processor =
                        new ArtistBookListProcessor(
                                bookUrl, false, dataBaseService,
                                diskService, spiderSetting.autoBookSkipPercent,
                                spiderSetting.bookSkipPercent, true,
                                spiderSetting.skipBookLostPage,realName);
                Spider.create(processor).addUrl(searchUrl).run();

                for (String iUrl :
                        bookUrl) {
                    BookPageProcessor bookPageProcessor = new BookPageProcessor(artistName, false, dataBaseService, diskService, spiderSetting.skipBookLostPage,spiderSetting);
                    bookPageProcessor.flexSite = BookPageProcessor.site;
                    Spider.create(bookPageProcessor).addUrl(iUrl).thread(spiderSetting.threadNum).run();
                    artistPageProcessor.downloaded += bookPageProcessor.downloaded;
                }
            }
            return artistPageProcessor.downloaded;
        } else { // ===============   10个作品模式   或者   全部下载模式
            if (spiderSetting.onlyTryTen) {
                logger.info("注意XXXXXXX 临时改成每个作者只下载第一页");
                String url = SpiderUtils.getUpdateStartUrl(keys);
                String[] urls = new String[1];
                urls[0] = url;
                ArtistPageProcessor pageProcessor =
                        new ArtistPageProcessor(
                                spiderSetting.onlyTryTen, false,
                                artistName, dataBaseService,
                                diskService,realName,spiderSetting);
                Spider.create(pageProcessor).addUrl(urls).thread(spiderSetting.threadNum).run();
                return pageProcessor.downloaded;
            } else {
                ArtworkNumberPageProcessor numberPageProcessor = new ArtworkNumberPageProcessor(dataBaseService, diskService);
                Spider.create(numberPageProcessor).addUrl(SpiderUtils.getNumberCheckUrl(keys)).thread(1).run();
                int number = numberPageProcessor.getNumber();
                if (number == 0) {
                    logger.info("作者： " + artistName + " 现有作品匹配数量为 0");
                    return -1;
                } else if (number > spiderSetting.downloadLimit) {
                    logger.info("作者： " + artistName + " 作品数量 " + number + " 高于下载限制 " + spiderSetting.downloadLimit);
                    return -2;
                } else {
                    String[] urls ;
                    if (spiderSetting.downloadAllTryBest) {
                        urls = SpiderUtils.getStartUrlsTryBest(number, keys);
                    } else {
                        urls = SpiderUtils.getStartUrls(number, keys);
                    }
                    ArtistPageProcessor pageProcessor =
                            new ArtistPageProcessor(
                                    spiderSetting.onlyTryTen,
                                    false, artistName, dataBaseService,
                                    diskService,realName,spiderSetting);
                    Spider.create(pageProcessor).addUrl(urls).thread(spiderSetting.threadNum).run();
                    return pageProcessor.downloaded;
                }

            }
        }
    }


    // 根据等级更新作者
    public void updateArtistByLevel(String level) {
        int lv = Integer.valueOf(level);
        List<String> artists = dataBaseService.getArtistListByLevel(lv, !spiderSetting.forceUpdate);
        artists.forEach((String name) -> {
            int downloaded = downloadArtist(name, true);
            int times = 50;
            boolean doUpdate = true;
            if (lv >= 5) {
                doUpdate = false;
            } else if (lv == 4) {
                times = 360;
            } else if (lv == 3) {
                times = 180;
            } else if (lv == 2) {
                times = 90;
            } else if (lv == 1 && downloaded > 0) {
                times = 30; // 1级 有更新 20 天更
            } else if (lv == 0) {
                if (downloaded == 0)
                    times = 30; // 0 级本次没更新下次 30天更新
                else times = 15; // 0 级别 本次有个更新，下次 15天更新
            }
            if (doUpdate) {
                long nextUpdateTime = System.currentTimeMillis() + times * 24 * 60 * 60 * 1000L;
                dataBaseService.touchArtist(name, nextUpdateTime);

            }
            diskService.touchArtist(name);


        });
    }
    public void updateBookByLevel(String level){
        int lv = Integer.valueOf(level);
        List<String> names = diskService.getBookArtistByLevel(lv,spiderSetting.bookOnlyDaiDing);
        names.forEach((String name)->{
            downloadArtistBook(name);
        });

    }
    public void downloadArtistBook(String name) {
        String prefix = "https://beta.sankakucomplex.com/wiki/en/";
        String urlName = name.replaceAll(" ", "_");
        String searchUrl = prefix + urlName;
        String relName = spiderSetting.getRelationName(name);
        List<String> bookUrl = new ArrayList<>();
        ArtistBookListProcessor processor = new ArtistBookListProcessor(
                bookUrl, false, dataBaseService, diskService,
                spiderSetting.bookSkipPercent, true,
                spiderSetting.skipBookLostPage,relName
        );
        Spider.create(processor).addUrl(searchUrl).run();
        int iii = 0;
        for (String iUrl :
                bookUrl) {
            logger.info("============[" + (iii++) + "/" + bookUrl.size() + "]===========");
            BookPageProcessor bookPageProcessor = new BookPageProcessor(relName, false, dataBaseService, diskService, spiderSetting.skipBookLostPage,spiderSetting);
            bookPageProcessor.flexSite = BookPageProcessor.site;
            Spider.create(bookPageProcessor).addUrl(iUrl).thread(spiderSetting.threadNum).run();
        }

        if (spiderSetting.artistRelation.containsKey(name)) {


            List<String> names = spiderSetting.artistRelation.get(name);
            for (int i = 0; i < names.size(); i++) {
                urlName = names.get(i).replaceAll(" ", "_");
                searchUrl = prefix + urlName;
                bookUrl = new ArrayList<>();
                processor = new ArtistBookListProcessor(bookUrl, false, dataBaseService, diskService,
                        spiderSetting.bookSkipPercent, true, spiderSetting.skipBookLostPage,name);
                Spider.create(processor).addUrl(searchUrl).run();
                for (String iUrl :
                        bookUrl) {
                    BookPageProcessor bookPageProcessor = new BookPageProcessor(name, false, dataBaseService, diskService, spiderSetting.skipBookLostPage,spiderSetting);
                    bookPageProcessor.flexSite = BookPageProcessor.site;
                    Spider.create(bookPageProcessor).addUrl(iUrl).thread(spiderSetting.threadNum).run();
                }

            }
        }
    }

//    public static Map<String, List<String>> fackName = new HashMap<>();
//
//    static {
//        List<String> alexanderdinh = new ArrayList<>();
//        alexanderdinh.add("alexanderdinh");
//        fackName.put("alexander dinh", alexanderdinh);
//        List<String> mingxing = new ArrayList<>();
//        mingxing.add("liu mingxing");
//        fackName.put("ming xing", mingxing);
//
//    }

    public void downloadArtistParent(String name) {
        ParentListPageProcessor parentListPageProcessor = new ParentListPageProcessor(spiderSetting.skipParentPoolId,false, name, dataBaseService, diskService,spiderSetting);
        parentListPageProcessor.start();
    }

    // Single 部分 ========================================
    // 单纯single 页面下载
    public void downloadSingle(String url) {
        Spider.create(new SinglePageProcessor(dataBaseService, diskService)).addUrl(url).thread(1).run();
    }

    // 带有 Parent的Single 页面下载
    public void downloadSingleParentByParentChildPageUrl(String url) {
        ParentListPageProcessor processor = new ParentListPageProcessor(spiderSetting.skipParentPoolId,true, "", dataBaseService, diskService,spiderSetting);
        Spider.create(processor).addUrl(url).thread(spiderSetting.threadNum).run();
    }

    // Book  部分 ========================================
    // 下载（处理） 某个作者的所有book
    public void downloadBookByName(String artistName) {

    }

    // 通过 一个属于book的 url 下载这个book
    public void downloadBookByBookChildPageUrl(String url) {

    }

    public void downloadCopyrightOfficial(String copyrightName) {
        String[] keys = new String[2];
        keys[0] = copyrightName;
        keys[1] = "official art";
        ArtworkNumberPageProcessor numberPageProcessor = new ArtworkNumberPageProcessor(dataBaseService, diskService);
        Spider.create(numberPageProcessor).addUrl(SpiderUtils.getNumberCheckUrl(keys)).thread(1).run();
        int number = numberPageProcessor.getNumber();
        String[] urls ;
        if (spiderSetting.downloadAllTryBest) {
            urls = SpiderUtils.getStartUrlsTryBest(number, keys);
        } else {
            urls = SpiderUtils.getStartUrls(number, keys);
        }
        List<String> tags = new ArrayList<>();
        tags.add(copyrightName);
        CopyrightPageProcessor pageProcessor = new CopyrightPageProcessor(false,tags,dataBaseService,diskService);
        Spider.create(pageProcessor).thread(spiderSetting.threadNum).addUrl(urls).run();

    }

    public void updateCopyrightOfficial(String copyright) {
        List<String> tags = new ArrayList<>();
        tags.add(copyright);
        CopyrightPageProcessor pageProcessor = new CopyrightPageProcessor(true,tags,dataBaseService,diskService);
        String startUrl = SpiderUtils.getUpdateStartUrl(copyright,"official art");
        Spider.create(pageProcessor).thread(spiderSetting.threadNum).addUrl(startUrl).run();
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                SpiderStarter starter = new SpiderStarter(args[0]);
                starter.start();
                // 根据外部参数，读取 系统配置文件
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.sleep(1000 * 30);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
