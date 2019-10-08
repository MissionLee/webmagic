package pers.missionlee.webmagic.spider.sankaku.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuDownloadSpider;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuNumberSpider;
import us.codecraft.webmagic.Spider;

import java.io.IOException;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-12 15:07
 * <p>
 * 用于取代 SankakuSpiderProcessor
 */
public class SourceSpiderRunner extends SpiderUtils {
    Logger logger = LoggerFactory.getLogger(SourceSpiderRunner.class);

    public SourceSpiderRunner() {
    }

    public void runTask(SpiderTask spiderTask) throws Exception {
        if (spiderTask.getTaskType() == SpiderTask.TaskType.NEW) {
            logger.debug("尝试下载作者[" + spiderTask.getArtistName() + "]的所有作品");
            runNewTask(spiderTask, true);
        } else {
            logger.debug("尝试更新作者[" + spiderTask.getArtistName() + "]的作品");
            runUpdateTask(spiderTask);
        }
    }

    /**
     * @Description:
     * @Param: [spiderTask , needCheckTotalNumber 有些情况下，spiderTask中会携带有效的作品总量，这个标志为会让此方法跳过总量检测阶段]
     * @return: pers.missionlee.webmagic.spider.sankaku.manager.SpiderTask
     * @Author: Mission Lee
     * @date: 2019-04-15
     */
    private SpiderTask runNewTask(SpiderTask spiderTask, boolean needCheckTotalNumber) throws Exception {

        if (needCheckTotalNumber) {
            int realArtworkNum = setTotalNumberWithSpider(spiderTask);
            spiderTask.total = realArtworkNum;
        }
        setStartUrlArray(spiderTask);
        runDownLoadSpider(spiderTask);
        return spiderTask;
    }

    private SpiderTask runUpdateTask(SpiderTask spiderTask) throws Exception {
        setStartUrlArray(spiderTask);
        logger.debug("\n爬虫任务详情：\n" + spiderTask);
        runDownLoadSpider(spiderTask);
        if (spiderTask.isGetAll()) {
            // 如果由获取全部内容的需求
            int nowWeHave = spiderTask.artworkAddress.size();
            int nowWeHaveInDB = spiderTask.getSourceManager().getArtworkNumOfDB(spiderTask.getArtistName());
            if (nowWeHaveInDB > nowWeHave) {

                nowWeHave = nowWeHaveInDB;
                System.out.println("数据库记录的数量多一些，数据库：" + nowWeHaveInDB + " 工具：" + nowWeHave);

            }
            int nowTheyHave = setTotalNumberWithSpider(spiderTask);
            if (nowWeHave < 2000) { // 本地存储大于2000 不再尝试全部获取
                if (
                        (nowTheyHave >= 2000 && nowWeHave < 1950) //总量大于两千，本地少于1950
                                || (nowTheyHave < 2000 && nowTheyHave >= 1500 && (nowTheyHave - nowWeHave > 20))// 总量1500~2000，本地比总量少
                                || (nowTheyHave < 1500 && nowTheyHave >= 1000 && (nowTheyHave - nowWeHave > 10))// 总量1000~15000 差值大于10
                                || (nowTheyHave < 1000 && nowTheyHave >= 500 && (nowTheyHave - nowWeHave > 5))// 总量1000 以下 差值大于5
                                || (nowTheyHave - nowWeHave) > 0

                ) {// 实际数量>2000
                    spiderTask.total = nowTheyHave;
                    spiderTask.setTaskType(SpiderTask.TaskType.NEW);
                    runNewTask(spiderTask, false);
                }
            }
        }
        return spiderTask;
    }

    // ⭐ 此方法实际运行一个爬虫
    private void runDownLoadSpider(SpiderTask spiderTask) throws IOException {
        SankakuDownloadSpider sankakuDownloadSpider = new SankakuDownloadSpider(site, spiderTask);
        Spider.create(sankakuDownloadSpider).addUrl(spiderTask.startUrls).thread(spiderTask.getThreadNum()).run();
    }

    // ⭐ 此方法实际运行一个爬虫
    private int setTotalNumberWithSpider(SpiderTask spiderTask) throws Exception {
        int retry = 3;
        SankakuNumberSpider spider = new SankakuNumberSpider(site, spiderTask);
        String url = BASE_SITE + urlFormater(spiderTask.getArtistName(), spiderTask.isOfficial());
        do {
            Spider.create(spider).addUrl(url).thread(1).run();

        } while (retry-- > 0 && spider.getNum() == Integer.MAX_VALUE);
        if (spider.getNum() == Integer.MAX_VALUE) {
            throw new Exception("未能成功获取数量");
        }
        return spider.getNum();
    }

    private String[] setStartUrlArray(SpiderTask spiderTask) {
        if (spiderTask.getTaskType() == SpiderTask.TaskType.UPDATE) {
            spiderTask.total = 9999;
            String[] urls = new String[1];
            urls[0] = SITE_ORDER_PREFIX.DATE.getPrefix(spiderTask.getArtistName(), spiderTask.isOfficial()) + 1;
            spiderTask.startUrls = urls;
            return urls;
        } else {
            spiderTask.startUrls = urlGenertor(spiderTask.getArtistName(), spiderTask.isOfficial(), spiderTask.total);
            return spiderTask.startUrls;
        }
    }
}
