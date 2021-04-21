package pers.missionlee.chan.starter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.ArtistPageProcessor;
import pers.missionlee.chan.spider.ArtworkNumberPageProcessor;
import pers.missionlee.chan.spider.SinglePageClassifyPageProcessor;
import pers.missionlee.chan.spider.SinglePageProcessor;
import pers.missionlee.chan.spider.book.ArtistBookListProcessor;
import pers.missionlee.chan.spider.book.BookPageProcessor;
import pers.missionlee.webmagic.spider.newsankaku.source.FakeSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.bookparentsingle.BookParentSingleSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.spider.book.BookSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.book.SingleSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.singlepageclassify.SinglePageClassifySpider;
import pers.missionlee.webmagic.spider.newsankaku.task.BookTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.FakeTaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-09 22:27
 */
public class SpiderStarter {
    Logger logger = LoggerFactory.getLogger(SpiderStarter.class);

    public SpiderStarter(String settingPath) throws IOException {
        File settingsFile = new File(settingPath);
        String settingString = FileUtils.readFileToString(settingsFile, "UTF8");
        // 初始化配置信息
        this.spiderSetting = JSON.parseObject(settingString, SpiderSetting.class);
        logger.info("配置信息：" + JSON.toJSONString(this.spiderSetting));
        // 初始化ChromeBookMark 解析
        this.reader = new ChromeBookmarksReader(this.spiderSetting.chromePath);
        // 初始化 data base service
        this.dataBaseService = new DataBaseService();
        // 初始化 作者名称映射
        initSpecialNames();
        // 初始化 Disk Service
        diskService = new DiskService(this.spiderSetting);

    }
    public void start() throws UnsupportedEncodingException {
        String[] works = this.spiderSetting.works;
        for (int i = 0; i < works.length; i++) {
            if (works[i].contains("-") && !works[i].endsWith("-")) {
                String workType = works[i].substring(0, works[i].indexOf("-"));
                String workParam = works[i].substring(works[i].indexOf("-") + 1);
                switch (workType) {
                    case "1": // 下载 chrome 收藏夹
                        downloadWithChromeBookmarks(workParam);
                        break;
                    case "2": // 下载 copyright official
                        downloadCopyrightOfficial(workParam);
                        break;
                    case "3":// 下载指定作者
                        downloadArtist(workParam, false);
                        break;
                    case "4": // 更新 copyright
                        updateCopyrightOfficial();
                        break;
                    case "5": // 按等级更新作者
                        updateArtistByLevel(workParam);
                        break;
                    case "6":// 更新单个作者
                        downloadArtist(workParam,true);
                        break;
                    case "7":
                        downloadArtistBook(workParam);
                        break;
                    case "9": {
                        int mode = Integer.valueOf(workParam);
                        if (1 == mode) {  // 更新作者目录
                            diskService.updateArtistLevel(dataBaseService);
                        } else if (2 == mode) { // 净化 artist book base 下面的文件，删除重复文件
                            diskService.cleanArtistBookParentBases();

                        }
                    }
                    break;
                }
            }
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
    public void downloadWithChromeBookmarks(String dir) throws UnsupportedEncodingException {
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
        while (iterator.hasNext()) {
            String artistName = iterator.next();
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
                dataBaseService.touchArtist(artistName,System.currentTimeMillis());
            } else {
                logger.info("启动下载[Artist]：" + artistName);
                // 遍历现有文件目录，如果没有这个作者，那么下载这个作者
                downloadArtist(artistName, false);
                dataBaseService.touchArtist(artistName,System.currentTimeMillis());
            }
        }
        if (spiderSetting.onlyArtist) {

        } else {
            Iterator<String> iterator1 = singleUrls.iterator();
            while (iterator1.hasNext()) {
                String url = iterator1.next();
                logger.info("启动下载[Single]：" + url);
                downloadSingle(url);
            }
            Iterator<String> iterator2 = parentUrls.iterator();
            while (iterator2.hasNext()) {
                String url = iterator2.next();
                logger.info("启动下载[SingleParent]：" + url);
                downloadSingleParentByParentChildPageUrl(url);
            }
            Iterator<String> iterator3 = bookUrls.iterator();
            while (iterator3.hasNext()) {
                String url = iterator3.next();
                logger.info("启动下载[Book]：" + url);
                downloadBookByBookChildPageUrl(url);
            }
        }
        SinglePageClassifyPageProcessor singlePageClassifyPageProcessor =
                new SinglePageClassifyPageProcessor(singleUrls, parentUrls, bookUrls);
        for (int i = 0; i < singleUrlTobeClassify.size(); i++) {
            String url = singleUrlTobeClassify.get(i);
            String sanCode = url.substring(url.lastIndexOf("/") + 1);
            // 查询有没有
            if (dataBaseService.sanCodeExist(sanCode)) { // 如果这个 sancode 作品已经下载

            } else {
                Spider.create(singlePageClassifyPageProcessor).addUrl(url).thread(1).run();
            }
        }


    }

    // 作者部分 =============================================
    // 下载单个作者  返回下载作品数量
    public int downloadArtist(String artistName, boolean update) {
        String[] keys = new String[1];
        keys[0] = artistName;
        if (update && !spiderSetting.forceNew) { // 只有更新模式，并且不强制以新作模式运行的时候，才更新
            // 以下逻辑 用于 更新内容
            String url = SpiderUtils.getUpdateStartUrl(keys);
            ArtistPageProcessor artistPageProcessor = new ArtistPageProcessor(true, artistName, dataBaseService, diskService);
            Spider.create(artistPageProcessor).addUrl(url).thread(spiderSetting.threadNum).run();
            // 以下逻辑 用于 判断是否自动 book 一个作品
            ArtworkInfo artworkInfo = new ArtworkInfo();
            artworkInfo.fileName = "1.jpg";
            artworkInfo.aimName = artistName;
            artworkInfo.PBPrefix = "B";
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
            if(artistPageProcessor.downloaded>0
                    && spiderSetting.autoBook  //
                    && diskService.getParentPath(artworkInfo,artworkInfo.PBPrefix,artworkInfo.storePlace).contains(spiderSetting.bookParentArtistBase.substring(spiderSetting.bookParentArtistBase.lastIndexOf("/")+1))
            ){// 1.下载了新内容 2。启动了自动 book 3. 这个作者存储位置位于 “bookParentArtistBase” 中
                String prefix = "https://beta.sankakucomplex.com/wiki/en/";
                String urlName = artistName.replaceAll(" ", "_");
                String searchUrl = prefix + urlName;
                List<String> bookUrl = new ArrayList<>();
                ArtistBookListProcessor processor = new ArtistBookListProcessor(bookUrl, false, dataBaseService, diskService,spiderSetting.autoBookSkipPercent,spiderSetting.bookSkipPercent);
                Spider.create(processor).addUrl(searchUrl).run();

                for (String iUrl :
                        bookUrl) {
                    BookPageProcessor bookPageProcessor = new BookPageProcessor(artistName, false, dataBaseService, diskService);
                    bookPageProcessor.flexSite = BookPageProcessor.site;
                    Spider.create(bookPageProcessor).addUrl(iUrl).thread(spiderSetting.threadNum).run();
                    artistPageProcessor.downloaded += bookPageProcessor.downloaded;
                }
            }
            return artistPageProcessor.downloaded;
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
                String[] urls = SpiderUtils.getStartUrls(number, keys);
                ArtistPageProcessor pageProcessor = new ArtistPageProcessor(false, artistName, dataBaseService, diskService);
                Spider.create(pageProcessor).addUrl(urls).thread(spiderSetting.threadNum).run();
                return pageProcessor.downloaded;
            }
        }
    }


