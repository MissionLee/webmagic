package pers.missionlee.webmagic.spider.newsankaku.spider.book;

import pers.missionlee.webmagic.spider.newsankaku.spider.AbstractSpocessSpider;
import pers.missionlee.webmagic.spider.newsankaku.task.BookTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import us.codecraft.webmagic.Page;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-27 17:41
 */
public class SingleSpider extends AbstractSpocessSpider {
    public BookTaskController taskController;
    public SingleSpider(TaskController task) {
        super(task);
        taskController = (BookTaskController)task;
    }

    @Override
    public void doProcess(Page page) {
        processAim(page);
    }
}
