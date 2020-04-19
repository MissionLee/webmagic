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
        try {
            Thread.sleep(1000*10L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String url = page.getUrl().toString();
        if (url.contains("tags")) {
            ListNum listNum = processList(page);
            int added = listNum.added;
            int all = listNum.all;
            if (all == 20 && (task.getWorkMode() == WorkMode.NEW || task.getWorkMode() == WorkMode.UPDATE_ALL)) {
                // 新建和完全更新模式下，指导当前页面有 20个作品，那么说明还有下一页
                getNextPage(page);
            } else if (task.getWorkMode() == WorkMode.UPDATE && added > 0 && all==20) {
                // 当前页面有20个，并且当前页面有新作品，就尝试下一页
                getNextPage(page);
            }
        } else if (url.startsWith("https://chan.sankakucomplex.com/post/show/")) {
            processAim(page);
        }
    }

}
