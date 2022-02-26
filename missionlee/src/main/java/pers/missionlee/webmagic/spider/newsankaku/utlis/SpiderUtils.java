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
                    copyRight.add(matcher.group(1).replaceAll("_"," "));
            }
            flag++;

        }
    }

    public static String extractTag(String html) {
        Matcher matcher = htmlTextPattern.matcher(html);
        if (matcher.find()) return matcher.group(1);
        else return null;
    }

    public static String getNumberCheckUrl(String... keys) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(BASE_SEARCH_URL);

        for (int i = 0; i < keys.length; i++) {
            buffer.append(urlFormater(keys[i]));
            if (i != keys.length - 1)
                buffer.append("%20");
        }
        String url = buffer.toString() + "&commit=Search";
        System.out.println("SpiderUtil - getNumberCheckUrl " + url);
        return url;
    }

    public static String getSearchUrlPageOne(OrderType orderType, String... keys) {
        return getSearchUrlPageEquals(orderType, keys) + "1";
    }

    public static String getSearchUrlPageEquals(OrderType orderType, String... keys) {
        return orderType.getPrefix(keys);
    }

    public static String getUpdateStartUrl(String... keys) {
        return OrderType.DATE.getPrefix(keys) + "1";
    }
    public static String[] getStartUrlsDateBest(int artworkNum,String... keys ){
        int pageNum  = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
        if(pageNum>50) pageNum = 50;
        String[] urls = new String[pageNum];
        String prefix = OrderType.DATE.getPrefix(keys);
        for (int i = 0; i < pageNum; i++) {
            urls[i] = prefix + (i + 1);
        }
        return urls;
    }
    public static String[] getStartUrlsTryBest(int artworkNum,String... keys){
        String urls[];
        if (artworkNum > 2000) {
            String prefixPop = OrderType.POPULAR.getPrefix(keys);
            String prefixQuality = OrderType.QUALITY.getPrefix(keys);
            String prefixMegAsc = OrderType.MPIXELS_ASC.getPrefix(keys);
            String prefixMegDec = OrderType.MPIXELS_DEC.getPrefix(keys);
            String prefixFileSizeAsc = OrderType.FILESIZE_ASC.getPrefix(keys);
            String prefixFileSizeDec = OrderType.FILESIZE_DEC.getPrefix(keys);
            String prefixTagAsc = OrderType.TAG_COUNT_ASC.getPrefix(keys);
            String prefixTagDec = OrderType.TAG_COUNT_DEC.getPrefix(keys);
            String prefixDate = OrderType.DATE.getPrefix(keys);
            String prefixLandScap = OrderType.LANDSCAPE.getPrefix(keys);
            String prefixPor = OrderType.PORTRAIT.getPrefix(keys);
            urls = new String[550];
            for (int i = 0; i < 50; i++) {
                urls[i] = prefixPop + (i + 1);
                urls[i + 50] = prefixQuality + (i + 1);
                urls[i + 100] = prefixMegAsc + (i + 1);
                urls[i + 150] = prefixMegDec + (i + 1);
                urls[i + 200] = prefixFileSizeAsc + (i + 1);
                urls[i + 250] = prefixFileSizeDec + (i + 1);
                urls[i + 300] = prefixTagAsc + (i + 1);
                urls[i + 350] = prefixTagDec + (i + 1);
                urls[i + 400] = prefixDate + (i + 1);
                urls[i + 450] = prefixLandScap + (i + 1);
                urls[i + 500] = prefixPor + (i + 1);
            }

        } else if (artworkNum > 1000) {
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            int loopNum = ((Double) Math.ceil(new Double(pageNum) / 2)).intValue();
            String prefixAsc = OrderType.TAG_COUNT_ASC.getPrefix(keys);
            String prefixDesc = OrderType.TAG_COUNT_DEC.getPrefix(keys);
            urls = new String[loopNum * 2];
            for (int i = 0; i < loopNum; i++) {
                urls[i] = prefixAsc + (i + 1);
                urls[i + loopNum] = prefixDesc + (i + 1);
            }
        } else {
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            String prefix = OrderType.POPULAR.getPrefix(keys);
            urls = new String[pageNum];
            for (int i = 0; i < pageNum; i++) {
                urls[i] = prefix + (i + 1);
            }
        }
        return urls;
    }
    public static String[] getStartUrls(int artworkNum, String... keys) {
        String urls[];
        if (artworkNum > 2000) {
            String prefixPop = OrderType.POPULAR.getPrefix(keys);
            String prefixQuality = OrderType.QUALITY.getPrefix(keys);
//            String prefixMegAsc = OrderType.MPIXELS_ASC.getPrefix(keys);
//            String prefixMegDec = OrderType.MPIXELS_DEC.getPrefix(keys);
//            String prefixFileSizeAsc = OrderType.FILESIZE_ASC.getPrefix(keys);
//            String prefixFileSizeDec = OrderType.FILESIZE_DEC.getPrefix(keys);
            String prefixTagAsc = OrderType.TAG_COUNT_ASC.getPrefix(keys);
            String prefixTagDec = OrderType.TAG_COUNT_DEC.getPrefix(keys);
            String prefixDate = OrderType.DATE.getPrefix(keys);
//            String prefixLandScap = OrderType.LANDSCAPE.getPrefix(keys);
//            String prefixPor = OrderType.PORTRAIT.getPrefix(keys);
            urls = new String[250];
            for (int i = 0; i < 50; i++) {
                urls[i] = prefixPop + (i + 1);
                urls[i + 50] = prefixQuality + (i + 1);
//                urls[i + 100] = prefixMegAsc + (i + 1);
//                urls[i + 150] = prefixMegDec + (i + 1);
//                urls[i + 200] = prefixFileSizeAsc + (i + 1);
//                urls[i + 250] = prefixFileSizeDec + (i + 1);
                urls[i + 100] = prefixTagAsc + (i + 1);
                urls[i + 150] = prefixTagDec + (i + 1);
                urls[i + 200] = prefixDate + (i + 1);
//                urls[i + 450] = prefixLandScap + (i + 1);
//                urls[i + 500] = prefixPor + (i + 1);
            }

        } else if (artworkNum > 1000) {
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            int loopNum = ((Double) Math.ceil(new Double(pageNum) / 2)).intValue();
            String prefixAsc = OrderType.TAG_COUNT_ASC.getPrefix(keys);
            String prefixDesc = OrderType.TAG_COUNT_DEC.getPrefix(keys);
            urls = new String[loopNum * 2];
            for (int i = 0; i < loopNum; i++) {
                urls[i] = prefixAsc + (i + 1);
                urls[i + loopNum] = prefixDesc + (i + 1);
            }
        } else {
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            String prefix = OrderType.POPULAR.getPrefix(keys);
            urls = new String[pageNum];
            for (int i = 0; i < pageNum; i++) {
                urls[i] = prefix + (i + 1);
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

    public static Site site = Site.me()
            .setRetryTimes(30)
            .setTimeOut(100000)
            // TODO: 7/10/2021 以下cookie 是添加了 配置文件配置cookie后注释的
//            .addCookie("__atuvc", "1%7C17")
//            .addCookie("__atuvs", "60849f97aa53f3fb000")
//            .addCookie("_pk_id.2.42fa", "9554c6e76fbec0b7.1606836488.27.1619305983.1618845859.")
//            .addCookie("_pk_ses.2.42fa", "1")
//            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiUzOTNhNDA0YmU0OGY0MGNkMGNiNTZmZmFiMDYwZDYyMQ%3D%3D--5761d48c8dee7ba2eea9efd75c2dbe90de90c2ed")
//            .addCookie("auto_page", "0")
//            .addCookie("blacklisted_tags", "")
//            .addCookie("locale", "en")
//            .addCookie("login", "zuixue3000")
//            .addCookie("mode", "view")
//            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
//            .addCookie("theme","0")
//            .addCookie("v","0")
            // TODO: 7/10/2021 以下cookie 是很早就注释掉的
//            .addCookie("__cfduid","dbf7e7773e9cff987a17a97a7985956eb1619305796")
//            .addCookie("_pk_id.1.eee1", "bf81d87657cab680.1618060321.4.1618932431.1618917060.")
//            .addCookie("_pk_ref.1.eee1", "%5B%22%22%2C%22%22%2C1618932419%2C%22https%3A%2F%2Fchan.sankakucomplex.com%2F%3Ftags%3Denchuu_kakiemon%22%5D")
//            .addCookie("cf_chl_2","a079cbdf9b378e8")
//            .addCookie("cf_chl_prog","a10")
//            .addCookie("cf_clearance","ecbe355b464686c006347f7bbcaeb5fc3fdcc328-1619305869-0-250")
//            .addCookie("hmn_cp_visitor","180.104.215.103")
//            .addCookie("loc", "MDAwMDBBU0NOSlMyMTQzMjk4NDA3NjAwMDBDSA==")
//            .addCookie("na_id", "2019013108302142389291160478")
//            .addCookie("na_tc", "Y")
//            .addCookie("ouid", "5c52b21d0001851467bf69b6fdc7c24e116c87aa31768b505e9b")
//            .addCookie("uid", "5c52b21d9f7161d1")
//            .addCookie("uvc", "0%7C1%2C13%7C52%2C466%7C1%2C780%7C2%2C643%7C3")
//            .addCookie("PHPSESSID", "rrb6lkmc07f4b0fapkcln52eht")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Host", "chan.sankakucomplex.com")
            .addHeader("Pragma", "no-cach")
            .addHeader("sec-ch-ua","\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"")
            .addHeader("sec-ch-ua-mobile","?0")
            .addHeader("sec-fetch-dest","image")
            .addHeader("sec-fetch-mode","no-cors")
            .addHeader("sec-fetch-site","same-site")
//            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36");

//            .setRetryTimes(3)
//            .setTimeOut(100000)
//            // beta.sankaku 增加了下面三条
////            .addCookie("track_view_24540324", "1")
////            .addCookie("_sankakucomplex_session","BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiVlZThmNzE0NWE1YzY5N2M4Mjc4Y2VkZThhMTA0N2Q5NQ%3D%3D--87ff44ddccdb93eb6958bb61b61bb3856b8632ba")
////            .addCookie("_pk_id.8.be6c", "%5B%22%22%2C%22%22%2C1606524802%2C%22https%3A%2F%2Fchan.sankakucomplex.com%2F%22%5D")
////            .addCookie("_pk_id.8.be6c", "b412a086f317b99d.1617467043.0.1618496716..")
////
//            .addCookie("PHPSESSID", "ec1t2o5dpfc9esmq8psl6hrpoj")
//            .addCookie("__atuvc", "1%7C13")
//            .addCookie("__atuvs", "607a2604c2db6a9f000")
//            .addCookie("_pk_id.1.eee1", "bf81d87657cab680.1618060321.2.1618558468.1618060321.")
//            .addCookie("_pk_id.2.42fa", "b377c991952da662.1586822823.901.1618617861.1618610591.")
//            .addCookie("_pk_ref.1.eee1", "%5B%22%22%2C%22%22%2C1618558468%2C%22https%3A%2F%2Fchan.sankakucomplex.com%2F%22%5D")
//            .addCookie("_pk_ses.2.42fa", "1")
//            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiUzMGE2MzUwYjQwZjI2YzIwM2I5ZTljMTRlMzMzOWZjNw%3D%3D--4a0543378d4e7229153a3a4b9564f691fb9eeab9")
//            .addCookie("auto_page", "0")
//            .addCookie("blacklisted_tags", "")
//            .addCookie("hmn_cp_visitor", "180.104.215.103")
//            .addCookie("locale", "en")
//            .addCookie("login", "zuixue3000")
//            .addCookie("mode", "view")
//            .addCookie("ouid", "5c52b21d0001851467bf69b6fdc7c24e116c87aa31768b505e9b")
//            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
//            .addCookie("loc", "MDAwMDBBU0NOU0gyMTEyMzAxNTA3MzAwMDBDSA==")
//            .addCookie("theme", "0")
//            .addCookie("na_id", "2019013108302142389291160478")
//            .addCookie("v", "0")
//            .addCookie("na_tc", "Y")
//            .addCookie("uvc", "1474%7C11%2C1361%7C12%2C1162%7C13%2C739%7C14%2C332%7C15")
//            .addCookie("uid", "5c52b21d9f7161d1")
//            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
//            .addHeader("Accept-Encoding", "gzip, deflate, br")
//            .addHeader("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")
//            .addHeader("Cache-Control", "no-cache")
//            .addHeader("Connection", "keep-alive")
//            .addHeader("Host", "chan.sankakucomplex.com")
//            .addHeader("Pragma", "no-cach")
//            .addHeader("Upgrade-Insecure-Requests", "1")
//            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
//    // TODO: 4/17/2021 这一条很重要，添加了 cookie 会提示权限问题
////            .addHeader("cookie","_sankakucomplex_session=BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiUzMGE2MzUwYjQwZjI2YzIwM2I5ZTljMTRlMzMzOWZjNw==--4a0543378d4e7229153a3a4b9564f691fb9eeab9");

    public enum OrderType {

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

        OrderType(String key, String desc) {
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

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.out.println(URLDecoder.decode("combos_%26_doodles".trim(), "UTF8").replaceAll("_", " "));
    }
}
