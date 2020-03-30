package pers.missionlee.webmagic.spider.newsankaku;

import pers.missionlee.webmagic.spider.newsankaku.task.Task;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:39
 */
public class TaskSpider implements PageProcessor {
    private Task task;

    public TaskSpider(Task task) {
        this.task = task;
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.contains("tags")) {
            int added = processList(page);
            if (task.getWorkMode() == WorkMode.UPDATE
                    && added > 0
                    && url.contains("date")) {
                String thisPage = url.substring(url.length() - 1);
                int thisPageNum = Integer.valueOf(thisPage);
                if (thisPageNum < 50) {
                    String urlPrefix = url.substring(0, url.length() - 1);
                    page.addTargetRequest(urlPrefix + (++thisPageNum));
                }
            }
        } else if (url.startsWith("https://chan.sankakucomplex.com/post/show/")) {
            processAim(page);
        }
    }

    @Override
    public Site getSite() {
        return null;
    }

    /**
     * 从列表中提取详情页
     */
    private int processList(Page page) {
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
                if (task.addTarget(url)) {
                    page.addTargetRequest(SpiderUtils.BASE_URL + url);
                    added++;
                } else {
                    task.confirmRel(SpiderUtils.BASE_URL + url);
                }
            }
            return added;
        }
        return 0;
    }

    /**
     * 从详情页开始下载
     */
    private void processAim(Page page) {
        try {
            Thread.sleep(task.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

}
