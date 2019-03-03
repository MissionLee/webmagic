package pers.missionlee.webmagic.spider.sankaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Spider;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 12:19
 */
@Deprecated
public class SankakuSpiderProcessor {
    public static Logger logger = LoggerFactory.getLogger(SankakuSpiderProcessor.class);
    // 系统参数
    private static final String chan_sankakucomplex_com_IP = "208.100.27.32";
    private static final String cs_sankakucomplex_com_IP="208.100.24.254";
    public static final String ARTIST_INFO_FILENAME = "artist.json";
    public static final String ARTWORK_INFO_FILENAME = "artwork.json";
    public static final String ARTWORK_INFO_FILENAME_LINED = "artwork.jsonline";
    public static final String BASE_SITE = "https://chan.sankakucomplex.com/?tags=";
    public enum ORDER{
        date("%20order%3Adate&page=","DATE"),
        tag_count_dec("%20order%3Atagcount&page=","TAG_COUNT_DEC"),
        tag_count_asc("%20order%3Atagcount_asc&page=","TAG_COUNT_ASC");
        String key;
        String desc;
        public String getKey() {
            return key;
        }
        public String getDesc(){
            return desc;
        }
        ORDER(String key,String desc) {
            this.key = key;
            this.desc=desc;
        }
    }
    // 构造参数
    public static String PARENT_PATH = "D:/sankaku/";
    public static int THREAD_NUM = 3;
    public static String TAG="other";
    // 运行参数
    @Deprecated
    public static List<ArtworkInfo> artworkInfoList;

    /**
     * @Description:
     * @Param: [startPage, endPage]
     * @return: java.util.List<java.lang.Integer>
     * @Author: Mission Lee
     * @date: 2019/3/2
     */
    @Deprecated
    private static List<Integer> getAimPageList(int startPage,int endPage){

        if(startPage>endPage || startPage<1 || endPage <1){
            return new ArrayList<Integer>();
        }else{
            List<Integer> pageList = new ArrayList<Integer>();
            int end = endPage+1;
            for (int i = startPage; i < end; i++) {
                pageList.add(i);
            }
            return pageList;
        }
    }
    @Deprecated
    public static void run(String parentPath,String tag,int page){
        run(parentPath,tag,page,page,ORDER.date,THREAD_NUM);
    }

    @Deprecated
    public static void runBrandNewTag(String parentPath,String tag,int totalNum,int threadNum){
        Double x =Math.ceil(totalNum/20);
        System.out.println(x);
        int pageEnd = ((Double)x).intValue();
        System.out.println(pageEnd);
        if(pageEnd>50){
            run(parentPath,tag,1,50,ORDER.tag_count_asc,threadNum);
            run(parentPath,tag,1,(pageEnd-50),ORDER.tag_count_dec,threadNum);
        }else{
            run(parentPath,tag,1,pageEnd,ORDER.date,threadNum);
        }
    }
    /**
     * @Description: 用于自动配置下载一个作者的所有作品，再登录判定通过的情况下，可下载
     *              2000 上限，登录信息失效可以查找 1000上限
     *                  ----- 但是需要变动参数  -----
     *              会先生成所有入口url一起添加到 scheduler中
     *              // TODO: 2019/3/2 只在所有爬虫运行完毕之后才保存一次信息，
     *                  如果页面多运行中间发生错误程序崩溃或者锁死或者网络条件差，
     *                  程序中断恢复运行的成本较高，必须重新遍历所有页面才能重新
     *                  获取信息（不用重新下载文件）
     * @Param: [parentPath, tag, totalNum, threadNum]
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/3/2
     */    
    public static void runWithAllUrlAddedAtStart(String parentPath,String tag,int totalNum,int threadNum){

        init(parentPath, tag, threadNum);

        int pageNum = (
                (Double)(
                        Math.ceil((new Double(totalNum))/20)
                        )
                ).intValue();
        System.out.println("pageNUM:"+pageNum);
        String[] urls = new String[pageNum];
        String pagedPathPrefixBefore50 = BASE_SITE+tag.replace(" ","_").replace("(","%28").replace(")","%29")+ORDER.tag_count_asc.getKey();
        String pagedPathPrefixAfter50 = BASE_SITE+tag.replace(" ","_").replace("(","%28").replace(")","%29")+ORDER.tag_count_dec.getKey();


        for (int i = 0; i < pageNum; i++) {
            if((i+1)<=50){
                urls[i]=pagedPathPrefixBefore50+(i+1);
            }else{
                urls[i]=pagedPathPrefixAfter50+(i-49);
            }
        }
        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i]);
        }
        
        SankakuSpider sankakuSpider = new SankakuSpider();
        Spider spider = Spider.create(sankakuSpider);
        
        
        spider.addUrl(urls).thread(threadNum).run();
        // TODO: 2019/3/2 另外起一个线程，负责定时 saveInformation 
        // TODO: 2019/3/2 也可以在爬虫内同步代码块保存出局 
        saveInformation();
    }
    @Deprecated
    public static void run(String parentPath,String tag,int startPage,int endPage,ORDER order,int threadNum){

        List<Integer> pageList = getAimPageList(startPage,endPage);
        if(pageList.size()>0){
            // 配置参数
            init(parentPath, tag, threadNum);
            // 预处理 除了 page页面之前的所有部分
            String pagedPathPrefix = BASE_SITE+tag.replace(" ","_").replace("(","%28").replace(")","%29")+order.getKey();

            SankakuSpider sankakuSpider = new SankakuSpider();
            Spider spider = Spider.create(sankakuSpider);

            logger.info("START SPIDER :\n ⭐ TAG: "+TAG +"\n ⭐ ORDER: "+order.getDesc()+"\n ⭐ PAGE: "+startPage+"~"+endPage);
            // TODO: 2019/3/1 此处应该添加模拟登录，然后获取Cookie并设置
            for (Integer i :
                    pageList) {
                logger.info("START PAGE: "+i);
                spider.addUrl(pagedPathPrefix+i).thread(THREAD_NUM).run();
                saveInformation();
                logger.info("END PAGE: "+i);
            }

        }

    }
    @Deprecated
    private static void init(String parentPath, String tag, int threadNum) {
        PARENT_PATH = parentPath;
        TAG=tag;
        THREAD_NUM =threadNum;
        // 本地文件预检测/处理
        ArtworkInfoUtils.filePathPreHandleByTag(TAG);

        artworkInfoList = ArtworkInfoUtils.getArtworkInfoList(TAG);
        if (artworkInfoList == null) {
            artworkInfoList = new ArrayList<ArtworkInfo>();
        }
    }

    @Deprecated
    public static void saveInformation() {
        ArtworkInfoUtils.setArtworkInfo(SankakuSpiderProcessor.TAG, artworkInfoList);
        logger.info("saveInformation");
    }
    public static void main(String[] args) {
        runWithAllUrlAddedAtStart("D:/sankaku","letdie1414",109,3);


    }
}
