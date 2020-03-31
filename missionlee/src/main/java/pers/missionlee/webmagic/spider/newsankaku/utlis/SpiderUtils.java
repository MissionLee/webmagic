package pers.missionlee.webmagic.spider.newsankaku.utlis;

import us.codecraft.webmagic.Site;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
    public static final String BASE_SEARCH_URL = "https://chan.sankakucomplex.com/?tags=";

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
    public static String getNumberCheckUrl(String... keys){
        StringBuffer buffer = new StringBuffer();
        buffer.append(BASE_SEARCH_URL);

        for (int i = 0; i < keys.length; i++) {
            buffer.append(urlFormater(keys[i]));
            if(i != keys.length-1)
            buffer.append("%20");
        }
        String url = buffer.toString()+"&commit=Search";
        System.out.println("SpiderUtil - getNumberCheckUrl "+url);
        return url;
    }
    public static String getUpdateStartUrl(String... keys){
        return SITE_ORDER_PREFIX.DATE.getPrefix(keys)+"1";
    }
    public static String[] getStartUrls(int artworkNum,String... keys){
        String urls[];
        if(artworkNum>2000){
            String prefixDate = SITE_ORDER_PREFIX.DATE.getPrefix(keys);
            String prefixPop = SITE_ORDER_PREFIX.POPULAR.getPrefix(keys);
            String prefixTagAsc = SITE_ORDER_PREFIX.TAG_COUNT_ASC.getPrefix(keys);
            String prefixTagDec = SITE_ORDER_PREFIX.TAG_COUNT_DEC.getPrefix(keys);
            urls = new String[200];
            for (int i = 0; i < 50; i++) {
                urls[i] = prefixDate+(i+1);
                urls[i+50] = prefixPop + (i+1);
                urls[i+100] = prefixTagAsc + (i+1);
                urls[i+150] = prefixTagDec+(i+1);
            }

        }else if(artworkNum>1000){
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            int loopNum =((Double) Math.ceil(new Double(pageNum)/2)).intValue();
            String prefixAsc = SITE_ORDER_PREFIX.TAG_COUNT_ASC.getPrefix(keys);
            String prefixDesc = SITE_ORDER_PREFIX.TAG_COUNT_DEC.getPrefix(keys);
            urls = new String[loopNum*2];
            for (int i = 0; i < loopNum; i++) {
                urls[i] = prefixAsc+(i+1);
                urls[i+loopNum] = prefixDesc + (i+1);
            }
        }else{
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            String prefix = SITE_ORDER_PREFIX.POPULAR.getPrefix(keys);
            urls = new String[pageNum];
            for (int i = 0; i < pageNum; i++) {
                urls[i] = prefix +(i+1);
            }
        }
        return urls;
    }
    public static String urlFormater(String artistName) {
        // 空格 () ’
        String artistFormat = artistName.trim()
                .replaceAll(" ", "_")// !important 这里吧空格对应成了下划线，是sankaku的特别处理方法
                //.replaceAll(" ", "%20")
                .replaceAll("!", "%21")
                .replaceAll("\"", "%22")
                .replaceAll("#", "%23")
                .replaceAll("\\$", "%24")
                //.replaceAll("%","%25")
                .replaceAll("&", "%26")
                .replaceAll("'", "%27")
                .replaceAll("\\(", "%28")
                .replaceAll("\\)", "%29")
                .replaceAll("\\*", "%2A")
                .replaceAll("\\+", "%2B")
                .replaceAll(",", "%2C")
                .replaceAll("-", "%2D")
                .replaceAll("\\.", "%2E")
                .replaceAll("/", "%2F")
                .replaceAll(":", "%3A")
                .replaceAll(";", "%3B")
                .replaceAll("<", "%3C")
                .replaceAll("=", "%3D")
                .replaceAll(">", "%3E")
                .replaceAll("\\?", "%3F")
                .replaceAll("@", "%40")
                .replaceAll("\\\\", "%5C")
                .replaceAll("\\|", "%7C");
        return artistFormat;
    }
    public  static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("__atuvc", "1%7C13")
            .addCookie("__atuvs", "5e1c553c1930ff14000")
            .addCookie("_pk_id.1.eee1", "660572c708fea8cf.1577603237.2.1577610247.1577610215.")
            .addCookie("_pk_id.2.42fa", "6dde919d87bd0a25.1552866104.85.1578915132.1578913654.")
            .addCookie("_pk_ref.1.eee1", "%5B%22%22%2C%22%22%2C1577610215%2C%22https%3A%2F%2Fchan.sankakucomplex.com%2F%3Fnext%3D2.35934045118749%2B19090001%26tags%3Ddate%3A2019-12-21..2019-12-28%20order%3Aquality%26page%3D74%22%5D")
            .addCookie("_pk_ses.2.42fa", "1")
            .addCookie("_pk_testcookie.2.42fa", "1")
            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiVlMzI1YjQ5MDg1ZTRiNTcyMzA2ZGMyMGJlZTM0OGRjNQ%3D%3D--93955adb171cc48de8ee353139d97ead3a364311")
            .addCookie("auto_page", "0")
            .addCookie("blacklisted_tags", "")
            .addCookie("loc", "MDAwMDBBU0NOSlMyMTQzMjk4NDA3NjAwMDBDSA==")
            .addCookie("locale", "en")
            .addCookie("login", "zuixue3000")
            .addCookie("mode", "view")
            .addCookie("na_id", "2019013108302142389291160478")
            .addCookie("na_tc", "Y")
            .addCookie("ouid", "5c52b21d0001851467bf69b6fdc7c24e116c87aa31768b505e9b")
            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
            .addCookie("uid", "5c52b21d9f7161d1")
            .addCookie("uvc", "0%7C1%2C13%7C52%2C466%7C1%2C780%7C2%2C643%7C3")
            .addCookie("PHPSESSID", "rrb6lkmc07f4b0fapkcln52eht")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Host", "chan.sankakucomplex.com")
            .addHeader("Pragma", "no-cach")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");

    public enum SITE_ORDER_PREFIX {

        DATE("date", "DATE"),
        TAG_COUNT_DEC("tagcount", "TAG_COUNT_DEC"),
        TAG_COUNT_ASC("tagcount_asc", "TAG_COUNT_ASC"),
        POPULAR("popular", "POPULAR"),
        FAV_COUNT("favcount", "FACOURITE_COUNT"),
        QUALITY("quality", ""),
        FILESIZE_DEC("filesize", ""),
        FILESIZE_ASC("filesize_asc", ""),
        VIEW_COUNT("viewcount", ""),
        MPIXELS_DEC("mpixels_desc", ""),
        MPIXELS_ASC("mpixels_asc", ""),
        PORTRAIT("portrait", "高"),
        LANDSCAPE("landscape", "宽");
        String key;
        String desc;

        public String getPrefix(String... keys) {
            StringBuffer prefix = new StringBuffer();
            prefix.append(BASE_SEARCH_URL);
            for (int i = 0; i < keys.length; i++) {
                prefix.append(urlFormater(keys[i]));
                prefix.append("%20");
            }
            prefix.append("%20order%3A" + key + "&page=");
            return prefix.toString();
        }

        public String getDesc() {
            return desc;
        }

        SITE_ORDER_PREFIX(String key, String desc) {
            this.key = key;
            this.desc = desc;
        }
    }
    public static String urlDeFormater(String codedName) throws UnsupportedEncodingException {
        String originName = URLDecoder.decode(codedName.trim(), "UTF8").replaceAll("_", " ");
//        String originName =codedName
//                .replaceAll("_"," ")
//                .replaceAll("%21","!")
//                .replaceAll("%22","\"")
//                .replaceAll("%23","#")
//                .replaceAll("%24","\\$")
//                .replaceAll("%26","&")
//                .replaceAll("%27","'")
//                .replaceAll("%28","(")
//                .replaceAll("%29",")")
//                .replaceAll("%2A","*")
//                .replaceAll("%2B","+")
//                .replaceAll("%2C",",")
//                .replaceAll("%2D","-")
//                .replaceAll("%2E",".")
//                .replaceAll("%2F","/")
//                .replaceAll("%3A",":")
//                .replaceAll("%3B",";")
//                .replaceAll("%3C","<")
//                .replaceAll("%3D","=")
//                .replaceAll("%3E",">")
//                .replaceAll("%3F","?")
//                .replaceAll("%40","@")
//                .replaceAll("%5C","\\\\")
//                .replaceAll("%7C","|");
        return originName;
    }
}
