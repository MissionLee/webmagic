package pers.missionlee.webmagic.spider.newsankaku;

import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.ArtistTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;
import us.codecraft.webmagic.Spider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: 通用作者下载，特别下载条件下的控制器，例如DOA系列，官方作品系列
 * @author: Mission Lee
 * @create: 2020-03-30 19:10
 */
public class SpecialSpiderManager {
    public NewSourceManager newSourceManager;

    public SpecialSpiderManager(NewSourceManager newSourceManager) {
        this.newSourceManager = newSourceManager;
    }

    public void updateCopyRight(String name, boolean official) {

    }

    public void updateCharacter(String name, boolean official) {

    }

    public void downloadArtist(String name, WorkMode workMode) {
        TaskController artistTask = new ArtistTaskController(newSourceManager, name);

        artistTask.setWorkMode(workMode); // 更新或新建
        if (workMode == WorkMode.NEW) {

            // 1. 使用数量爬虫，爬取总数
            NumberSpider spider = new NumberSpider(artistTask);
            Spider.create(spider).addUrl(artistTask.getNumberCheckUrl()).thread(1).run();
            System.out.println(artistTask);
            // 2. 启动爬取爬虫
            String[] urls = artistTask.getStartUrls();
            TaskSpider taskSpider = new TaskSpider(artistTask);
            Spider.create(taskSpider).addUrl(urls).thread(3).run();
            // 更新作者 信息
            newSourceManager.touchArtist(name);
        } else if (workMode == WorkMode.UPDATE_ALL) { // 全部获取，遍历目标

        } else if (workMode == WorkMode.UPDATE) {
            // 1. 获取启动url（爬虫会自动根据条件“翻页”）
            String[] urls = artistTask.getStartUrls();
            // 2. 启动爬虫
            TaskSpider taskSpider = new TaskSpider(artistTask);
            Spider.create(taskSpider).addUrl(urls).thread(3).run();
            // 3. 根据任务情况保存作者信息（计算下次更新时间）
            ((ArtistTaskController)artistTask).finishUpdate();

        }

    }

    public void updateDOA() {

    }
    // =======================  一下两个入库 为 作者模式 ============================

    /**
     * 以作者模式，读取本地 chrome 的书签文件，解析作者名称，并创建对应下载任务
     */
    public void downLoadChromeArtistDir(String dir) throws IOException {
        ChromeBookmarksReader reader = new ChromeBookmarksReader(ChromeBookmarksReader.defaultBookmarkpath);
        List<Map> artistList = reader.getBookMarkListByDirName(dir);
        List<String> namelist = new ArrayList<>();
        for (Map bookmark :
                artistList) {
            System.out.println(bookmark.get("url"));
            String artistName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
            if (newSourceManager.getArtworkNumOfArtistDirectly(artistName) == 0
                    && newSourceManager.getArtworkNumOfArtist(AimType.ARTIST, artistName) == 0) {
                namelist.add(artistName);
                System.out.println(artistName);
            } else {
                //  newSourceManager.touchArtist(artistName);
            }
        }
        System.out.println("本次下载目标[" + namelist.size() + "] " + namelist);
        for (String name :
                namelist) {
            downloadArtist(name, WorkMode.NEW);
        }

    }

    /**
     * 以作者模式，更新指定分析的作者
     * <p>
     * maxLevel : 要更新的作者的最高等级（等级越低，优先级越高）
     * updateLevel : 是否需要刷新当前用户在数据库里面的等级（例如我重排等级，挪动文件夹位置）
     * forceUpdate: 没到更新时间的作者是否更新（有时候我需要强制更新 0 / 1 这种收藏级别）
     */
    public void downLoadArtistByLevel(int maxLevel, boolean updateLevel, boolean forceUpdate) {
        /**
         * 1.更新数据库中的作者等级
         * 2.从数据库中搜索指定等级的作者列表
         * 3.启动更新
         * */
        List<String> artists = newSourceManager.getArtistsByMaxLevel(!forceUpdate, maxLevel);
        System.out.println("本次任务需要更新：" + artists.size());
        System.out.println(artists);
        boolean start = false;
        int x = 0;
        for (String artist :
                artists) {
            System.out.println(++x+"/"+artists.size() +" | "+artist );
//            if (artist.equals("ponta (velmar)")||artist.equals("pnt (ddnu4555)")) {
//                start = true;
//            }
//            if (start)
                downloadArtist(artist, WorkMode.UPDATE);
        }
    }

    public void downLoadArtistByLevel(int maxLevel) {
        downLoadArtistByLevel(maxLevel, false, false);
    }

    private TaskController startSpider(TaskController task) {
        NewSpiderRunner runner = new NewSpiderRunner();
        runner.runTask(task);
        return task;
    }

    public static void main(String[] args) throws IOException {
        SpecialSpiderManager manager = new SpecialSpiderManager(new NewSourceManager("H:\\ROOT", "G:\\ROOT"));
//        manager.updateArtist("combos & doodles",WorkMode.NEW);
        // combos &amp; doodles
//        manager.downloadArtist("sakimichan",WorkMode.UPDATE);
//        manager.downLoadArtistByLevel(0, false, true);
        manager.downLoadChromeArtistDir("san8");

    }
}