    // 根据等级更新作者
    public void updateArtistByLevel(String level) {
        int lv = Integer.valueOf(level);
        List<String> artists = dataBaseService.getArtistListByLevel(lv, !spiderSetting.forceUpdate);
        artists.forEach((String name) -> {
            int downloaded = downloadArtist(name, true);
            if (downloaded >= 0) {
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
                if(doUpdate){
                    long nextUpdateTime = System.currentTimeMillis() + times * 24*60*60*1000L;
                    dataBaseService.touchArtist(name,nextUpdateTime);
                }
            }

        });
    }
    public void downloadArtistBook(String name){
        String prefix = "https://beta.sankakucomplex.com/wiki/en/";
        String urlName = name.replaceAll(" ", "_");
        String searchUrl = prefix + urlName;
        List<String> bookUrl = new ArrayList<>();
        ArtistBookListProcessor processor = new ArtistBookListProcessor(bookUrl, false, dataBaseService, diskService,spiderSetting.bookSkipPercent);
        Spider.create(processor).addUrl(searchUrl).run();

        for (String iUrl :
                bookUrl) {
            BookPageProcessor bookPageProcessor = new BookPageProcessor(name, false, dataBaseService, diskService);
            bookPageProcessor.flexSite = BookPageProcessor.site;
            Spider.create(bookPageProcessor).addUrl(iUrl).thread(spiderSetting.threadNum).run();
        }
    }
    // Single 部分 ========================================
    // 单纯single 页面下载
    public void downloadSingle(String url) {
        Spider.create(new SinglePageProcessor(dataBaseService, diskService)).addUrl(url).thread(1).run();
    }

    // 带有 Parent的Single 页面下载
    public void downloadSingleParentByParentChildPageUrl(String url) {
        //
    }

    // Book  部分 ========================================
    // 下载（处理） 某个作者的所有book
    public void downloadBookByName(String artistName) {

    }

    // 通过 一个属于book的 url 下载这个book
    public void downloadBookByBookChildPageUrl(String url) {

    }

    public void downloadCopyrightOfficial(String copyrightName) {

    }

    public void updateCopyrightOfficial() {

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
