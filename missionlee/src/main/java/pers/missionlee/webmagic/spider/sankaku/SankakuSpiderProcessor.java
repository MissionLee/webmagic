package pers.missionlee.webmagic.spider.sankaku;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.UpdateInfo;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuDownloadSpider;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuNumberSpider;
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
public class SankakuSpiderProcessor extends SankakuBasicUtils {

    public static Logger logger = LoggerFactory.getLogger(SankakuSpiderProcessor.class);

    public static final String BASE_SITE = "https://chan.sankakucomplex.com/?tags=";

    public SankakuDownloadSpider diarySankakuSpider;
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

    private static Site site = Site.me()
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

    private SankakuSpiderProcessor() {
    }


    public static void runUpdate(String parentPath, int threadNum) throws IOException {
        if (!parentPath.endsWith("/"))
            parentPath = parentPath + "/";

        File root = new File(parentPath);
        File updateInfoFile = new File(parentPath + "update.json");
        UpdateInfo updateInfo = UpdateInfo.getUpdateInfo(updateInfoFile);
        if (updateInfo == null)
            updateInfo = new UpdateInfo();
        if (root.exists()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (updateInfo.needUpdate(files[i].getName())) {
                        System.out.println("NEED :  artist: " + files[i].getName() + "UPDATED:" + updateInfo.getUpdateDate(files[i].getName()));
                        int numberNow = getRealNumOfArtist(files[i].getName());
                        int numberStored = SankakuInfoUtils.getArtworkNumber(files[i]);
                        System.out.println("numberNow: " + numberNow + " numberStored: " + numberStored);
                        if (numberNow > numberStored) { // 如果需要更新的超过3个 开启更新
                            /**
                             * Spider 在检测到 查询url里面有date这个关键字的时候，就会自动触发更新检测机制，如果当前页面被更新了，那么下个页面就会被更新
                             * */
                            String startPage = BASE_SITE + urlFormater(files[i].getName()) + ORDER.date.getKey() + 1;
                            SankakuSpiderProcessor processor = new SankakuSpiderProcessor();
                            int num = processor.runDownloadSpider(parentPath, files[i].getName(), threadNum, startPage);
                            // 作品总数在2000以下，并且通过更新新作品没有获取完整作品，那么尝试更新整个内容
                            // 又各种排序方法，综合应用可以获得超过2000的内容，但是没必要
                            System.out.println("update num: " + num);
                            if (numberNow < 2000 && (num < numberNow)) {
                                processor.runAsNewWithTagNumOrder(parentPath, files[i].getName(), numberNow, threadNum);
                            }


                        }
                        updateInfo.update(files[i].getName());
                        updateInfo.writeUpdateInfo(updateInfoFile);
                    } else {
                        System.out.println("already updated artist: " + files[i].getName() + "UPDATED:" + updateInfo.getUpdateDate(files[i].getName()));
                    }


                }

            }
        }
    }

    private static Integer getRealNumOfArtist(String artistName) {
        String url = BASE_SITE + urlFormater(artistName);
        SankakuNumberSpider spider = new SankakuNumberSpider(site);
        Spider.create(spider).addUrl(url).thread(1).run();
        return spider.getNum();
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
    public int runAsNewWithTagNumOrder(String parentPath, String tag, int totalNum, int threadNum) throws IOException {
        if (!parentPath.endsWith("/"))
            parentPath = parentPath + "/";

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
        return runDownloadSpider(parentPath, tag, threadNum, urls);

    }

    /**
     * @Return int 返回当前作者作品总量
     */
    private int runDownloadSpider(String parentPath, String artistName, int threadNum, String... urls) throws IOException {
        SankakuDownloadSpider sankakuSpider = new SankakuDownloadSpider(site, parentPath, artistName);
        this.diarySankakuSpider = sankakuSpider;
        Spider spider = Spider.create(sankakuSpider);
        spider.addUrl(urls).thread(threadNum).run();
        // TODO: 2019/3/4  以上内容运行结束之后，重构对应作者的artistinfo
        ArtistInfo artistInfo = SankakuInfoUtils.freshArtistInfo(sankakuSpider.artworkInfos, parentPath + artistName, artistName);
        return artistInfo.getArtworkNum();
    }

    /**
     * ⭐⭐ ===========  通过作者列表文档 开启下载 ============ ⭐
     */
    public static void runWithNameList(String rootPath, String nameListPath, int threadNum) {
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        // rootPath
        try {
            File nameListFile = new File(nameListPath);
            File updateInfoFile = new File(rootPath + "update.json");
            UpdateInfo updateInfo = UpdateInfo.getUpdateInfo(updateInfoFile);

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
            Map<String, Integer> sortedMap = sortNameList(nameListMap);

            // TODO: 2019/3/9 把整理好的内容 重写到文件里面
            rewriteTodoList(nameListFile, sortedMap);
            Map<String, Integer> storageMap = new LinkedHashMap<String, Integer>(sortedMap);
            Set<String> set = sortedMap.keySet();
            Iterator iterator = set.iterator();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                // TODO: 2019/3/24 这里使用爬虫重置目标数量，如果获取到了 大于0的数值，就覆盖当前值
                int numUpdated = 0;
                try{
                    numUpdated = getRealNumOfArtist(key);
                }catch (Exception e){

                }


                int aimNum = sortedMap.get(key);
                if (numUpdated > 0) {
                    System.out.println("LIST NUM: " + sortedMap.get(key) + " REAL NUM: " + numUpdated);
                    aimNum = numUpdated;

                }

                SankakuSpiderProcessor processor = new SankakuSpiderProcessor();
                int num = processor.runAsNewWithTagNumOrder(rootPath
                        , key, aimNum, threadNum);
                // TODO: 2019/3/9  检查下载好的数量和总数量，下载好的与总数量 相差在（总量10%） 与 20中较小值，则删除这个key，把剩下内容写入文件，否则把当前
                int maxDiff = ((Double) Math.min(20, aimNum * 0.1)).intValue();
                System.out.println(maxDiff);
                if (aimNum - num <= maxDiff) {
                    System.out.println("aim:" + aimNum + "/doNum:" + num);
                    storageMap.remove(key);
                } else {
                    storageMap.remove(key);
                    storageMap.put(key, aimNum);
                }
                rewriteTodoList(nameListFile, storageMap);
                // 写入更新时间
                updateInfo.update(key);
                updateInfo.writeUpdateInfo(updateInfoFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void rewriteTodoList(File file, Map<String, Integer> info) {
        try {
//            FileUtils.writeStringToFile(file, "", "UTF8", false);
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

    public static void main(String[] args) {
        runWithNameList("F:\\sankaku","C:\\Users\\MissionLee\\Desktop\\totallist.md",4);
//        try {
//            runUpdate("D:\\sankaku", 4);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}