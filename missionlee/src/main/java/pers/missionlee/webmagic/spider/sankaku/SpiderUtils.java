package pers.missionlee.webmagic.spider.sankaku;

import org.apache.commons.io.FileUtils;
import us.codecraft.webmagic.Site;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-24 22:01
 */
public class SpiderUtils {
    public static String UserName = "zuixue3000@163.com";
    public static String UserName2 = "hisanily";
    public static String Password = "mingshun1993";
    public static final String BASE_SITE = "https://chan.sankakucomplex.com/?tags=";
    private static final String www_sankakucomplex_com_IP = "208.100.24.252";
    private static final String chan_sankakucomplex_com_IP = "208.100.27.32";
    private static final String cs_sankakucomplex_com_IP = "208.100.24.254";
    protected static Site site = Site.me()
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

    public enum RunType {
        RUN_WITH_ARTIST_NAMElIST,
        RUN_WITH_COPYRIGHT_NAMELIST,
        RUN_WITH_ARTIST_NAME,
        RUN_WITH_COPYRIGHT_NAME,
        UPDATE_ARTIST,
        UPDATE_COPYRIGHT
    }

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

        public String getPrefix(String artistName, boolean offical) {
            return BASE_SITE + urlFormater(artistName, offical) + "%20order%3A" + key + "&page=";
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

    public static String urlFormater(String artistName, boolean offical) {
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
        return offical ? (artistFormat + "%20official_art") : artistFormat;
    }

    protected static Map<String, Integer> sortNameList(Map<String, Integer> nameList) {
        return sortNameList(nameList, false);
    }

    protected static Map<String, Integer> sortNameList(Map<String, Integer> namelist, boolean desc) {
        Set<Map.Entry<String, Integer>> valueSet = namelist.entrySet();
        Map.Entry<String, Integer>[] entries = new Map.Entry[namelist.size()];
        Iterator iterator = valueSet.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            entries[i++] = (Map.Entry<String, Integer>) iterator.next();
        }
        int length = namelist.size();
        for (int j = 0; j < length; j++) {
            for (int k = 0; k < length; k++) {
                if (desc) {
                    if (entries[j].getValue() > entries[k].getValue()) {
                        Map.Entry<String, Integer> tmp = entries[j];
                        entries[j] = entries[k];
                        entries[k] = tmp;
                    }
                } else {
                    if (entries[j].getValue() < entries[k].getValue()) {
                        Map.Entry<String, Integer> tmp = entries[j];
                        entries[j] = entries[k];
                        entries[k] = tmp;
                    }
                }

            }
        }
        Map<String, Integer> aimMap = new LinkedHashMap<String, Integer>();
        for (int j = 0; j < entries.length; j++) {
            aimMap.put(entries[j].getKey(), entries[j].getValue());
        }
        return aimMap;
    }

    protected static void rewriteTodoList(File file, Map<String, Integer> info) {
        try {
            StringBuffer stringBuffer = new StringBuffer();
            Set<String> set = info.keySet();
            for (String key :
                    set) {
                stringBuffer.append(key + " " + info.get(key) + "\n");
            }
            FileUtils.writeStringToFile(file, stringBuffer.toString(), "UTF8", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给定 作者名称 与 是否 official 是否 为更新 来获取 目标url
     * 其中 整体下载的情况下（非update） 可以通过爬虫自动获取 目标数量
     */
    protected String[] urlGenertor(String artist, boolean offical, int artworkNum) {
        System.out.println("初始URL生成器，目标数量 " + artworkNum);
        String[] urls;
        // TODO: 2020-01-13 sankaku网站变了 tag count 直接跳转某个页面应该有检测（目测是有bug）,部分排序结果 一个页面只有一两个结果，甚至么有结果了
        String BaseTagAsc = SITE_ORDER_PREFIX.TAG_COUNT_ASC.getPrefix(artist, offical);
        String BaseTagDec = SITE_ORDER_PREFIX.TAG_COUNT_DEC.getPrefix(artist, offical);
        String BaseDate = SITE_ORDER_PREFIX.DATE.getPrefix(artist, offical);
        String BasePopular = SITE_ORDER_PREFIX.POPULAR.getPrefix(artist, offical);
        String BaseQurlity = SITE_ORDER_PREFIX.QUALITY.getPrefix(artist, offical);
        String BaseFav = SITE_ORDER_PREFIX.FAV_COUNT.getPrefix(artist, offical);
        String BaseFilesizeAsc = SITE_ORDER_PREFIX.FILESIZE_ASC.getPrefix(artist, offical);
        String BaseFilesizeDes = SITE_ORDER_PREFIX.FILESIZE_DEC.getPrefix(artist, offical);
        String BaseLandscape = SITE_ORDER_PREFIX.LANDSCAPE.getPrefix(artist, offical);
        String BaseMpixelsAsc = SITE_ORDER_PREFIX.MPIXELS_ASC.getPrefix(artist, offical);
        String BaseMpixelsDec = SITE_ORDER_PREFIX.MPIXELS_DEC.getPrefix(artist, offical);
        String BaseProtrait = SITE_ORDER_PREFIX.PORTRAIT.getPrefix(artist, offical);
        String BaseView = SITE_ORDER_PREFIX.VIEW_COUNT.getPrefix(artist, offical);
        if (artworkNum > 2000) {// 大于2000 只能用尽量覆盖
            urls = new String[400];
            for (int i = 0; i < 50; i++) {
                urls[i] = BaseTagAsc + (i + 1);
                urls[i + 50] = BaseTagDec + (i + 1);
                urls[i + 100] = BaseDate + (i + 1);
                urls[i + 150] = BaseQurlity + (i + 1);
                urls[i + 200] = BasePopular + (i + 1);
                urls[i + 250] = BaseFav + (i + 1);
                urls[i + 300] = BaseFilesizeAsc + (i + 1);
                urls[i + 350] = BaseFilesizeDes + (i + 1);
//                urls[i + 400] = BaseLandscape + (i + 1);
//                urls[i + 450] = BaseMpixelsAsc + (i + 1);
//                urls[i + 500] = BaseMpixelsDec + (i + 1);
            }
            
        } else if (artworkNum > 1000) { // 1000 - 2000 一个条件的升降就可以覆盖
            urls = new String[100];
            for (int i = 0; i < 50; i++) {
                urls[i] = BaseTagAsc + (i + 1);
                urls[i + 50] = BaseTagDec + (i + 1);
//                urls[i + 100] = BaseDate + (i + 1);
//                urls[i + 150] = BaseQurlity + (i + 1);
//                urls[i + 200] = BasePopular + (i + 1);
            }

        } else { // 1-1000
            int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
            urls = new String[pageNum];
//            urls = new String[pageNum * 3];
            // TODO: 2020-01-13  popular 应该不会有问题，所以不需要做额外限制
            for (int i = 0; i < pageNum; i++) {
                urls[i] = BasePopular + (i + 1);
//                urls[i + pageNum] = BaseDate + (i + 1);
//                urls[i + pageNum * 2] = BaseQurlity + (i + 1);
            }
        }
        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }
        return urls;
    }

    private static Map<String, String> SPECIAL_NAME = new HashMap<String, String>() {{
        put("rib:y(uhki)", "rib_y(uhki)");
    }};

    public static String fileNameGenerator(String artistName) {
        artistName = artistName.trim();
        if (SPECIAL_NAME.containsKey(artistName))
            return SPECIAL_NAME.get(artistName);
        else {
            return artistName.endsWith(".") ? artistName.substring(0, artistName.length() - 1) : artistName;
        }
    }

}
