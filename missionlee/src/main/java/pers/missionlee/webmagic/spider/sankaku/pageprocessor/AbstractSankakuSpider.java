package pers.missionlee.webmagic.spider.sankaku.pageprocessor;

import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.SankakuFileUtils;
import pers.missionlee.webmagic.spider.update.SpiderTask;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-22 14:19
 */
public abstract class AbstractSankakuSpider implements PageProcessor {
    static Pattern htmlTextPattern = Pattern.compile(">(.+?)<");
    static Pattern htmlTitlePattern = Pattern.compile("title=\"(.+?)\"");
    static Pattern resolutionPattern = Pattern.compile("bytes\">(.+?)<");

    String BASE_URL = "https://chan.sankakucomplex.com";

    Site site;
    SpiderTask spiderTask;
    public AbstractSankakuSpider(Site site, SpiderTask task){
        this.site =site;
        this.spiderTask=task;
    }
    public AbstractSankakuSpider(Site site,String rootPath,String artistName) {
        this.site = site;
        if(!SankakuFileUtils.makeArtistDir(rootPath, artistName)){
            throw new RuntimeException("can not create dir:"+artistName);
        }
    }

    public String extractStats(String sourceStr, Pattern pattern) {
        String aim = "";
        Matcher textMatcher = pattern.matcher(sourceStr);
        if (textMatcher.find())
            aim = textMatcher.group(1);
        return aim;
    }

    public void extractTags(List<String> copyRightHtmlList, List<String> copyRight) {
        int flag = 0;
        for (String str : copyRightHtmlList
        ) {
            if (flag % 2 == 0) {

                Matcher matcher = htmlTextPattern.matcher(str);
                if (matcher.find())
                    copyRight.add(matcher.group(1));
            }
            flag++;

        }
    }
    @Override
    public  Site getSite(){
        return site;
    }

}
