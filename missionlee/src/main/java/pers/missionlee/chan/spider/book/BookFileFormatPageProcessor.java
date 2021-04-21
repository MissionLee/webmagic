package pers.missionlee.chan.spider.book;

import org.apache.commons.lang3.StringUtils;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.AbstractPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-17 06:00
 */
public class BookFileFormatPageProcessor extends AbstractPageProcessor {
    BookPageProcessor.FileSubfix subfix;

    public BookFileFormatPageProcessor(BookPageProcessor.FileSubfix subfix, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        this.subfix = subfix;
    }

    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void doProcess(Page page) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(page.getHtml());
        // 图片  image-link
        //
        System.out.println("xxxxxxxxxxxxxxxxxxxxx");
        List<String> ilinkSrc = page.getHtml().$("#post-content").$("#image-link", "href").all();
        if (null != ilinkSrc && ilinkSrc.size() > 0) {
            String llll = ilinkSrc.get(0);
            System.out.println(llll);
            if (null != llll && !StringUtils.isEmpty(llll))
                subfix.subfix = llll.substring(llll.lastIndexOf("."), llll.indexOf("?"));
        }
        if (null == subfix.subfix || StringUtils.isEmpty(subfix.subfix)) {
            List<String> srcs = page.getHtml().$("#post-content").$("img", "src").all();
            if (null != srcs && srcs.size() > 0) {
                String srccc = srcs.get(0);
                System.out.println(srccc);
                subfix.subfix = srccc.substring(srccc.lastIndexOf("."), srccc.indexOf("?"));
                System.out.println(subfix.subfix);
            } else {
                List<String> srcvvv = page.getHtml().$("#post-content").$("video", "src").all();
                System.out.println(srcvvv);
                if (null != srcvvv && srcvvv.size() > 0) {
                    String v = srcvvv.get(0);
                    subfix.subfix = v.substring(v.lastIndexOf("."), v.indexOf("?"));
                }
            }

        }


        if (null == subfix.subfix || StringUtils.isEmpty(subfix.subfix)) {
            System.out.println("meizhaodao");
            subfix.subfix = ".png";
        }
        System.out.println(subfix.subfix);

    }

    @Override
    public Site getSite() {
        return SpiderUtils.site;
    }

    public static void main(String[] args) {
        BookFileFormatPageProcessor pageProcessor = new BookFileFormatPageProcessor(new BookPageProcessor.FileSubfix(), new DataBaseService(), new DiskService(SpiderSetting.buildSetting()));
        Spider.create(pageProcessor).addUrl("https://chan.sankakucomplex.com/post/show/23060261").thread(1).run();
//        String x = "/s.sankakucomplex.com/data/sample/a9/83/sample-a9833492f90f0a098718a5fc51540fa4.jpg?e=1618614549&m=AKa6QK4UUoYbZlNSYVaq1g";
//        System.out.println(x.lastIndexOf("."));
//        System.out.println(x.indexOf("?"));   23060256
    }
}
