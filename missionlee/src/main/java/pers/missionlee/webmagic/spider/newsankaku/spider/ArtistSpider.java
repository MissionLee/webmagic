package pers.missionlee.webmagic.spider.newsankaku.spider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:39
 */
public class ArtistSpider extends AbstractSpocessSpider {
    Logger logger = LoggerFactory.getLogger(ArtistSpider.class);

//    private TaskController task;

    public ArtistSpider(TaskController task) {
        super(task);
    }
    @Override
    public void doProcess(Page page) {
        String url = page.getUrl().toString();
        if (url.contains("tags")) {
            try {
                System.out.println("查询页面列表的时候，额外Sleep 10秒 ： ArtistSpider#process 30 行");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ListNum num= processList(page);
            // 说明 WorkMode.NEW 的时候，所有的url 都是初始化好的，不用考虑下一页，下面的逻辑是 如果是更新，需要自行判断是否有下一页
            int added = num.added;
            int all = num.all;
            if (
                    task.getWorkMode() == WorkMode.UPDATE //更新模式
                    && added > 0 //当前页面有更新
                    && url.contains("date")) {
                getNextPage(page);
            } else if (
                    (task.getWorkMode() == WorkMode.UPDATE_20_DATE_PAGE || task.getWorkMode() == WorkMode.UPDATE_10_DATE_PAGE)  //定量更新模式
                    && url.contains("date")
                    && all==20 // 有下一页
            ) {
                getNextPage(page);
            }
        } else if (url.startsWith("https://chan.sankakucomplex.com/post/show/")) {
            processAim(page);
        }
    }

    @Override
    public Site getSite() {
        return SpiderUtils.site;
    }

    /**
     * 从列表中提取详情页
     */


}
