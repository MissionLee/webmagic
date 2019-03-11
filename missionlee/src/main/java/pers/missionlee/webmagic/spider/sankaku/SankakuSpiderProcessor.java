package pers.missionlee.webmagic.spider.sankaku;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.omg.PortableInterceptor.INACTIVE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:10
 */
public class SankakuSpiderProcessor {
    // TODO: 2019/3/9 整改，Processor存储全局内容


    public static String UserName = "zuixue3000@163.com";
    public static String Password = "mingshun1993";

    public static Logger logger = LoggerFactory.getLogger(SankakuSpiderProcessor.class);
    private static final String chan_sankakucomplex_com_IP = "208.100.27.32";
    private static final String cs_sankakucomplex_com_IP = "208.100.24.254";

    public static final String BASE_SITE = "https://chan.sankakucomplex.com/?tags=";

    public DiarySankakuSpider diarySankakuSpider;
    public SankakuInfoUtils sankakuInfoUtils;

    public enum ORDER {
        date("%20order%3Adate&page=", "DATE"),
        tag_count_dec("%20order%3Atagcount&page=", "TAG_COUNT_DEC"),
        tag_count_asc("%20order%3Atagcount_asc&page=", "TAG_COUNT_ASC");
        String key;
        String desc;

        public String getKey() {
            return key;
        }

        public String getDesc() {
            return desc;
        }

        ORDER(String key, String desc) {
            this.key = key;
            this.desc = desc;
        }
    }

    // 构造参数
    public String ROOT_PATH = "D:/sankaku/";
    public int THREAD_NUM = 3;

    public String TAG = "other";

    public int d_suc = 0;
    public int d_err = 0;
    public int d_skip = 0;
    private static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("__atuvc", "1%7C10")
            .addCookie("__atuvs", "5c791f63ddd2a1f5000")
            .addCookie("_pk_id.2.42fa", "adde0e4a1e63d583.1551189849.23.1551441764.1551440466..1551189849.16.1551362603.1551361875.")
            .addCookie("_pk_ses.2.42fa", "1")
            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiU5NWZhNGMyZjk2Y2M5MGJkZTNmOTZiMGM5ZmNmYzY3OQ%3D%3D--9d80a0ba02f9c4e31c13c7db0a08eb2cd035b80f%3D%3D--2d44e3f79213fc98bd4cb3b167394ecf18ded724")
            .addCookie("auto_page", "0")
            .addCookie("blacklisted_tags", "")
            .addCookie("loc", "MDAwMDBBU0NOSlMyMTQ0Mjk4NDA3NjAwMDBDSA==")
            .addCookie("locale", "en")
            .addCookie("login", "zuixue3000")
            .addCookie("mode", "view")
            .addCookie("na_id", "2018122723475293368621024808")
            .addCookie("na_tc", "Y")
            .addCookie("ouid", "5c2564a80001da35a1ed736217e8a4379998383b2fa5f1877d3a")
            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
            .addCookie("uid", "5c2564a827f935b5")
            .addCookie("uvc", "9%7C5%2C0%7C6%2C3%7C7%2C13%7C8%2C46%7C9")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Host", "chan.sankakucomplex.com")
            .addHeader("Pragma", "no-cach")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");

    private SankakuSpiderProcessor() {
    }

