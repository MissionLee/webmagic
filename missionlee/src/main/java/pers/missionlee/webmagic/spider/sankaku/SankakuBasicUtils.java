package pers.missionlee.webmagic.spider.sankaku;

import org.apache.commons.io.FileUtils;
import us.codecraft.webmagic.Site;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-24 22:01
 */
public class SankakuBasicUtils {
    public static String UserName = "zuixue3000@163.com";
    public static String Password = "mingshun1993";
    public static final String BASE_SITE = "https://chan.sankakucomplex.com/?tags=";
    private static final String chan_sankakucomplex_com_IP = "208.100.27.32";
    private static final String cs_sankakucomplex_com_IP = "208.100.24.254";
    protected static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("__atuvc", "1%7C11")
            .addCookie("__atuvs", "5c87c05942853024000")
            .addCookie("_pk_id.2.42fa", "adde0e4a1e63d583.1551189849.96.1552400777.1552396857.")
            .addCookie("_pk_ses.2.42fa", "1")
            .addCookie("_sankakucomplex_session", "BAh7CDoMdXNlcl9pZGkDKuwNOg9zZXNzaW9uX2lkIiVmZjAzNDhjNTk3NGZmMDAxZDhmZTkwMzI4ZjMzYmEyYiIKZmxhc2hJQzonQWN0aW9uQ29udHJvbGxlcjo6Rmxhc2g6OkZsYXNoSGFzaHsABjoKQHVzZWR7AA%3D%3D--4998ec89968de454aca9c6eea2068beac6cdf88c")
            .addCookie("auto_page", "0")
            .addCookie("blacklisted_tags", "")
            .addCookie("loc", "MDAwMDBBU0NOSlMyMTQ0Mjk4NDA3NjAwMDBDSA==")
            .addCookie("locale", "en")
            .addCookie("login", "hisanily")
            .addCookie("mode", "view")
            .addCookie("na_id", "2018122723475293368621024808")
            .addCookie("na_tc", "Y")
            .addCookie("ouid", "5c2564a80001da35a1ed736217e8a4379998383b2fa5f1877d3a")
            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
            .addCookie("uid", "5c2564a827f935b5")
            .addCookie("uvc", "9%7C5%2C0%7C6%2C3%7C7%2C13%7C8%2C46%7C9")
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
    public enum RunType{
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
        POPULAR("popular","POPULAR"),
        FAV_COUNT("favcount","FACOURITE_COUNT"),
        QUALITY("quality",""),
        FILESIZE_DEC("filesize",""),
        FILESIZE_ASC("filesize_asc",""),
        VIEW_COUNT("viewcount",""),
        MPIXELS_DEC("mpixels_desc",""),
        MPIXELS_ASC("mpixels_asc",""),
        PORTRAIT("portrait","高"),
        LANDSCAPE("landscape","宽");
        String key;
        String desc;

        public String getPrefix(String artistName, boolean offical) {
            return BASE_SITE+urlFormater(artistName,offical)+"%20order%3A"+key+"&page=";
        }

        public String getDesc() {
            return desc;
        }

        SITE_ORDER_PREFIX(String key, String desc) {
            this.key = key;
            this.desc = desc;
        }
    }
    protected static String urlFormater(String artistName,boolean offical) {
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
        return offical?(artistFormat+"%20official_art"):artistFormat;
    }
    protected static Map<String,Integer> sortNameList(Map<String,Integer> nameList){
        return sortNameList(nameList,false);
    }
    protected static Map<String, Integer> sortNameList(Map<String, Integer> namelist,boolean desc) {
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
                if(desc){
                    if (entries[j].getValue() > entries[k].getValue()) {
                        Map.Entry<String, Integer> tmp = entries[j];
                        entries[j] = entries[k];
                        entries[k] = tmp;
                    }
                }else{
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
}
