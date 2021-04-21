package pers.missionlee.webmagic.spider.newsankaku.task;

import com.alibaba.fastjson.JSON;
import pers.missionlee.webmagic.spider.newsankaku.source.FakeSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceService;
import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.bookparentsingle.BookParentSingleSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.spider.book.BookSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.book.SingleSpider;
import pers.missionlee.webmagic.spider.newsankaku.spider.singlepageclassify.SinglePageClassifySpider;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.BookParentInfo;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-21 17:15
 */
public class BookTaskController extends AbstractTaskController{
    public BookParentInfo bookParentInfo = new BookParentInfo();

    public Map<String,String> sanCodeSequence = new HashMap<>();
    public Map<String,String> filenameSequence = new HashMap<>();

    public ArtistTaskController artistController;
    public BookTaskController(SourceManager sourceManager, String root, String... roots){
        super(sourceManager);
        this.artistController = new ArtistTaskController(new ArtistSourceManager(root,roots),"no-artist");
        System.out.println("注意：startString 最好是完整url，如果是 sanCode，会被认为是 single页面的sancode 而非 book对应的sanCode");
//        if(startString.contains("book")){
//            type = TYPE_BOOK;
//        }

    }

    public BookTaskController(SourceManager artistSourceManager) {
        super(artistSourceManager);
    }

    @Override
    public Boolean existOnDisk(ArtworkInfo artworkInfo) {
        String parentPath = sourceManager.getAimDic(this,artworkInfo);
        String[] names = new File(parentPath).list();
        System.out.println(names);
        String aim = artworkInfo.getFileName();
        for (int i = 0; i < names.length; i++) {
            if(names[i].endsWith(aim)) return true;
        }
        return false;
    }

    public void reset(){
        this.bookParentInfo = new BookParentInfo();
        this.sanCodeSequence.clear();
        this.filenameSequence.clear();
    }





    public void saveBookInfo(){
        System.out.println("saveBookInfo");
        System.out.println(JSON.toJSONString(bookParentInfo).length());
        sourceManager.saveBookInfo(bookParentInfo);
    }
    @Override
    public String[] getStartUrls() {
        return new String[0];
    }

    @Override
    public boolean confirmRel(String fullUrl) {
        return false;
    }

    @Override
    public String getNumberCheckUrl() {
        return null;
    }

    public static void main(String[] args) throws IOException {
        SourceService sourceService = new SourceService();
        ChromeBookmarksReader reader = new ChromeBookmarksReader("C:\\Documents and Settings\\MissionLee\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Bookmarks");
        List<Map> urls = reader.getBookMarkListByDirName("test");
        List<String> singleList = new ArrayList<>();
        for (Map bookmark   :urls
        ) {
            String url = bookmark.get("url").toString();
            if(url.contains("/show/") && url.contains("sankakucomplex")){
                singleList.add(url);
            }
        }
        HashSet<String> singleUrls = new HashSet<>();
        HashSet<String> parentUrls = new HashSet<>();
        HashSet<String> bookUrls = new HashSet<>();

        for (int i = 0; i < singleList.size(); i++) {
            String url = singleList.get(i);
            String sanCode = url.substring(url.lastIndexOf("/")+1);
            System.out.println(url +"_"+sanCode);
            // 查询有没有
            if(false && sourceService.sanCodeExist(sanCode) == 1){

            }else{
                Spider.create(
                        new SinglePageClassifySpider(new FakeTaskController(new FakeSourceManager()),parentUrls,singleUrls,bookUrls)
                        ).addUrl(url).thread(1).run();
            }
        }
        BookTaskController taskController = new BookTaskController(new BookParentSingleSourceManager("F://ROOT"),"F://ROOT","F://ROOT");
        System.out.println("Single :"+singleUrls);
        SingleSpider singleSpider = new SingleSpider(taskController);
        System.out.println("Book :"+bookUrls);
        BookSpider bookSpider = new BookSpider(taskController);
        System.out.println("Parent :"+parentUrls);

        for (int i = 0; i < singleUrls.size(); i++) {
            // 下载 真 single 页面

        }
        for (int i = 0; i < bookUrls.size(); i++) {
            // 下载book
        }
        for (int i = 0; i < parentUrls.size(); i++) {
            // 下载parent
        }
    }
}
