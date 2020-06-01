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
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.contains("tags")) {
            ListNum num= processList(page);
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
