package pers.missionlee.webmagic.spider.newsankaku;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceService;
import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.bookparentsingle.BookParentSingleSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.official.CopyrightOfficialSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.series.DOASourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.series.FinalFantasySourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.series.OverwatchSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.spider.book.BookSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.SeriesSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.NumberSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.ArtistSpider;
import pers.missionlee.webmagic.spider.newsankaku.task.ArtistTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.BookTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.OfficialTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.copyright.DOATaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.copyright.FinalFantasyTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.copyright.OverwatchTaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description: 通用作者下载，特别下载条件下的控制器，例如DOA系列，官方作品系列
 * @author: Mission Lee
 * @create: 2020-03-30 19:10
 */
@Deprecated
public class SpecialSpiderManager {
    public SourceManager source;
    public static Map<String, String> settings = new HashMap<>();// 系统参数

    public SpecialSpiderManager(SourceManager source) {
        this.source = source;
    }

    public void updateCopyRight(String name, boolean official) {

    }

    public void updateCharacter(String name, boolean official) {

    }

    public void downloadArtist(String name, WorkMode workMode) {
        TaskController artistTask = new ArtistTaskController(source, name);

        artistTask.setWorkMode(workMode); // 更新或新建

        NumberSpider spider = new NumberSpider(artistTask);
        Spider.create(spider).addUrl(artistTask.getNumberCheckUrl()).thread(1).run();
        boolean skip = false;
//        System.out.println(artistTask);
        if (artistTask.getAimNum() == 0) {
            System.out.println("!!!!!!!! 这个作者被网站清零了 !!!" + artistTask.getAimKeys()[0]);
            skip = true;
        }
        if (settings.containsKey("chromeNewDownloadLimit") // 开启限制
                && !StringUtils.isEmpty(settings.get("chromeNewDownloadLimit"))//
                && artistTask.getAimNum() > Integer.valueOf(settings.get("chromeNewDownloadLimit"))
        ) {
            System.out.println("因为数量限制 跳过这个作者");
            skip = true;
        }
        if (!skip) {
            if (workMode == WorkMode.NEW) {
                // 1. 使用数量爬虫，爬取总数
                // 2. 启动爬取爬虫
                String[] urls = artistTask.getStartUrls();
                ArtistSpider artistSpider = new ArtistSpider(artistTask);
                Spider.create(artistSpider).addUrl(urls).thread(3).run();
                // 更新作者 信息
                ((ArtistSourceManager) source).touchArtist(name);
            } else if (workMode == WorkMode.UPDATE
                    || workMode == WorkMode.UPDATE_20_DATE_PAGE
                    || workMode == WorkMode.UPDATE_10_DATE_PAGE) {

                // 1. 获取启动url（爬虫会自动根据条件“翻页”）
                String[] urls = artistTask.getStartUrls();
                // 2. 启动爬虫
                ArtistSpider artistSpider = new ArtistSpider(artistTask);
                Spider.create(artistSpider).addUrl(urls).thread(3).run();
                // 3. 根据任务情况保存作者信息（计算下次更新时间）
                ((ArtistTaskController) artistTask).finishUpdate();
                // finishUpdate 里面有 touchArtist 的功能
//                    ((ArtistSourceManager) source).touchArtist(name);

            }
        }

    }

    public void updateOverwatch(WorkMode workMode) {
        TaskController taskController = new OverwatchTaskController(source);
        taskController.setWorkMode(workMode);
        PageProcessor pageProcessor = new SeriesSpider(taskController);
        Spider.create(pageProcessor).addUrl(taskController.getStartUrls()).thread(4).run();
    }

    public void updateDOA(WorkMode workMode) {
        TaskController doaTask = new DOATaskController(source);
        doaTask.setWorkMode(workMode);
        PageProcessor pageProcessor = new SeriesSpider(doaTask);
        Spider.create(pageProcessor).addUrl(doaTask.getStartUrls()).thread(4).run();

    }

