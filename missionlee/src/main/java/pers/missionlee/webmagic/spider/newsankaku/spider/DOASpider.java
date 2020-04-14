package pers.missionlee.webmagic.spider.newsankaku.spider;

import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

public class DOASpider extends AbstractSpocessSpider {
    public DOASpider(TaskController task) {
        super(task);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.contains("tags")) {
            int added = processList(page);
            if (task.getWorkMode() == WorkMode.UPDATE && added > 0) {
                getNextPage(page);
            }
        } else if (url.startsWith("https://chan.sankakucomplex.com/post/show/")) {
            processAim(page);
        }
    }

}
