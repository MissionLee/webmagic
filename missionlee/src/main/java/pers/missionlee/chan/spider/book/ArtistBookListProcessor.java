package pers.missionlee.chan.spider.book;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.pojo.ArtistPathInfo;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.AbstractPageProcessor;
import pers.missionlee.chan.spider.AbstractTagPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-15 19:34
 */
public class ArtistBookListProcessor extends AbstractPageProcessor {
    // 作者信息页首页
    // https://beta.sankakucomplex.com/tag?tagName=as109
    // 作者首页-加载作者示例图片
    // https://capi-v2.sankakucomplex.com/posts/keyset?lang=en&default_threshold=2&limit=40&tags=order:popularity+as109
    // 作者首页-加载作者示例book
    // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&limit=20&includes[]=series&tags=as109

    // 作者book页面
    // https://beta.sankakucomplex.com/books?tags=order%3Apopularity%20as109
    // book页面-推荐作品
    // https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=10&tags=order:popularity++as109&page=1&includes[]=pools&exceptStatuses[]=deleted
    // book页面-懒加载-第一页作品
    // https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=40&tags=+order:date+order:popularity+as109&page=1&includes[]=pools&exceptStatuses[]=deleted
    // book页面-懒加载-第二页作品
    // https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=40&tags=+order:date+order:popularity+as109&page=2&includes[]=pools&exceptStatuses[]=deleted
    // book 明细接口
    // https://capi-v2.sankakucomplex.com/pools/173714?lang=en&exceptStatuses[]=deleted
    public static String CHAN_BOOK_PREFIX = "https://beta.sankakucomplex.com/books/";
    public static String CHAN_ARTIST_BOOK_LIST_PREFIX = "https://capi-v2.sankakucomplex.com/poolseriesv2";// 底层接口
    public static String CHAN_WIKI_PREFIX = "https://beta.sankakucomplex.com/tag?tagName="; // 搜索作者页面，会同时展示出普通作品 以及  Book&Series
    public static String getPoolSeriesUrl(String artist,int pageNum){
        artist=artist.replaceAll(" ","_");
        return "https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=40&tags=+order:date+order:popularity+"+artist+"&page="+pageNum+"&includes[]=pools&exceptStatuses[]=deleted";
    }
    public static String getPoolDetailUrl(int bookId){
        return "https://capi-v2.sankakucomplex.com/pools/"+bookId+"?lang=en&exceptStatuses[]=deleted";
    }
    static Logger logger = LoggerFactory.getLogger(ArtistBookListProcessor.class);
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
        this.initSettingInfo(diskService.getParentPath(
                ArtworkInfo.getArtistPicPathInfo(realName),"",ArtworkInfo.STORE_PLACE.ARTIST.storePlace
        ));
    }
    public ArtistBookListProcessor(List<String> bookUrlList, boolean onlyNew, DataBaseService dataBaseService, DiskService diskService, double bookSkipPercent, boolean skipWhileBookIdExists, boolean skipBookLostPage, String realName) {

        super(dataBaseService, diskService);
        this.skipBookLostPage = skipBookLostPage;
        this.bookUrlList = bookUrlList;
        this.onlyNew = onlyNew;
        this.bookSkipPercent = bookSkipPercent;
        this.skipWhileBookIdExits = skipWhileBookIdExists;
        this.realName = realName;
        this.initSettingInfo(diskService.getParentPath(
                ArtworkInfo.getArtistPicPathInfo(realName),"",ArtworkInfo.STORE_PLACE.ARTIST.storePlace
        ));
    }
    public ArtistPathInfo artistPathInfo;
    public void initSettingInfo(String artistFilePaht){
        try {
            this.artistPathInfo = ArtistPathInfo.refreshInfo(artistFilePaht);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
//        System.out.println(page.getHtml());
        String url = page.getUrl().toString();

        if (url.startsWith(CHAN_WIKI_PREFIX)) {
            /**
             * 从作者信息页面，获取 作者的 Tagged Book
             * */
            logger.info("*检测到作者信息查询页面，解析Tagged Book");
            String artistName = url.substring(url.lastIndexOf("=") + 1);
            artistPathName = artistName;
            this.artistName = artistName.replaceAll("_", " ");
            logger.info("检测到Wiki页面，即将爬取作品列表页面:" + this.artistName);
            // https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=40&tags=+order:date+order:popularity+as109&page=1&includes[]=pools&exceptStatuses[]=deleted
            String poolUrl = getPoolSeriesUrl(artistName,1);
            page.addTargetRequest(poolUrl);
            // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&limit=20&includes[]=series&tags=vycma
        } else if (url.startsWith(CHAN_ARTIST_BOOK_LIST_PREFIX)) {
            logger.info("*PoolSeries接口返回");
            Map<String, Object> res = getJsonStringFromRestPage(page);
            // 处理返回数据的  data 字段  data字段就是一个 放着 book信息的 list
            logger.info("解析返回数据中的 pools->data->[item]");
            List<Map<String, Object>> poolData =  ( List<Map<String, Object>>)((Map<String, Object>)res.get("pools")).get("data");
            for (int i = 0; i < poolData.size(); i++) {
                // 已经保存了，根据保存数据和配置要求 判断是否下载
                // TODO: 2023/1/8 提示用todo  skip 是最终跳过这个作品的判断条件
                boolean skip = false;
                boolean stored = false;
                boolean markSkip = false;
                Map<String, Object> bookInfo = poolData.get(i);
                /**
                 * 解析基本信息
                 * */
                int storedNum = 0;
                String bookName = (String) bookInfo.get("name"); // pool 名称
                int bookId = (int) bookInfo.get("id"); // book id
                int postCount = (int) bookInfo.get("visible_post_count");// pool内作品数量
                ArtworkInfo artworkInfo = new ArtworkInfo();
                artworkInfo.bookId = bookId;
                artworkInfo.bookName = bookName;
                artworkInfo.isSingle = false;
                artworkInfo.aimName = this.realName;
                artworkInfo.fileName = "1.jpg";
                artworkInfo.PBPrefix = "B";

                // 判断这个 book 的存在情况
                // todo:  2023-12-22 新增   0-如果删除列表里面有这个，则跳过
                // 1-存储作者，指的是 两个名字实际是同一个作者，检查要保存的位置有没有
                // 2-连接作者，两个作者实际是同一个，当前用其中一个作者明，从网站搜索到的这个作品，看看这个作者名有没有
                // 3-book的作者tag里面的作者，book tag里面可能有更多的作者，看看有没有存在更多的作者那里
                // ==== 1.是否在存储作者
//                System.out.println("不再下载的pool");
//                System.out.println(artistPathInfo.delPool);
//                System.out.println("当前book id");
//                System.out.println(bookId);
                if(artistPathInfo.delPool.contains(String.valueOf(bookId))){
                    logger.info("s.json文件夹中记录到删除了这个book 跳过这个book");
                    stored = true;
                    markSkip = true;
                }
                if(skipWhileBookIdExits && dataBaseService.bookIdExists(bookId)){
                    logger.info("数据库已经记录这个book id，非全部更新的模式下，直接跳过这个book");
                    stored = true;
                    markSkip = true;

                }
                if(bookId == 1857 || bookId ==8560){
                    logger.info("现在特定标记跳过 1857 是个copyright的book，由很多作者，但是判断的时候找不到作者");
                    stored = true;
                    markSkip = true;
                }
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
                    // TODO: 2023/1/8  20230108 发现下载了book但是不属于这个作者，所以此处增加遍历作者清单
                    boolean findThisArtist = false;
                    for (int j = 0; j < artists_tags.size(); j++) {
                        Map<String,Object> art = artists_tags.get(j);
                        if(art.containsKey("name")){
                            String name = art.get("name").toString();
                            if(name.equals(this.realName))
                                findThisArtist=true;
                        }
                    }
                    if(!findThisArtist){
                        // TODO: 2023/1/8 判断作品列表中有没有当前作者，如果没有直接跳过，之前好像也有book作者异常多，获取不到作者列表的情况，这里一并处理了
                        logger.info("未在当前Pool信息中找到目标作者的名称，所以不下载这个book");
                        skip =true;
                    }
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
                                        logger.info("在Pool的作者之一 ["+name+"]发现了作品,跳过下载");
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
                            logger.info("在数据库中查询到["+storedArtist+"]关联存储了这个Pool，跳过下载");
                            stored = true;
                        }
                    }

                }
                String bookUrl = getPoolDetailUrl(bookId);
                if(stored){

                    if(markSkip){
                        skip = true;
                        logger.info("作品=特定标记/或非完整模式=为跳过：" + bookId + "_" + bookName);

                    }
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
            Map<String, Object> meta = (Map<String, Object>)((Map<String, Object>)res.get("pools")).get("mete");
            if (meta.containsKey("next") && !"null".equals(meta.get("next")) && !(null == meta.get("next"))) {
                // LMS 20240316  next属性不参与接口访问，但是参与判断是否还有下一页
                logger.info("发现next信息，解析页面并加入下一页");
                // LMS 20240316 网站接口变更，下面这个不生效了
                //  page.addTargetRequest(CHAN_ARTIST_BOOK_LIST_PREFIX + "?lang=en&next=" + meta.get("next") + "&limit=20&tags=order:popularity+" + artistPathName + "&pool_type=0");

                // https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=40&tags=+order:date+order:popularity+as109&page=2&includes[]=pools&exceptStatuses[]=deleted
                page.addTargetRequest(getPoolSeriesNextPage(url));
            }
        }
    }
    public String artistPathName;

    @Override
    public Site getSite() {
        return BookPageProcessor.site;
    }
    // https://beta.sankakucomplex.com/books?tags=order%3Apopularity%20vycma

    // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&limit=20&includes[]=series&tags=vycma

    // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&limit=20&tags=order:popularity+roke&pool_type=0
    //   返回：
    // https://capi-v2.sankakucomplex.com/pools/keyset?lang=en&next=d83532336bd0fb4e3a262a5ce7d6888ba07f94b1d11e1318216f302b239f92ab&limit=20&tags=order:popularity+roke&pool_type=0
    public static String getPoolSeriesNextPage(String url){
        int pageEQNum = url.indexOf("page=");
        String thisPageNum = url.substring(pageEQNum+5,pageEQNum+6);
        int thisPageNumInt = Integer.valueOf(thisPageNum);
        int nextPageNumInt = thisPageNumInt+1;
        return url.replace("page="+thisPageNumInt,"page="+nextPageNumInt);
    }
    public static void main(String[] args) {
        String s = " https://capi-v2.sankakucomplex.com/poolseriesv2?lang=en&filledPools=true&offset=0&limit=40&tags=+order:date+order:popularity+as109&page=2&includes[]=pools&exceptStatuses[]=deleted";
        int n1 = s.indexOf("page=");
        System.out.println(n1);
        String num = s.substring(n1+5,n1+6);
        System.out.println(num);
    }
}