    public void updateFinalFantasy(WorkMode workMode) {
        if (true) {
            try {
                throw new Exception("FinalFantasy 系列没弄完");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        TaskController finalTask = new FinalFantasyTaskController(source);
        finalTask.setWorkMode(workMode);
        PageProcessor pageProcessor = new SeriesSpider(finalTask);
        Spider.create(pageProcessor).addUrl(finalTask.getStartUrls()).thread(4).run();
    }

    public void updateOfficial(String name, WorkMode workMode) {
        TaskController officialController = new OfficialTaskController(source, name);
        officialController.setWorkMode(workMode);

        NumberSpider spider = new NumberSpider(officialController);
        Spider.create(spider).addUrl(officialController.getNumberCheckUrl()).thread(1).run();
        if (officialController.getAimNum() == 0) {
            System.out.println("!!!!!! 这个 official 消失了");
        } else {


            PageProcessor pageProcessor = new ArtistSpider(officialController);
            Spider.create(pageProcessor).addUrl(officialController.getStartUrls()).thread(4).run();

        }

    }
    //
    public void chromeDirDownloadArtistSingleBookParent(String dir) throws IOException {
        System.out.println("谷歌浏览器书签综合下载");
        ChromeBookmarksReader reader = new ChromeBookmarksReader(settings.get("chromePath"));
        List<Map> artistList = reader.getBookMarkListByDirName(dir);
        List<String> namelist = new ArrayList<>();
        //
        //  从bookmark list里面 筛选出 artist
        //  特征是： https://chan.sankakucomplex.com/?tags=sho_%28shoichi-kokubun%29
        //  筛选出 book
        //  特征是： https://beta.sankakucomplex.com/books/298867
        //  筛选出 parent
        //  特征是： https://chan.sankakucomplex.com/?tags=parent%3A11041221
        //  筛选出 single页面
        //  特征是 https://chan.sankakucomplex.com/post/show/11041221
        for (Map bookmark :
                artistList) {
            String url = bookmark.get("url").toString();
            String lowUrl = url.toLowerCase();
            System.out.println(bookmark.get("url"));
            if(!url.contains("sankakucomplex")){ // 如果不包含 sankakucomplex 说明是不是目标

            }else{
                if(lowUrl.contains("beta")){
                    if(lowUrl.contains("search")){ // 作者页面

                    }else if(lowUrl.contains("tags=")){ // 作者页面

                    }else if(lowUrl.contains("show")){ // 单独页面

                    }else if(lowUrl.contains("parent")){

                    } else{ // 其他

                    }
                }else{
                    if(lowUrl.contains("wiki")){ // book 列表页面

                    }else if(lowUrl.contains("book")){ // book 首页

                    }else{

                    }
                }
            }
        }
    }
    /**
     * 读取本地chrome书签文件，解析 single页面，并进行下载任务
     * */
    public void downloadSinglePlusInChromeDir(String dir) throws IOException {
        ChromeBookmarksReader reader = new ChromeBookmarksReader(settings.get("chromePath"));
        List<Map> urls = reader.getBookMarkListByDirName(dir);
        List<String> singleList = new ArrayList<>();
        for (Map bookmark   :urls
             ) {
            String url = bookmark.get("url").toString();
            if(url.contains("show") && url.contains("sankakucomplex")){
                singleList.add(url);
            }
        }
        for (int i = 0; i < singleList.size(); i++) {
            String url = singleList.get(i);
            String sanCode = url.substring(url.lastIndexOf("/")+1);
            System.out.println(url +"_"+sanCode);
            // 查询有没有
        }
    }
    /**
     * 以作者模式，读取本地 chrome 的书签文件，解析作者名称，并创建对应下载任务
     */
    public void downLoadArtistInChromeDir(String dir) throws IOException {
//        ChromeBookmarksReader reader = new ChromeBookmarksReader(ChromeBookmarksReader.defaultBookmarkpath);
        ChromeBookmarksReader reader = new ChromeBookmarksReader(settings.get("chromePath"));
        List<Map> artistList = reader.getBookMarkListByDirName(dir);
        List<String> namelist = new ArrayList<>();

        for (Map bookmark :
                artistList) {
            System.out.println(bookmark.get("url"));
            if (bookmark.get("url").toString().contains("show") ||  //single 页面放在了artist里面
                    !bookmark.get("url").toString().contains("sankakucomplex") ||  // 其他书签放错位置
                    bookmark.get("url").toString().toLowerCase().contains("search") ||
                    !bookmark.get("url").toString().contains("tags=")
            ) { // artist 可能是 tag 也可能是 search
                System.out.println("这个页面存错了地方" + bookmark.get("url"));
            } else {
                String artistName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
                String pathName = artistName;
                if (((ArtistSourceManager) source).specialName.containsKey(artistName)) {
                    pathName = ((ArtistSourceManager) source).specialName.get(artistName);
                }
                try {
//                    boolean noFileOnDisk = (((ArtistSourceManager) source).getArtworkNumOfArtistDirectly(artistName) == 0);
//                    boolean noFileInDB = ((ArtistSourceManager) source).getArtworkNumOfArtist(AimType.ARTIST, artistName) == 0;
//                    boolean existsArtistPath = ((ArtistSourceManager) source).pathNameExists(pathName);
                    if (!((ArtistSourceManager) source).pathNameExists(pathName)
                            || (
                            ((ArtistSourceManager) source).getArtworkNumOfArtist(AimType.ARTIST, artistName) == 0
                                    && (((ArtistSourceManager) source).getArtworkNumOfArtistDirectly(artistName) == 0)
                    )

                    ) { // 关于上面的判断，有些作品是 多个人同时创作的，所以 作者2信息在下载作者1的时候可能被下载了，所以判断一个作者是否被下载，要判断 作品为零，并且自身名字没被记录

                        // 只要 文件路径不存在 需要下载
                        // 或者 本地目录下为零并且数据库为零 需要下载
                        namelist.add(artistName);
                        System.out.println("需要下载：" + artistName);
                    } else {
                        //  newSourceManager.touchArtist(artistName);
                        System.out.println("已经下载：" + artistName);
                    }
                } catch (Exception e) {
                    e.getStackTrace();
                    System.out.println("Chrome 解析报错============");
                    try {
                        Thread.sleep(1000 * 100);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }
        System.out.println("本次下载目标[" + namelist.size() + "] " + namelist);
        try {
            Thread.sleep(1000 * 3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (String name :
                namelist) {
            downloadArtist(name, WorkMode.NEW);
        }


    }
    public void downloadBookSingleParent(String url,SourceManager sourceManager){
        System.out.println("通过 Single/Book 页面下载，或通过Single页面解析出Parent Book 并整理下载");
        BookTaskController bspTask = new BookTaskController(sourceManager);
        if(url.contains("book")){

        }else{
            System.out.println("当前downloadBookSingleParent仅支持 book");
        }
    }

    /**
     * 以作者模式，更新指定分析的作者
     * <p>
     * maxLevel : 要更新的作者的最高等级（等级越低，优先级越高）
     * updateLevel : 是否需要刷新当前用户在数据库里面的等级（例如我重排等级，挪动文件夹位置）
     * forceUpdate: 没到更新时间的作者是否更新（有时候我需要强制更新 0 / 1 这种收藏级别）
     * WorkMode : 可选 UPDATE 普通更新  UPDATE_10_PAGE , UPDATE_20_PAGE 强制更新最近 10/20 页面 （有时网络问题导致系统不能按照要求运行，强制一定页面可以补充）
     */
    public void downLoadArtistByLevel(int maxLevel, boolean updateLevel, boolean forceUpdate, WorkMode workMode) {
        if (updateLevel) {
            try {
                ((ArtistSourceManager) source).updateArtistPathAndLevel();
//                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 1.更新数据库中的作者等级
         * 2.从数据库中搜索指定等级的作者列表
         * 3.启动更新
         * */
        List<String> artists = ((ArtistSourceManager) source).getArtistsByMaxLevel(!forceUpdate, maxLevel);
        System.out.println("本次任务需要更新：" + artists.size());
        System.out.println(artists);
        boolean start = true;
//        if(System.currentTimeMillis()>(1595583069409L+1000*60*10)){
//            start = true;
//        }
        int x = 0;

        for (String artist :
                artists) {
            // hage2013 volkor https://chan.sankakucomplex.com/?tags=stevencarson%20%20order%3Adate&page=10
            System.out.println(++x + "/" + artists.size() + " | " + artist);
//            if (artist.equals("obui")) {
//                start = true;
//            }
            if (start) {
//                if(artist.equals("ke-ta")) workMode = WorkMode.UPDATE_10_DATE_PAGE;
                downloadArtist(artist, workMode);
//                workMode = WorkMode.UPDATE;
            }


        }
    }

    public void downLoadArtistByLevel(int maxLevel) {
        downLoadArtistByLevel(maxLevel, false, false, WorkMode.UPDATE);
    }


    public static void main(String[] args) throws IOException {
        try {
            if (args.length == 1) {
                // 根据外部参数，读取 系统配置文件
                File settingsFile = new File(args[0]);
                String settingString = FileUtils.readFileToString(settingsFile, "UTF8");
                Map<String, String> setting = (Map<String, String>) JSON.parse(settingString);
                SpecialSpiderManager.settings.putAll(setting);
                String[] works = setting.get("works").split(",");

                Thread.sleep(5 * 1000);
                System.out.println(JSON.toJSONString(works));

                for (int i = 0; i < works.length; i++) {
                    String[] work = new String[2];
                    // 有些作者名字里面带有 - ，在命令5/6/7/8的时候需要做以下处理
                    work[0] = works[i].substring(0, works[i].indexOf("-"));
                    work[1] = works[i].substring(works[i].indexOf("-") + 1);

                    switch (work[0]) {

                        case "1":
                            new SpecialSpiderManager(new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","))).downLoadArtistInChromeDir(work[1]);
                            break;
                        case "2":
                            new SpecialSpiderManager(new CopyrightOfficialSourceManager(setting.get("base"), setting.get("bases").split(","))).updateOfficial(work[1], WorkMode.NEW);
                            break;
                        case "3":
                            new SpecialSpiderManager(new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","))).downloadArtist(work[1], WorkMode.NEW);
                            break;
                        case "4":
                            if ("overwatch".equals(work[1].toLowerCase().trim())) {
                                new SpecialSpiderManager(new OverwatchSourceManager(setting.get("base"), setting.get("bases").split(","))).updateOverwatch(WorkMode.NEW);
                            } else if ("doa".equals(work[1].toLowerCase().trim())) {
                                new SpecialSpiderManager(new DOASourceManager(setting.get("base"), setting.get("bases").split(","))).updateDOA(WorkMode.NEW);
                            } else if ("finalfantasy".equals(work[1].toLowerCase().trim())) {
                                new SpecialSpiderManager(new FinalFantasySourceManager(setting.get("base"), setting.get("bases").split(","))).updateOverwatch(WorkMode.NEW);
                            }
                            break;

                        case "5":
                            new SpecialSpiderManager(new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","))).downLoadArtistByLevel(Integer.valueOf(work[1]), true, false, WorkMode.UPDATE);
                            break;
                        case "6":
                            new SpecialSpiderManager(new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","))).downLoadArtistByLevel(Integer.valueOf(work[1]), true, false, WorkMode.UPDATE_10_DATE_PAGE);
                            break;
                        case "7":
                            new SpecialSpiderManager(new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","))).downLoadArtistByLevel(Integer.valueOf(work[1]), true, true, WorkMode.UPDATE);
                            break;
                        case "8":
                            new SpecialSpiderManager(new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","))).downLoadArtistByLevel(Integer.valueOf(work[1]), true, true, WorkMode.UPDATE_10_DATE_PAGE);
                            break;
                        case "9": {
                            if (1 == Integer.valueOf(work[1])) {
                                System.out.println("更新作者目录 / 查找重复的文件夹");
                                ArtistSourceManager sourceManager = new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","));
                                sourceManager.updateArtistPathAndLevel();
                                sourceManager.findTwo();
                            } else if (2 == Integer.valueOf(work[1])) {
                                System.out.println("全面验证作品存在情况");
                                // 1.搜集所有现存作品的 file_name
                                Set<String> diskFiles = new HashSet<>();
                                // 1.1 搜集作者目录的名字
                                ArtistSourceManager artistSourceManager = new ArtistSourceManager(setting.get("base"), setting.get("bases").split(","));
                                artistSourceManager.extractAllFileNames(diskFiles);
                                // 1.2 搜集official目录的名字
                                CopyrightOfficialSourceManager copyrightOfficialSourceManager = new CopyrightOfficialSourceManager(setting.get("base"), setting.get("bases").split(","));
                                copyrightOfficialSourceManager.extractAllFileNames(diskFiles);
                                // 1.3 搜集系列目录的名字
                                DOASourceManager doaSourceManager = new DOASourceManager(setting.get("base"), setting.get("bases").split(","));
                                doaSourceManager.extractAllFileNames(diskFiles);

                                OverwatchSourceManager overwatchSourceManager = new OverwatchSourceManager(setting.get("base"), setting.get("bases").split(","));
                                overwatchSourceManager.extractAllFileNames(diskFiles);

                                // 1.4 ⭐ 有一些内容没被统计，感觉不重要，就算了
                                // san_artwork 上的 file_name 字段是有索引的，所以查询起来问题不大
                                SourceService sourceService = new SourceService();
                                Set<String> dbFileNames = sourceService.getFileNames();

                                System.out.println("数据库作品数量：" + dbFileNames.size());
                                System.out.println("磁盘统计部份作品数量：" + diskFiles.size());

                                dbFileNames.forEach(name -> {
                                    if (diskFiles.contains(name)) {
                                        sourceService.updateLostFile(name, true);
                                    } else {
                                        System.out.println(name + "丢失 store_type 改为3（正常1 主动删除2）");
                                        sourceService.updateLostFile(name, false);
                                    }
                                });

                                // 查询所有数据库种的作品的 file_name
                                // 对比更新 file_name 存在情况，将 当前状态为 1正常 并且消失的文件，标记为 2 丢失 （因为有些是刻意删除的）
//                                System.out.println(JSON.toJSONString(diskFiles));
                            } else if (3 == Integer.valueOf(work[1])) {
                            }
                            try {
                                Thread.sleep(1000 * 30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                            break;
                        case "b":{ // 筛选 chrome path 中的 single页面


                        }break;
                        case "a":{
//

                        }
                        break;
                    }
                }
                try {
                    System.out.println("60秒后结束");
                    Thread.sleep(1000 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("请输入配置文件全路径地址");
                System.out.println("{\n" +
                        "    \"base\":\"H://ROOT\",\n" +
                        "//基础目录，临时文件位置\n" +
                        "    \"bases\":\"I://ROOT,H://ROOT\",\n" +
                        "//附加目录，多个目录用 , 分隔\n" +
                        "    \"namePairs\":\"I://ROOT/name-change.txt\",\n" +
                        "//作者名称纠正文件地址\n" +
                        "    \"chromePath\":\"\",\n" +
                        "谷歌浏览器收藏夹文件地址\n" +
                        "    \"works\":\"\"\n" +
                        "任务类型,在以下字符串里面选一个（多个任务 , 分隔）: \n" +
                        "1-san7:1表示下载chrome收藏夹，san7 表示收藏见目录名字\n" +
                        "2-league of legends:2表示下载official作品，league of legends 是系列名字\n" +
                        "3-sank：3表示全部下载某个特定作者，sank是作者名字\n" +
                        "4-doa:4表示 下载某个系列的作品，doa是系列名字，现在支持的系列有：\n" +
                        "5-3:5表示=普通-按时间=更新现有内容，3表示更新最低等级\n" +
                        "6-3:6表示=强制-按时间=更新现有内容=10页（200）=，3表示更新最低等级\n" +
                        "7-3:7表示=普通-忽略时间=更新现有内容，3表示更新最低等级\n" +
                        "8-3:8表示=强制-忽略时间=更新现有内容=10页（200）=，3表示更新最低等级\n" +
                        "9-1:9表示=特殊功能：1.更新作者目录信息，查找重复文件夹\n" +
                        "                   2.更新作品存在情况\n" +
                        "                   3.将 delete目录下的作品标记为删除\n" +
                        "                   4.将lost 文件目录下的作品标记为删除(用于发现损坏文件后，以后可以再次下载)\n" +
                        "}");
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
