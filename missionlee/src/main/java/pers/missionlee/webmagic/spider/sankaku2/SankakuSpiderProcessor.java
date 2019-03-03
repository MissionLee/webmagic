package pers.missionlee.webmagic.spider.sankaku2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.ArtworkInfoUtils;
import pers.missionlee.webmagic.spider.sankaku.SankakuSpider;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:10
 */
public class SankakuSpiderProcessor {

    public static String UserName = "zuixue3000@163.com";
    public static String Password = "mingshun1993";

    public static Logger logger = LoggerFactory.getLogger(SankakuSpiderProcessor.class);
    private static final String chan_sankakucomplex_com_IP = "208.100.27.32";
    private static final String cs_sankakucomplex_com_IP = "208.100.24.254";

    public static final String BASE_SITE = "https://chan.sankakucomplex.com/?tags=";

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

    public int d_suc=0;
    public int d_err=0;
    public int d_skip = 0;
    private static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("__atuvc", "1%7C9")
            .addCookie("__atuvs", "5c791f63ddd2a1f5000")
            .addCookie("_pk_id.2.42fa", "adde0e4a1e63d583.1551189849.23.1551441764.1551440466..1551189849.16.1551362603.1551361875.")
            .addCookie("_pk_ses.2.42fa", "1")
            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiU5NWZhNGMyZjk2Y2M5MGJkZTNmOTZiMGM5ZmNmYzY3OQ%3D%3D--9d80a0ba02f9c4e31c13c7db0a08eb2cd035b80f%3D%3D--2d44e3f79213fc98bd4cb3b167394ecf18ded724")
            .addCookie("auto_page", "0")
            .addCookie("blacklisted_tags","")
            .addCookie("loc","MDAwMDBBU0NOSlMyMTQ0Mjk4NDA3NjAwMDBDSA==")
            .addCookie("locale", "en")
            .addCookie("login", "zuixue3000")
            .addCookie("mode", "view")
            .addCookie("na_id","2018122723475293368621024808")
            .addCookie("na_tc","Y")
            .addCookie("ouid","5c2564a80001da35a1ed736217e8a4379998383b2fa5f1877d3a")
            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
            .addCookie("uid", "5c2564a827f935b5")
            .addCookie("uvc","9%7C5%2C0%7C6%2C3%7C7%2C13%7C8%2C46%7C9")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Host", "chan.sankakucomplex.com")
            .addHeader("Pragma", "no-cach")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");


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
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/3/2
     */
    public  void runWithAllUrlAddedAtStart(String parentPath, String tag, int totalNum, int threadNum) {

        init(parentPath, tag, threadNum);

        int pageNum = (
                (Double) (
                        Math.ceil((new Double(totalNum)) / 20)
                )
        ).intValue();
        System.out.println("pageNUM:" + pageNum);
        String[] urls = new String[pageNum];
        String pagedPathPrefixAsc = BASE_SITE + tag.replace(" ", "_").replace("(", "%28").replace(")", "%29") + ORDER.tag_count_asc.getKey();
        String pagedPathPrefixDec = BASE_SITE + tag.replace(" ", "_").replace("(", "%28").replace(")", "%29") + ORDER.tag_count_dec.getKey();

        if(pageNum>50){
            for (int i = 0; i < pageNum; i++) {
                if ((i + 1) <= 50) {
                    urls[i] = pagedPathPrefixAsc + (i + 1);
                } else {
                    urls[i] = pagedPathPrefixDec + (i - 49);
                }
            }
        }else {
            for (int i = 0; i < pageNum; i++) {
                if((i+1)<=25){
                    urls[i]=pagedPathPrefixAsc+(i+1);
                }else{
                    urls[i]=pagedPathPrefixDec+(i-24);
                }
            }
        }

        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }
        DiarySankakuSpider sankakuSpider = new DiarySankakuSpider(site,new SankakuInfoUtils(this),this);
        Spider spider = Spider.create(sankakuSpider);
        spider.addUrl(urls).thread(threadNum).run();
    }

    private void init(String parentPath, String tag, int threadNum) {
        ROOT_PATH = parentPath;
        TAG = tag;
        THREAD_NUM = threadNum;
        // 本地文件预检测/处理
    }
    public static void run(String parentPath, String tag, int totalNum, int threadNum){
        String rootPath = parentPath;
        if(!parentPath.endsWith("/"))
            rootPath = parentPath+"/";
        SankakuSpiderProcessor processor = new SankakuSpiderProcessor();
        processor.runWithAllUrlAddedAtStart(rootPath,tag,totalNum,threadNum);
    }
    public static void main(String[] args) {
        String pa = "C:\\Users\\Administrator\\Desktop\\sankaku";


//        // yang-do 1021
//        // logan cure 455
//        // zalsfm 112
        // sayika 149
        // songjikyo 291
        // xiaoshou xiansheng 137
        // stanley lau  637
        // yamakawa 267
//        secaz  426
        // strapy 388
        // viola (seed) 308
        // tokinohimitsu 176
        // usaginagomu 182
        // saejin oh 153
        // v1z3t4 201
        // zumi (zumidraws) 190
        // tarakanovich 226
        // masak (masaki4545) 137
        // m-rs 410
        // phamoz 138
        // pockyin 288
        // pyz (cath x tech) 207
        // mu-nyako 147
        // nanoless 301
        // pewposterous 797
        // niodreth  181
        // metagraphy 126
        // guweiz 356
        // leslyzerosix 215
        // lesdias 279
        // fluffy pokemon 194
        // gorgeous mushroom 346
        // kruel-kaiser 193
        // lerapi 138
        // instant-ip 284
        // hoobamon 235
        // liang xing 454
        // love cacao 329
        // jonathan hamilton 179
        // likkezg 228
        // lerico213 165
        // raikoart 235
        // marushin (denwa0214)   904
        // rak (kuraga)  609
        // monaim 114
        // pink lady mage 268
        // miura naoko 315
        // ross tran 134
        // onagi 447
        // fatcat17 149
        // sfmsnip 223
        // orutoro 840




//        run("D:/sankaku/","sasaoka gungu",261,4);
//        // sasaoka gungu 261
//        run("D:/sankaku/","yeero",207,4);
//        // yeero 207
//        run("D:/sankaku/","vgerotica",263,4);
//        // vgerotica 263
//        run("D:/sankaku/","tiazsfm",169,4);
//        // tiazsfm 169
//        run("D:/sankaku/","cakeofcakes",186,4);
        // cakeofcakes 186
        run("D:/sankaku","",15,4);

        // bennemonte 187
        // steelxxxhotogi 127
        // daye bie qia lian 244

    }
}
