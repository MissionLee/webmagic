package pers.missionlee.webmagic.spider.newsankaku.spider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.util.List;

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
            int added = processList(page);
            if (task.getWorkMode() == WorkMode.UPDATE
                    && added > 0
                    && url.contains("date")) {
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
    @Override
    protected int processList(Page page) {
        try {
            Thread.sleep(task.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<String> urlList = page.getHtml().$(".thumb").$("a", "href").all();
        if (urlList != null && urlList.size() > 0) {
            int added = 0;
            for (String url :
                    urlList) {
                // 此处获得 url形式为 /post/show/5287781
                if (task.addTarget(url)) {
                    page.addTargetRequest(url);
                    added++;
                } else {
                    task.confirmRel(SpiderUtils.BASE_URL + url);
                }
            }
            logger.info("新增："+added+" 页面:"+page.getUrl().toString());
            return added;
        }
        return 0;
    }

}
