package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.BookParentInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-27 18:23
 */
public class ParentTaskController extends AbstractTaskController{
    public BookParentInfo bookParentInfo = new BookParentInfo();
    public ArtistTaskController artistController;
    // 因为要搜集作者信息，ParentSpider 是从母作品 开始的，搜集完母作品信息之后，下载 列表页，然后下载具体页面
    // 但是具体下载的时候，还会访问一次 母作品页面，所以用下面这个标志位
    public boolean parentPageDownloaded = false;
    public Map<String,String> codeNamePairs = new HashMap<>();

    public ParentTaskController(SourceManager artistSourceManager,String root,String... roots) {
        super(artistSourceManager);
        this.artistController = new ArtistTaskController(new ArtistSourceManager(root,roots),"no-artist");

    }
    public void reset(){
        this.bookParentInfo = new BookParentInfo();
        this.parentPageDownloaded = false;
        this.codeNamePairs.clear();
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
}
