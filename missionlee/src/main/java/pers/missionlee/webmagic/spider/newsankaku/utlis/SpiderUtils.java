package pers.missionlee.webmagic.spider.newsankaku.utlis;

import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:41
 */
public class SpiderUtils {
    public static String BASE_URL = "https://chan.sankakucomplex.com";

    public static Pattern htmlTextPattern = Pattern.compile(">(.+?)<");
    public static Pattern htmlTitlePattern = Pattern.compile("title=\"(.+?)\"");
    public static Pattern resolutionPattern = Pattern.compile("bytes\">(.+?)<");
}
