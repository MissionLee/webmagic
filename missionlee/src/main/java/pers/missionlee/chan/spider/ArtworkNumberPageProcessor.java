package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;
import java.util.regex.Matcher;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-19 19:26
 */
public class ArtworkNumberPageProcessor extends AbstractPageProcessor {
    public ArtworkNumberPageProcessor(DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
    }
    int number = 0;
    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void doProcess(Page page) {
        List<String> list  = page.getHtml().$(".tag-type-none").$("a").all();
        if(list == null || list.size() ==0)
            list = page.getHtml().$(".tag-count").all();
        Matcher matcher = SpiderUtils.htmlTextPattern.matcher(list.get(0));
        matcher.find();
        number = Integer.valueOf(matcher.group(1).trim().replaceAll(",",""));
        logger.info("NumberPageProcessor: 给定Url解析到作品数量[ "+number+" ] "+page.getUrl().toString());
    }

    public int getNumber(){
        return  number;
    }
}
