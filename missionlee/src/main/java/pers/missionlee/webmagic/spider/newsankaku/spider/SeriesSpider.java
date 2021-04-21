package pers.missionlee.webmagic.spider.newsankaku.spider;

import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import us.codecraft.webmagic.Page;

public class SeriesSpider extends AbstractSpocessSpider {
    public SeriesSpider(TaskController task) {
        super(task);
    }

    @Override
    public void doProcess(Page page) {
        String url = page.getUrl().toString();
        if (url.contains("tags")) {
            ListNum listNum = processList(page);
            int added = listNum.added;
            int all = listNum.all;
            if (
                    all == 20
                    && (task.getWorkMode() == WorkMode.NEW || task.getWorkMode() == WorkMode.UPDATE_10_DATE_PAGE)
            ) {
                // 新建和完全更新模式下，指导当前页面有 20个作品，那么说明还有下一页
                getNextPage(page);
            } else if (task.getWorkMode() == WorkMode.UPDATE && added > 0 && all == 20) {
                // 当前页面有20个，并且当前页面有新作品，就尝试下一页
                getNextPage(page);
            }
        } else if (url.startsWith("https://chan.sankakucomplex.com/post/show/")) {
            processAim(page);
        }
    }

}
