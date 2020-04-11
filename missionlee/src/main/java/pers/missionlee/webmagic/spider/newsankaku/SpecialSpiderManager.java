package pers.missionlee.webmagic.spider.newsankaku;

import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.ArtistTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;
import pers.missionlee.webmagic.spider.sankaku.manager.SpiderManager;
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

    public void updateArtist(String name, WorkMode workMode) {
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
        } else if (workMode == WorkMode.ALL) { // 全部获取，遍历目标

        } else if (workMode == WorkMode.UPDATE) {

        }

    }

    public void updateDOA() {

    }

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
            updateArtist(name, WorkMode.NEW);
        }

    }

    private TaskController startSpider(TaskController task) {
        NewSpiderRunner runner = new NewSpiderRunner();
        runner.runTask(task);
        return task;
    }

    public static void main(String[] args) throws IOException {
        SpecialSpiderManager manager = new SpecialSpiderManager(new NewSourceManager("H:\\ROOT", "G:\\ROOT"));
//        manager.updateArtist("combos & doodles",WorkMode.NEW);
        manager.downLoadChromeArtistDir("san7");
        // combos &amp; doodles

        SourceManager sourceManager = new SourceManager("H:\\ROOT", "G:\\ROOT");
        SpiderManager spiderManager = new SpiderManager();
        spiderManager.update(sourceManager, SourceManager.SourceType.SANKAKU,false,1,false);
    }
}
