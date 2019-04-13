package pers.missionlee.webmagic.spider.sankaku.manager;

import pers.missionlee.webmagic.spider.sankaku.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuDownloadSpider;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuNumberSpider;
import us.codecraft.webmagic.Spider;

import java.io.IOException;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-12 15:07
 *
 * 用于取代 SankakuSpiderProcessor
 */
public class SourceSpiderRunner extends SpiderUtils {
    public SourceSpiderRunner() {
    }
    public void runTask(SpiderTask spiderTask) throws IOException {
        if(spiderTask.getTaskType() == SpiderTask.TaskType.NEW)
            runNewTask(spiderTask);
        else
            runUpdateTask(spiderTask);
    }
    private SpiderTask runNewTask(SpiderTask spiderTask) throws IOException {
        int realArtworkNum = setTotalNumberWithSpider(spiderTask);
        spiderTask.total = realArtworkNum;
        setStartUrlArray(spiderTask);
        runDownLoadSpider(spiderTask);
        return spiderTask;
    }
    private SpiderTask runUpdateTask(SpiderTask spiderTask) throws IOException {
        setStartUrlArray(spiderTask);
        runDownLoadSpider(spiderTask);
        return spiderTask;
    }
    // ⭐ 此方法实际运行一个爬虫
    private void runDownLoadSpider(SpiderTask spiderTask) throws IOException {
        SankakuDownloadSpider sankakuDownloadSpider = new SankakuDownloadSpider(site,spiderTask);
        Spider.create(sankakuDownloadSpider).addUrl(spiderTask.startUrls).thread(spiderTask.getThreadNum()).run();
    }
    // ⭐ 此方法实际运行一个爬虫
    private int setTotalNumberWithSpider(SpiderTask spiderTask){
        String url = BASE_SITE+urlFormater(spiderTask.getArtistName(),spiderTask.isOfficial());
        SankakuNumberSpider spider = new SankakuNumberSpider(site,spiderTask);
        Spider.create(spider).addUrl(url).thread(1).run();
        return spider.getNum();
    }
    private String[] setStartUrlArray(SpiderTask spiderTask) {
        if (spiderTask.getTaskType() == SpiderTask.TaskType.UPDATE) {
            spiderTask.total=9999;
            String[] urls = new String[1];
            urls[0] = SITE_ORDER_PREFIX.DATE.getPrefix(spiderTask.getArtistName(), spiderTask.isOfficial()) + 1;
            spiderTask.startUrls = urls;
            return urls;
        } else {
            spiderTask.startUrls= urlGenertor(spiderTask.getArtistName(), spiderTask.isOfficial(), spiderTask.total);
            return spiderTask.startUrls;
        }
    }
}
