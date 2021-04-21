package pers.missionlee.chan.spider.book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.AbstractPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-15 19:34
 */
public class ArtistBookListProcessor extends AbstractPageProcessor {
    static Logger logger = LoggerFactory.getLogger(ArtistBookListProcessor.class);
    public static String CHAN_WIKI_PREFIX = "https://beta.sankakucomplex.com/wiki/en/";
    List<String> bookUrlList;
    boolean onlyNew;
    double autoBookSkipPercent = 1;// 某个book 作品多余这个数，就不再下载
    double bookSkipPercent = 0.8;// 某个book 缺失作品少于这个数，就不下载

    public ArtistBookListProcessor(List<String> bookUrlList, boolean onlyNew, DataBaseService dataBaseService, DiskService diskService,double autoBookSkipPercent, double bookSkipPercent ) {
        super(dataBaseService, diskService);
        this.bookUrlList = bookUrlList;
        this.onlyNew = onlyNew;
        this.autoBookSkipPercent = autoBookSkipPercent;
        this.bookSkipPercent = bookSkipPercent;
    }
    public ArtistBookListProcessor(List<String> bookUrlList, boolean onlyNew, DataBaseService dataBaseService, DiskService diskService,double bookSkipPercent) {
        super(dataBaseService, diskService);
        this.bookUrlList = bookUrlList;
        this.onlyNew = onlyNew;
        this.bookSkipPercent = bookSkipPercent;
    }
    public ArtistBookListProcessor(List<String> bookUrlList, boolean onlyNew, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        this.bookUrlList = bookUrlList;
        this.onlyNew = onlyNew;
    }

    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    String artistName;

    @Override
    public void doProcess(Page page) {
        String url = page.getUrl().toString();
        if (url.startsWith(CHAN_WIKI_PREFIX)) {
            String artistName = url.substring(url.lastIndexOf("/") + 1);
            artistPathName = artistName;
            this.artistName = artistName.replaceAll("_", " ");
            logger.info("检测到Wiki页面，即将爬取作品列表页面:" + this.artistName);
            page.addTargetRequest(CHAN_ARTIST_BOOK_LIST_PREFIX + "?lang=en&limit=20&tags=order:popularity+" + artistName + "&pool_type=0");
        } else if (url.startsWith(CHAN_ARTIST_BOOK_LIST_PREFIX)) {
            Map<String, Object> res = getJsonStringFromRestPage(page);
            // 处理返回数据的  data 字段  data字段就是一个 放着 book信息的 list
            List<Map<String, Object>> bookList = (List<Map<String, Object>>) res.get("data");
            for (int i = 0; i < bookList.size(); i++) {
                logger.info("列表页面：Book信息JSON   " + bookList.get(i));
                Map<String, Object> bookInfo = bookList.get(i);
                int bookId = (int) bookInfo.get("id");
                int postCount = (int) bookInfo.get("visible_post_count");
                String bookName = (String) bookInfo.get("name");
                ArtworkInfo artworkInfo = new ArtworkInfo();
                artworkInfo.bookId = bookId;
                artworkInfo.bookName = bookName;
                artworkInfo.isSingle = false;
                artworkInfo.aimName = this.artistName;
                artworkInfo.fileName = "1.jpg";
                artworkInfo.PBPrefix = "B";
                int savedNum = diskService.getBookStoredNum(artworkInfo);
                if((autoBookSkipPercent == 1 && (savedNum*1.0/postCount > bookSkipPercent)) // 常规模式，满足完整度
                || (savedNum*1.0/postCount >autoBookSkipPercent)
                ){
                    // 情况1  常规模式(auto = 1) 这时候 || 后面的条件 一定不满足，所以 是根据 savedNum*1.0/postCount > bookSkipPercent 进行判断
                    // 情况2  自动模式(auto !=1 )， 如果完整度高，跳过
                    //                             完整度不高，但是超过了 autoBook 要求的完整度，也跳过， autoBook 一般给定比较低的数值 比如 0.3
                    logger.info("作品完整度满足要求，跳过下载：" + bookId + "_" + bookName);
                } else {
                    // https://beta.sankakucomplex.com/books/ +  id + ?tags=order%3Apopularity%20 +  name
                    String bookUrl = CHAN_BOOK_PREFIX + bookId + "?tags=order%3Apopularity%20" + artistPathName;
                    logger.info("添加Url：" + bookUrl);
                    bookUrlList.add(bookUrl);
                }
            }
            // 处理返回数据的 meta 字段
            Map<String, Object> meta = (Map<String, Object>) res.get("meta");
            if (meta.containsKey("next") && !"null".equals(meta.get("next")) && !(null == meta.get("next"))) {
                logger.info("发现next信息，解析页面并加入下一页");
                page.addTargetRequest(CHAN_ARTIST_BOOK_LIST_PREFIX + "?lang=en&next=" + meta.get("next") + "&limit=20&tags=order:popularity+" + artistPathName + "&pool_type=0");
            }
        }
    }

    public static String CHAN_BOOK_PREFIX = "https://beta.sankakucomplex.com/books/";
    public String artistPathName;

    @Override
    public Site getSite() {
        return BookPageProcessor.site;
    }

    public static String CHAN_ARTIST_BOOK_LIST_PREFIX = "https://capi-v2.sankakucomplex.com/pools/keyset?";

    // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&limit=20&tags=order:popularity+roke&pool_type=0
    //   返回：
    // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&next=d83532336bd0fb4e3a262a5ce7d6888ba07f94b1d11e1318216f302b239f92ab&limit=20&tags=order:popularity+roke&pool_type=0
    public static void main(String[] args) {
//        List<String> aims = new ArrayList<>();
//        aims.add("yang-do");
//        aims.add("hews hack");
//        aims.add("kidmo (kimdonga)");
//        aims.add("cian yo");
//        aims.add("aoin");
//        aims.add("sayika");
//        aims.add("letdie");
//        aims.add("xxoom");
//        aims.add("jm");
//        aims.add("roke");
//        aims.add("yuzhou");
//        aims.add("tokinohimitsu");
//        aims.add("orico");
//        aims.add("mignon");
//        aims.add("badapple");
//        aims.add("as109");
//        aims.add("lao meng");
//        aims.add("ninra");
//        aims.add("ratatatat74");
//        aims.add("nyamota");
//        for (int i = 0; i < 3; i++) {
//            for (String name :
//                    aims) {
//                String prefix = "https://beta.sankakucomplex.com/wiki/en/";
//                String urlName = name.replaceAll(" ", "_");
//                String searchUrl = prefix + urlName;
//                DataBaseService dataBaseService = new DataBaseService();
//                DiskService diskService = new DiskService(SpiderSetting.buildSetting());
//                List<String> bookUrl = new ArrayList<>();
//                ArtistBookListProcessor processor = new ArtistBookListProcessor(bookUrl, false, dataBaseService, diskService);
//                Spider.create(processor).addUrl(searchUrl).run();
//
//                for (String url :
//                        bookUrl) {
//                    BookPageProcessor bookPageProcessor = new BookPageProcessor(name, false, dataBaseService, diskService);
//                    bookPageProcessor.flexSite = BookPageProcessor.site;
//                    Spider.create(bookPageProcessor).addUrl(url).thread(4).run();
////                bookPageProcessor.reset(name, false);
//                }
//            }
//        }
        System.out.println(0.00001 == 0.00001);
    }
}