    public void runUpdate(String parentPath,  int pageNum, int threadNum) throws IOException {
        if (!parentPath.endsWith("/"))
            parentPath = parentPath + "/";

        File root = new File(parentPath);
        if (root.exists()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                init(parentPath, files[i].getName(), threadNum);
                String pagePrefix = BASE_SITE + urlFormater(files[i].getName()) + ORDER.date.getKey();
                String[] urls = new String[pageNum];
                for (int j = 0; j < pageNum; j++) {
                    urls[j] = pagePrefix + (j+1);
                }
                runSpider(threadNum,urls);
            }
        }
    }

    /**
     * @Description: 用于自动配置下载一个作者的所有作品，再登录判定通过的情况下，可下载
     * 2000 上限，登录信息失效可以查找 1000上限
     * ----- 但是需要变动参数  -----
     * 会先生成所有入口url一起添加到 scheduler中
     * // TODO: 2019/3/2 只在所有爬虫运行完毕之后才保存一次信息，
     * 如果页面多运行中间发生错误程序崩溃或者锁死或者网络条件差，
     * 程序中断恢复运行的成本较高，必须重新遍历所有页面才能重新
     * 获取信息（不用重新下载文件）
     * @Param: [parentPath, tag, totalNum, threadNum]
     * @return: int 收录的总数
     * @Author: Mission Lee
     * @date: 2019/3/2
     */
    public int runAsNew(String parentPath, String tag, int totalNum, int threadNum) throws IOException {
        if (!parentPath.endsWith("/"))
            parentPath = parentPath + "/";
        init(parentPath, tag, threadNum);

        int pageNum = (
                (Double) (
                        Math.ceil((new Double(totalNum)) / 20)
                )
        ).intValue();
        System.out.println("pageNUM:" + pageNum);
        String[] urls = new String[pageNum];
        String formativeTag = urlFormater(tag);
        String pagedPathPrefixAsc = BASE_SITE + formativeTag + ORDER.tag_count_asc.getKey();
        String pagedPathPrefixDec = BASE_SITE + formativeTag + ORDER.tag_count_dec.getKey();

        if (pageNum > 50) {
            for (int i = 0; i < pageNum; i++) {
                if ((i + 1) <= 50) {
                    urls[i] = pagedPathPrefixAsc + (i + 1);
                } else {
                    urls[i] = pagedPathPrefixDec + (i - 49);
                }
            }
        } else {
            for (int i = 0; i < pageNum; i++) {
                if ((i + 1) <= 25) {
                    urls[i] = pagedPathPrefixAsc + (i + 1);
                } else {
                    urls[i] = pagedPathPrefixDec + (i - 24);
                }
            }
        }

        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }
        return runSpider(threadNum, urls);

    }

    private int runSpider(int threadNum, String[] urls) throws IOException {
        DiarySankakuSpider sankakuSpider = new DiarySankakuSpider(site, this);
        this.diarySankakuSpider = sankakuSpider;
        Spider spider = Spider.create(sankakuSpider);
        spider.addUrl(urls).thread(threadNum).run();
        // TODO: 2019/3/4  以上内容运行结束之后，重构对应作者的artistinfo
        ArtistInfo artistInfo = SankakuInfoUtils.freshArtistInfo(sankakuSpider.artworkInfos, ROOT_PATH + "/" + TAG, TAG);
        return artistInfo.getArtworkNum();
    }

    private void init(String parentPath, String tag, int threadNum) {
        ROOT_PATH = parentPath;
        TAG = tag;
        THREAD_NUM = threadNum;
        // 本地文件预检测/处理
    }

    @Deprecated
    public static void run(String parentPath, String tag, int totalNum, int threadNum) throws IOException {
        String rootPath = parentPath;
        if (!parentPath.endsWith("/"))
            rootPath = parentPath + "/";
        SankakuSpiderProcessor processor = new SankakuSpiderProcessor();

        processor.runAsNew(rootPath, tag, totalNum, threadNum);


    }

    @Deprecated
    public static void run(String... strings) {
        int length = strings.length;

        if (length > 0) {
            for (int i = 0; i < length; i++) {
                String name = strings[i].substring(0, strings[i].lastIndexOf("("));
                int num = Integer.valueOf(strings[i].substring(strings[i].lastIndexOf("(") + 1, strings[i].length() - 1).replace(",", ""));
                System.out.println("name:" + name + "/num:" + num);
            }
        }
    }

    private static String urlFormater(String urlFragment) {
        // 空格 () ’
        return urlFragment.trim()
                .replaceAll(" ", "_")// !important 这里吧空格对应成了下划线，是sankaku的特别处理方法
                .replaceAll(" ", "%20")
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
    }

    @Deprecated
    public static void run(String tag, int num) throws IOException {
        String pa = "D:/sankaku";

        run(pa, tag, num, 4);
    }

    static Pattern pattern = Pattern.compile("\\d+");

    public static void runWithNameList() {
        try {
            File nameListFile = new File("C:\\Users\\Administrator\\Desktop\\list.md");
            String nameListString = FileUtils.readFileToString(nameListFile, "UTF8");
            String[] nameListArray = nameListString.split("\n");
            int length = nameListArray.length;
            Map<String, Integer> nameListMap = new LinkedHashMap<String, Integer>();
            int nn = 0;
            for (int i = 0; i < length; i++) {
                String str = nameListArray[i].trim();
                if (!StringUtils.isEmpty(str))
                    if (!str.contains("run")) {
                        while (str.startsWith("//"))
                            str = str.substring(2).trim();
                        int lastIndex = str.lastIndexOf(" ");
                        if (lastIndex != -1) {
                            String name = str.substring(0, str.lastIndexOf(" ")).trim();
                            String num = str.substring(str.lastIndexOf(" ")).trim().replaceAll("\\(", "").replaceAll("\\)", "").replaceAll(",", "");
                            if (Pattern.matches("\\d+", num)) {
                                nameListMap.put(name, Integer.valueOf(num));
                            }

                        }
                    }
            }
            Map<String,Integer> sortedMap = sortNameList(nameListMap);

            // TODO: 2019/3/9 把整理好的内容 重写到文件里面
            rewriteTodoList(nameListFile, sortedMap);
            Map<String, Integer> storageMap = new LinkedHashMap<String, Integer>(sortedMap);
            Set<String> set = sortedMap.keySet();
            Iterator iterator = set.iterator();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                int aimNum = sortedMap.get(key);
                SankakuSpiderProcessor processor = new SankakuSpiderProcessor();
                int num = processor.runAsNew("D:/sankaku"
                        , key, sortedMap.get(key), 4);
                // TODO: 2019/3/9  检查下载好的数量和总数量，下载好的与总数量 相差在（总量10%） 与 20中较小值，则删除这个key，把剩下内容写入文件，否则把当前
                int maxDiff = ((Double) Math.min(20, aimNum * 0.1)).intValue();
                System.out.println(maxDiff);
                if (aimNum - num <= maxDiff) {
                    System.out.println("aim:"+aimNum+ "/doNum:"+num);
                    storageMap.remove(key);
                } else {
                    storageMap.remove(key);
                    storageMap.put(key, aimNum);
                }
                rewriteTodoList(nameListFile, storageMap);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Map<String,Integer> sortNameList(Map<String,Integer> namelist){
        System.out.println(namelist.size());
        Set<Map.Entry<String, Integer>> valueSet = namelist.entrySet();
        Map.Entry<String,Integer>[] entries = new Map.Entry[namelist.size()];
        Iterator iterator = valueSet.iterator();
        int i = 0;
        while (iterator.hasNext()){
            entries[i++] = (Map.Entry<String, Integer>) iterator.next();
        }
        int length =namelist.size();
        for (int j = 0; j < length; j++) {
            for (int k = 0; k < length; k++) {
                if(entries[j].getValue()<entries[k].getValue()){
                    Map.Entry<String,Integer> tmp = entries[j];
                    entries[j]=entries[k];
                    entries[k]=tmp;
                }
            }
        }
        Map<String,Integer> aimMap = new LinkedHashMap<String, Integer>();
        for (int j = 0; j < entries.length; j++) {
            aimMap.put(entries[j].getKey(),entries[j].getValue());
        }
        System.out.println(aimMap);
        System.out.println(aimMap.size());
        return aimMap;
    }
    private static void rewriteTodoList(File file, Map<String, Integer> info) {
        try {
            FileUtils.writeStringToFile(file, "", "UTF8", false);
            Set<String> set = info.keySet();
            for (String key :
                    set) {
                FileUtils.writeStringToFile(file, (key + " " + info.get(key) + "\n"), "UTF8", true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        runWithNameList();
//        SankakuSpiderProcessor processor = new SankakuSpiderProcessor();
//        try {
//            processor.runUpdate("D:/sankaku",6,3);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}