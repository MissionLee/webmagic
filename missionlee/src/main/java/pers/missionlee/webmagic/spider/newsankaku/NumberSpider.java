package pers.missionlee.webmagic.spider.newsankaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-31 10:30
 */
public class NumberSpider implements PageProcessor {
    Logger logger = LoggerFactory.getLogger(NumberSpider.class);
    private TaskController task;
    public NumberSpider(TaskController task) {
        this.task = task;
    }

    @Override
    public void process(Page page) {
        List<String> list = new ArrayList<>();
        list = page.getHtml().$(".tag-type-none").$("a").all();
        if(list == null || list.size() == 0)
            list = page.getHtml().$(".tag-count").all();

        Matcher matcher = SpiderUtils.htmlTextPattern.matcher(list.get(0));
        matcher.find();
        int num = Integer.valueOf(matcher.group(1).trim().replaceAll(",",""));
        task.setAimNum(num);
    }

    @Override
    public Site getSite() {
        return SpiderUtils.site;
    }
}
