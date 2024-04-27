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
    public static  String BASE_SEARCH_URL = "https://chan.sankakucomplex.com/en/?tags=";
    public static String  BASE_CHAN_URL="https://chan.sankakucomplex.com/en/";
    public static String BASE_SEARCH_CHAN_URL="https://chan.sankakucomplex.com/en/?tags=";
    public static String BASE_IDOL_URL="https://idol.sankakucomplex.com";
    public static  String BASE_SEARCH_IDOL_URL="https://idol.sankakucomplex.com/en/?tags=";

    public static String BASE_BOOK_URL="beta.sankakucomplex.com";
    public static Pattern htmlTextPattern = Pattern.compile(">(.+?)<");
    public static Pattern htmlTitlePattern = Pattern.compile("title=\"(.+?)\"");
    public static Pattern resolutionPattern = Pattern.compile("bytes\">(.+?)<");
    public static boolean vipMode = false;
    public static int allowedPageNum = 25;
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
    public static String getNextModeUrl(String... keys){
        StringBuffer prefix = new StringBuffer();
        prefix.append(BASE_SEARCH_URL);
        for (int i = 0; i < keys.length; i++) {
            prefix.append(urlFormater(keys[i]));
            if(i==(keys.length-1)){

            }else
            prefix.append("%20");
        }
        return prefix.toString();
    }
    @Deprecated
    public static String[] getStartUrlsDateBest(int artworkNum,String... keys ){
        int pageNum  = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
        if(pageNum>allowedPageNum) pageNum = allowedPageNum;
        String[] urls = new String[pageNum];
        String prefix = OrderType.DATE.getPrefix(keys);
        for (int i = 0; i < pageNum; i++) {
            urls[i] = prefix + (i + 1);
        }
        return urls;
    }
    public static String[] getStartUrlsTryBestNextMode(int num,String... keys){
        String urls[];
        boolean official = false;
        // tip 没有 nextMode之前 作品数量探测爬虫没法在多个标签模式下获得准确作品数量
        //     nextMode模式下，没有这个问题
        if(num>50*20){// 常规模式（按日期）可以访问前50页，超过这个限度，适量增加其他下载模式

            urls = new String[4];
            urls[0] = getNextModeUrl(keys); // 常规（即日期）
            urls[1] = OrderType.POPULAR.getPrefix(keys)+"1";
            urls[2] = OrderType.QUALITY.getPrefix(keys)+"1";
            urls[3] = OrderType.POPULAR.getPrefix(keys)+"1";
        }else {
            String startUrl = getNextModeUrl(keys);
            urls = new String[1];
            urls[0] = startUrl;
        }
        return urls;
    }
    @Deprecated
    public static String[] getStartUrlsTryBest(int artworkNum,String... keys){
        String urls[];
        boolean official = false;
        for (int i = 0; i < keys.length; i++) {
            if("official art".equals(keys[i])){
                official = true;
                System.out.println("XXXXXXXXXXXXXX  发现official art 标签，和copyright相关，会被采集到一个非常大的非official作品量，系统特定只作为 20*allowedPageNum 正负tag——count");
            }
        }

        if (artworkNum > allowedPageNum*2*20 && !official) {
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
            urls = new String[11*allowedPageNum];
            for (int i = 0; i < allowedPageNum; i++) {
                urls[i] = prefixPop + (i + 1);
                urls[i + allowedPageNum] = prefixQuality + (i + 1);
                urls[i + allowedPageNum*2] = prefixMegAsc + (i + 1);
                urls[i + allowedPageNum*3] = prefixMegDec + (i + 1);
                urls[i + allowedPageNum*4] = prefixFileSizeAsc + (i + 1);
                urls[i + allowedPageNum*5] = prefixFileSizeDec + (i + 1);
                urls[i + allowedPageNum*6] = prefixTagAsc + (i + 1);
                urls[i + allowedPageNum*7] = prefixTagDec + (i + 1);
                urls[i + allowedPageNum*8] = prefixDate + (i + 1);
                urls[i + allowedPageNum*9] = prefixLandScap + (i + 1);
                urls[i + allowedPageNum*10] = prefixPor + (i + 1);
            }

        } else if (artworkNum > allowedPageNum*20) {
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            int loopNum = ((Double) Math.ceil(new Double(pageNum) / 2)).intValue();
            if(loopNum>allowedPageNum){
                loopNum = allowedPageNum; //  ⭐⭐ 这个是用来处理 上面official art判断留下的问题
            }
            String prefixAsc = OrderType.TAG_COUNT_ASC.getPrefix(keys);
            String prefixDesc = OrderType.TAG_COUNT_DEC.getPrefix(keys);
            String prefixDate = OrderType.DATE.getPrefix(keys);

            urls = new String[loopNum * 2];
            if(official){
                urls = new String[loopNum * 3];

            }
            for (int i = 0; i < loopNum; i++) {
                urls[i] = prefixAsc + (i + 1);
                urls[i + loopNum] = prefixDesc + (i + 1);
                if(official){
                    urls[i + loopNum*2] = prefixDate + (i + 1);

                }
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
    @Deprecated
    public static String[] getStartUrls(int artworkNum, String... keys) {
        String urls[];
        boolean official = false;
        for (int i = 0; i < keys.length; i++) {
            if("official art".equals(keys[i])){
                official = true;
                System.out.println("XXXXXXXXXXXXXX  发现official art 标签，和copyright相关，会被采集到一个非常大的非official作品量，系统特定只作为 20*allowedPageNum 正负tag——count");
            }
        }
        if (artworkNum > allowedPageNum*20*2 && !official) {
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
            urls = new String[allowedPageNum*5];
            for (int i = 0; i < allowedPageNum; i++) {
                urls[i] = prefixPop + (i + 1);
                urls[i + allowedPageNum] = prefixQuality + (i + 1);
//                urls[i + 100] = prefixMegAsc + (i + 1);
//                urls[i + 150] = prefixMegDec + (i + 1);
//                urls[i + 200] = prefixFileSizeAsc + (i + 1);
//                urls[i + 250] = prefixFileSizeDec + (i + 1);
                urls[i + allowedPageNum*2] = prefixTagAsc + (i + 1);
                urls[i + allowedPageNum*3] = prefixTagDec + (i + 1);
                urls[i + allowedPageNum*4] = prefixDate + (i + 1);
//                urls[i + 450] = prefixLandScap + (i + 1);
//                urls[i + 500] = prefixPor + (i + 1);
            }

        } else if (artworkNum > allowedPageNum*20) {
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            int loopNum = ((Double) Math.ceil(new Double(pageNum) / 2)).intValue();
            if(loopNum>allowedPageNum){
                loopNum = allowedPageNum;
                //  ⭐⭐ 这个是用来处理 上面official art判断留下的问题
            }
            String prefixAsc = OrderType.TAG_COUNT_ASC.getPrefix(keys);
            String prefixDesc = OrderType.TAG_COUNT_DEC.getPrefix(keys);
            String prefixDate = OrderType.DATE.getPrefix(keys);

            urls = new String[loopNum * 2];
            if(official){
                urls = new String[loopNum * 3];

            }
            for (int i = 0; i < loopNum; i++) {
                urls[i] = prefixAsc + (i + 1);
                urls[i + loopNum] = prefixDesc + (i + 1);
                if(official){
                    urls[i + loopNum*2] = prefixDate + (i + 1);

                }
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
            .addHeader("Accept", "text/html, */*; q=0.01")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "en")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Host", "chan.sankakucomplex.com")
            .addHeader("Pragma", "no-cach")
            .addHeader("Sec-Ch-Ua","\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
            .addHeader("Sec-Ch-Ua-Mobile","?0")
            .addHeader("Sec-Ch-Ua-Platform","\"Windows\"")
            .addHeader("Sec-Fetch-Dest","empty")
            .addHeader("Sec-Fetch-Mode","cors")
            .addHeader("Sec-Fetch-Site","same-origin")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
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
