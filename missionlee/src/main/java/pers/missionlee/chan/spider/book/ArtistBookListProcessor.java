package pers.missionlee.chan.spider.book;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.AbstractPageProcessor;
import pers.missionlee.chan.spider.AbstractTagPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    boolean skipWhileBookIdExits;
    boolean skipBookLostPage;

    public ArtistBookListProcessor(List<String> bookUrlList, boolean onlyNew, DataBaseService dataBaseService, DiskService diskService, double autoBookSkipPercent, double bookSkipPercent, boolean skipWhileBookIdExists, boolean skipBookLostPage, String realName) {
        super(dataBaseService, diskService);
        this.skipBookLostPage = skipBookLostPage;
        this.bookUrlList = bookUrlList;
        this.onlyNew = onlyNew;
        this.autoBookSkipPercent = autoBookSkipPercent;
        this.bookSkipPercent = bookSkipPercent;
        this.skipWhileBookIdExits = skipWhileBookIdExists;
        this.realName = realName;
    }

    public ArtistBookListProcessor(List<String> bookUrlList, boolean onlyNew, DataBaseService dataBaseService, DiskService diskService, double bookSkipPercent, boolean skipWhileBookIdExists, boolean skipBookLostPage, String realName) {

        super(dataBaseService, diskService);
        this.skipBookLostPage = skipBookLostPage;
        this.bookUrlList = bookUrlList;
        this.onlyNew = onlyNew;
        this.bookSkipPercent = bookSkipPercent;
        this.skipWhileBookIdExits = skipWhileBookIdExists;
        this.realName = realName;
    }

    String realName;


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
//                logger.info("列表页面：Book信息JSON   " + bookList.get(i));
                Map<String, Object> bookInfo = bookList.get(i);
                String bookName = (String) bookInfo.get("name");
                boolean stored = false;
                int storedNum = 0;
                int bookId = (int) bookInfo.get("id");
                int postCount = (int) bookInfo.get("visible_post_count");
                ArtworkInfo artworkInfo = new ArtworkInfo();
                artworkInfo.bookId = bookId;
                artworkInfo.bookName = bookName;
                artworkInfo.isSingle = false;
                artworkInfo.aimName = this.realName;
                artworkInfo.fileName = "1.jpg";
                artworkInfo.PBPrefix = "B";
//                if(skipWhileBookIdExits && dataBaseService.bookIdExists(bookId)){
//                    stored = true;
//                }
                // 判断这个 book 的存在情况
                // 1-存储作者，指的是 两个名字实际是同一个作者，检查要保存的位置有没有
                // 2-连接作者，两个作者实际是同一个，当前用其中一个作者明，从网站搜索到的这个作品，看看这个作者名有没有
                // 3-book的作者tag里面的作者，book tag里面可能有更多的作者，看看有没有存在更多的作者那里
                // ==== 1.是否在存储作者
                storedNum = diskService.getBookStoredNum(artworkInfo);
                if (storedNum > 0) {
                    stored = true;
                    logger.info("在当前作者 [目标保存作者]"+this.realName+"发现了作品");
                }
                // === 2.s是否在连接作者中存储
                if (!stored && !this.realName.equals(this.artistName)) {
                    artworkInfo.aimName = this.artistName;
                    storedNum = diskService.getBookStoredNum(artworkInfo);
                    if (storedNum > 0) {
                        logger.info("在当前作者 [列表查询作者]"+this.artistName+"发现了作品-");
                        stored = true;
                    }
                }
                // === 3 判断 artist——tags 里面的作者
                // TODO: 7/4/2021  通过 artist_tags 判断 book可能存放的位置，如果存在，那么不再下载
                if (!stored && bookInfo.containsKey("artist_tags")) { // 根据 artist_tags 信息 判断 book是否存在别的作者名下
                    List<Map<String, Object>> artists_tags = (List<Map<String, Object>>) bookInfo.get("artist_tags");
                    if (artists_tags.size() >= 3) {
                        logger.info("注意：因为一些特殊跨作者book的原因，一个作品的作者数量大于等于 3 的时候，放弃这个作品");
                        stored = true;
                    } else if (null != artists_tags && artists_tags.size() > 0)
                        // 遍历 artist_tags（一个json） ,找到   key =name 取到name
                        for (int j = 0; j < artists_tags.size(); j++) {
                            Map<String, Object> art = artists_tags.get(j);
                            if (art.containsKey("name")) {
                                String name = art.get("name").toString();
                                if (!artistName.equals(name) && this.realName.equals(name)) {
                                    // 把作品作者 改为 这个不一样的作者，如果在这个作者名下 book的作品数量大于 0 就不在当前作者下载
                                    artworkInfo.aimName = name;
                                    storedNum = diskService.getBookStoredNum(artworkInfo);
                                    if (storedNum > 0) {
                                        logger.info("在 [作品的作者列表中]"+name+"发现了作品");
                                        stored = true;
                                    }
//                                    if (diskService.getBookStoredNum(artworkInfo) > 0) {
//                                        stored = true;
//                                        logger.info("作品 " + bookName + " 保存在：" + name + " 本作者不再下载");
//                                        continue;
//                                    }
//                                    artworkInfo.aimName = this.artistName;
                                }
                            }
                        }
                }
//                String saveArtist = dataBaseService.getBookStoredArtistById(bookId);
//                if(dataBaseService.containsBoook(bookId)){
//
//                }
// TODO: 7/4/2021 额外查询，是不是放在其他位置，这个位置可能是 因为一些手动的改名操作 变化了
                if(!stored){
                    String storedArtist = dataBaseService.getBookStoredArtistById(bookId);
                    artworkInfo.aimName = storedArtist;
                    if(StringUtils.isEmpty(storedArtist)){

                    }else{
                        storedNum = diskService.getBookStoredNum(artworkInfo);
                        if (storedNum > 0) {
                            logger.info("在 [作品数据库中保存作者]"+storedArtist+"发现了作品");
                            stored = true;
                        }
                    }

                }
                String bookUrl = CHAN_BOOK_PREFIX + bookId + "?tags=order%3Apopularity%20" + artistPathName;
                if(stored){
                    // 已经保存了，根据保存数据和配置要求 判断是否下载
                    boolean skip = false;
                    if(storedNum == postCount){ // 保存完整，跳过下载
                        skip = true;
                        logger.info("作品完整度满[100%]跳过下载：" + bookId + "_" + bookName);

                    }
                    if(skipBookLostPage && storedNum >0){// 已经保存部分，缺页不下载
                        logger.info("skipBookLostPage模式下，缺页作品跳过下载：" + bookId + "_" + bookName);
                        skip = true;
                    }
                    if((storedNum*1.0/postCount)>autoBookSkipPercent){// 保存完整度 满足要求不下载
                        logger.info("作品完整度满[满足跳过条件]跳过下载：" + bookId + "_" + bookName);

                    }
                    if(skip){

                    }else{
                        logger.info("添加Url：" + bookUrl);
                        bookUrlList.add(bookUrl);
                    }
                }else{
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


}
