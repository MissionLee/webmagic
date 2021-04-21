package pers.missionlee.webmagic.spider.newsankaku.spider.singlepageclassify;

import pers.missionlee.webmagic.spider.newsankaku.source.FakeSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.spider.AbstractSpocessSpider;
import pers.missionlee.webmagic.spider.newsankaku.task.FakeTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-25 23:23
 */
public class SinglePageClassifySpider extends AbstractSpocessSpider {

    public Set<String> singleUrl;
    public Set<String> parentUrl ;
    public Set<String> bookUrl ;
    public SinglePageClassifySpider(TaskController task,Set<String> parentUrl,Set<String> singleUrl,Set<String> bookUrl) {
        super(task);
        this.singleUrl = singleUrl;
        this.parentUrl = parentUrl;
        this.bookUrl = bookUrl;
    }

    @Override
    public void doProcess(Page page) {
        String pageString = page.getHtml().toString();
        System.out.println(pageString);
        if (pageString.contains("This post has") && pageString.contains("child post")) { // 如果当前页面是Parent中的母页面，直接加入带下载列表

            System.out.println("“有些”子作品没有记录作品信息（例如作者），所以交给ParentSpider的页面是“母作品”页面，而不是作品列表页面");
            this.parentUrl.add(page.getUrl().toString());

        } else if (pageString.contains("This post belongs to") && pageString.contains("a parent post")) {// 如果当前页面是Parent中的子页面，将母页面加入下载列表
            System.out.println("当前页面是某个Parent的子页面，跳转Parent页面");
            List<String> href = page.getHtml().$("#parent-preview + div").$("a", "href").all();
            this.parentUrl.add("https://chan.sankakucomplex.com" + href.get(0));
//            page.addTargetRequest("https://chan.sankakucomplex.com" + href.get(0));

        } else if (pageString.contains("Books") && pageString.contains("&gt;&gt;") && pageString.contains("legacy")) { // 怕 Books 可能不准确，所以多验证
            List<String> href = page.getHtml().$(".content>.status-notice").$("a", "href").all();
            bookUrl.add(href.get(0));
            System.out.println(href);
            System.out.println(bookUrl);
        } else {
            singleUrl.add(page.getUrl().toString());
        }
    }

    public static void main(String[] args) {
//        SinglePageClassifySpider spider =
//                new SinglePageClassifySpider(new FakeTaskController(new FakeSourceManager()),new ArrayList<>(),new ArrayList<>(),new ArrayList<>());
//        Spider.create(spider).addUrl("https://chan.sankakucomplex.com/post/show/24540324").thread(1).run();

    }
}
