package pers.missionlee.webmagic.demo;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.selenium.SeleniumDownloader;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-26 16:47
 */
public class HuaBanMeiNv2 implements PageProcessor {
    Site site = Site.me();
    @Override
    public void process(Page page) {
        System.out.println(page.getHtml());

    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new HuaBanMeiNv2()).setDownloader(new SeleniumDownloader("C:\\Windows\\System32\\chromedriver.exe")).addUrl("http://huaban.com/favorite/beauty/").thread(1).run();
    }
}
