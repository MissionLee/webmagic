package pers.missionlee.webmagic.spider.newsankaku.utlis;

import java.util.List;
import java.util.regex.Matcher;
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

    public static String extractStats(String sourceStr, Pattern pattern) {
        String aim = "";
        Matcher textMatcher = pattern.matcher(sourceStr);
        if (textMatcher.find())
            aim = textMatcher.group(1);
        return aim;
    }

    public static void extractTags(List<String> copyRightHtmlList, List<String> copyRight) {
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
}
