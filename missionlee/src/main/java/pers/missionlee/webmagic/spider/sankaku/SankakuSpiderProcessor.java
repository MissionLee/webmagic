package pers.missionlee.webmagic.spider.sankaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private SankakuSpiderProcessor() {
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
                    urls[i]=pagedPathPrefixDec+(i-23);
                }
            }
        }

        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }
        SankakuInfoUtils sankakuInfoUtils = new SankakuInfoUtils(this);
        //this.sankakuInfoUtils = sankakuInfoUtils;
        DiarySankakuSpider sankakuSpider = new DiarySankakuSpider(site,sankakuInfoUtils,this);
        this.diarySankakuSpider = sankakuSpider;
        Spider spider = Spider.create(sankakuSpider);
        spider.addUrl(urls).thread(threadNum).run();
        // TODO: 2019/3/4  以上内容运行结束之后，重构对应作者的artistinfo
        sankakuInfoUtils.freshArtistInfo();
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
    public static void run(String tag,int num){
        String pa = "D:/sankaku";

        run(pa,tag,num,4);
    }
    public static void main(String[] args) {
        // thekite 120
        // santalol  59
        // sfmporn 331
        // matilda vin 235
        // the dude 339
        // ponkosfm 194
        // tortuga 190
        // nns (sobchan) 380
        // kabeu mariko 266
        // da-moment 35
        // orico 124
        // dragon@harry dragon%40harry 492
//        run("nishieda",1911);
//        // nishieda  1911
//        run("kachima",427);
//        // kachima  427
//        run("masak (masaki4545)",137);
//        // masak (masaki4545)  137
//        run("shirow masamune",1555);
//        // shirow masamune 1555
        run("yurisaki",87);
        // yurisaki  87
//        run("vempire",349);
//        // vempire 349
//        run("mappaninatta",204);
//        // mappaninatta 204
//        run("yewang19",62);
//        // yewang19 62
//        run("pharah-best-girl",519);
//        // pharah-best-girl  519
//        run("tinnies",279);
//        // tinnies 279
//        run("motokonut",156);
//        // motokonut 156
//        run("hitomiluv3r",705);
//        // kingbang 134
//        run("ogura anko",375);
//        // ogura anko 375
//        run("devilscry",147);
//        // devilscry  147
//        run("orion (orionproject)",809);
//        // orion (orionproject) 809
//        run("chong wuxin",172);
//        // chong wuxin 172
//        run("joixxx",180);
//        // joixxx 180
////        run("hitomiluv3r",705);
////        // pixiv id 18323769 30  重复
        run("feizaisfm",43);
        // feizaisfm 43
//        run("evulchibi",245);
//        // evulchibi 245
//        run("kruth666",193);
//        // kruth666 193
        run("mr h.l.",68);
        // mr h.l. 68
//        run("iri-neko",320);
//        // iri-neko 320
//        run("hk (hk)",198);
//        // hk (hk) 198
        run("temutemutemu",41);
        // temutemutemu 41
        run("hypnorekt",67);
        // hypnorekt 67
        run("karasu kame ex",44);
        // karasu kame ex 44
        run("hentaiborg",30);
        // hentaiborg 30
//        run("kakao",271);
//        // kakao 271
        run("jellytits7",64);
        // jellytits7 64
//        run("obaoba (monkeyix)",64);
        // obaoba (monkeyix) 56
        run("rikume",64);
        // rikume 64
//        run("zombie-andy",73);
//        // zombie-andy 73
////        run("hitomiluv3r",705);
////        // yan'yo (yan'yan'yo) yan%27yo_%28yan%27yan%27yo%29 524
//        run("sieyarelow",149);
//        // sieyarelow 149
//        run("sane-person",124);
//        // sane-person 124
//        run("rukitsura",202);
//        // rukitsura  202
//        run("dark arts kai",132);
//        // dark arts kai 132
//        run("dan-98",48);
//        // dan-98 48
//        run("imflain",195);
//        // imflain  195
//        run("ctrlz77",177);
//        // ctrlz77 177
//        run("kazenoko",425);
//        // kazenoko 425
//        run("haneto",189);
//        // haneto 189
//        run("q azieru",348);
//        // q azieru 348
//        run("furisuku",388);
//        // furisuku 388
//        run("momoko (momopoco)",491);
//        // momoko (momopoco) 491
//        run("prywinko",448);
//        // prywinko 448
//        run("user apfv5385",30);
//        // user apfv5385 30
//        run("gemuo",447);
//        // gemuo 447
//        run("ulrik",156);
//        // ulrik 156
//        run("ixmmxi",104);
//        // ixmmxi 104
//        run("untsue",581);
//        // untsue 581
//        run("aimee (emi)",91);
//        // aimee (emi) 91
//        run("ikomochi",242);
//        // ikomochi 242
//        run("andreygovno",68);
//        // andreygovno 68
//        run("akita hika",196);
//        // akita hika 196
////        run("hitomiluv3r",705);
////        // 1=2  %3D 146
//        run("pink crown",99);
//        // pink crown 99
//        run("ohayou girls",32);
//        // ohayou girls 32
//        run("yaziri",144);
//        // yaziri  144
//        run("spizzy",93);
//        // spizzy 93
//        run("thore (nathalukpol)",28);
//        // thore (nathalukpol)  28
//        run("unidentifiedsfm",218);
//        // unidentifiedsfm 218
//        run("tanishi (tani4)",271);
//        // tanishi (tani4) 271
//        run("maruwa tarou",316);
//        // maruwa tarou 316
//        run("eddysstash",120);
//        // eddysstash 120
//        run("cocq taichou",49);
//        // cocq taichou 49
//        run("eu03",824);
//        // eu03 824
//        run("soranamae",425);
//        // soranamae 425
//        run("tsoni",65);
//        // tsoni 65
//        run("muraosamu",41);
//        // muraosamu 41
//        run("damao yu",236);
//        // damao yu 236
//        run("universn",90);
//        // universn 90
//        run("kotani tomoyuki",339);
//        // kotani tomoyuki 339
//        run("dominothecat",121);
//        // dominothecat  121
//        run("badcompzero",505);
//        // badcompzero 505
//        run("hplay",241);
//        // hplay 241
//        run("ninjartist",81);
//        // ninjartist 81
//        run("hirume",60);
//        // hirume 60
//        run("afukuro",198);
//        // afukuro 198
//        run("kimdonga",166);
//        // kimdonga 166
//        run("snowball22",228);
//        // snowball22 228
//        run("ayya saparniyazova",301);
//        // ayya saparniyazova 301
//        run("vintem",166);
//        // vintem 166
//        run("hitomiluv3r",705);
//        // quadrastate  245
//        run("fuckhead",230);
//        // fuckhead 230
//        run("urbanator",73);
//        // urbanator  73
//        run("blueberg",248);
//        // blueberg 248
//        run("whitetentaclesfm",27);
//        // whitetentaclesfm 27
//        run("ox (baallore)",155);
//        // ox (baallore) 155
//        run("sumthindifrnt",61);
//        // sumthindifrnt 61
//        run("redapple2",43);
//        // redapple2 43
//        run("raxastake",42);
//        // raxastake 42
//        run("aurahack",310);
//        // aurahack 310
//        run("hitomiluv3r",163);
//        // pd (pdpdlv1)
//        run("mstivoy",136);
//        // mstivoy 136
//        run("honjou raita",538);
//        // honjou raita 538
//        run("ecoas",428);
//        // ecoas 428
//        run("shiory",496);
//        // shiory 496
//        run("nakano sora",566);
//        // nakano sora 566
//        run("kyuritizu",274);
//        // kyuritizu 274
//        run("ggli (yuine wantan)",118);
//        // ggli (yuine wantan) 118
//        run("fireboxstudio",265);
//        // fireboxstudio 265
//        run("rindou aya",628);
//        // rindou aya 628
//        run("xvladtepesx",35);
//        // xvladtepesx 35
//        run("zumi (zumidraws)",191);
//        // zumi (zumidraws)  191
//        run("nanao (mahaya)",474);
//        // nanao (mahaya) 474
//        run("firolian",402);
//        // firolian 402
//        run("c.cu",212);
//        // c.cu 212
//        run("fluffy pokemon",194);
//        // fluffy pokemon 194
//        run("gorgeous mushroom",346);
//        // gorgeous mushroom 346
//        run("kruel-kaiser",193);
//        // kruel-kaiser 193
//        run("lerapi",138);
//        // lerapi 138
//        run("instant-ip",284);
//        // instant-ip 284
//        run("hoobamon",235);
//        // hoobamon 235
//        run("liang xing",454);
//        // liang xing 454
//        run("love cacao",329);
//        // love cacao 329
//        run("jonathan hamilton",179);
//        // jonathan hamilton 179
//        run("likkezg",228);
//        // likkezg 228
//        run("lerico213",165);
//        // lerico213 165
//        run("caustic crayon",203);
//        // caustic crayon 203
//        run("raikoart",235);
//        // raikoart 235
//        run("marushin (denwa0214)",904);
//        // marushin (denwa0214)   904
//        run("rak (kuraga)",609);
//        // rak (kuraga)  609
//        run("monaim",114);
//        // monaim 114
//        run("pink lady mage",268);
//        // pink lady mage 268
//        run("miura naoko",315);
//        // miura naoko 315
//        run("ross tran",134);
//        // ross tran 134
//        run("onagi",447);
//        // onagi 447
//        run("fatcat17",149);
//        // fatcat17 149
//        run("sfmsnip",223);
//        // sfmsnip 223
//        run("orutoro",840);
//        // orutoro 840
//        run("dantewontdie",525);
//        // dantewontdie 525
//        run("bennemonte",187);
//        // bennemonte 187
//        run("steelxxxhotogi",127);
//        // steelxxxhotogi 127
//        run("daye bie qia lian",244);
//        // daye bie qia lian 244
//        run("darkholestuff",106);
//        // darkholestuff 106
//        run("forceballfx",124);
//        // forceballfx 124
//        run("hitomiluv3r",705);
//        // aoin 203
//        run("qosic",138);
////        // qosic 138
//        run("ask (askzy)",274);
////        // ask (askzy) 274
//        run("illusionk",167);
//        // illusionk 167
//        run("kagematsuri",313);
//        // kagematsuri 313
//        run("ouma tokiichi",749);
//        // ouma tokiichi  749
//        run("callmehaymaker",166);
//        // callmehaymaker 166
//        run("nababa (arata yokoyama)",290);
//        // nababa (arata yokoyama) 290
//        run("neromasin",186);
//        // neromasin 186
//        run("ion (cation)",122);
//        // ion (cation)  122
//        run("galian-beast",52);
//        // galian-beast 52
//        run("rasmus-the-owl",511);
//        // rasmus-the-owl 511
//        run("hitomiluv3r",705);
//        // homare (fool's art) 2100

        // materclaws 164
        // pokedudesfm 26
        // miaw34 34
        // redchicken 132
        // kazedesune 115
        // atdan 251
        // val-val 128
        // krabby (artist) 144
        // mattdarey91sfm  62
        // fainxel 59
        // nyuunzi 588
        // enosan 159
        // didi esmeralda 217
        // forged3dx 143
        // melon22 493
        // hanarito 252
        // etsem 41
        // mikiron 121
        // hayabusa 345
        // timpossible 381
        // surock 46
        // yuuji (and) 1305
        // rei kun 121
        // bdone 65
        // rayzoir 201
        // powergene 45
        // skello 40
        // doomsatan666 63
        // jeff macanoli 60
        // asa (teng zi) 42
        // hentaix  51
        // junkerz 61
        // zhean li 78
        // tim loechner 359
        // kishiyo 461
        // mrbonessfm 33
        // oiran ichimi 181
        // shimashima08123 156
        // 197 fuetakishi
    }
}
