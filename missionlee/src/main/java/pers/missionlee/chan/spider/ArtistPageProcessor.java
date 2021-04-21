package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.book.ArtistBookListProcessor;
import pers.missionlee.chan.spider.book.BookPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-14 22:17
 */
public class ArtistPageProcessor extends AbstractTagPageProcessor {
    boolean autoNextPage;
    String artistName;
    public int downloaded;
    SpiderSetting spiderSetting;
    public ArtistPageProcessor(boolean autoNextPage, String artistName, DataBaseService dataBaseService, DiskService diskService) {
        super(artistName, dataBaseService, diskService);
        this.autoNextPage = autoNextPage;
        this.artistName = artistName;
        this.storedSanCodes = initStoredSanCodes();
        this.downloaded = 0;
    }
    public ArtistPageProcessor(boolean autoNextPage, String artistName, DataBaseService dataBaseService, DiskService diskService,SpiderSetting spiderSetting) {
        super(artistName, dataBaseService, diskService);
        this.autoNextPage = autoNextPage;
        this.artistName = artistName;
        this.storedSanCodes = initStoredSanCodes();
        this.downloaded = 0;
        this.spiderSetting = spiderSetting;
    }

    @Override
    public Set<String> initStoredSanCodes() {
        List<String> sanCodes = dataBaseService.getSanCodeByArtistName(artistName);
        HashSet<String> codes = new HashSet<>();
        codes.addAll(sanCodes);
        logger.info("现存["+codes.size()+"]个相关SanCode："+sanCodes);
        return codes;
    }

    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void doProcess(Page page) {
        String url = page.getUrl().toString();
        if(url.contains("tags")){
            int added = extractUrlFromListPage(page);
            if(added>0 && autoNextPage) addNextPageAsTarget(page);
        }else if(url.contains("/show/")){

            AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
            ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
            artworkInfo.aimName = artistName;
            artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
            artworkInfo.fileSaveName = artworkInfo.fileName;
            boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
            artworkInfo.isSingle = false;
            if (download) {
                dataBaseService.saveArtworkInfo(artworkInfo);
                downloaded++;
            }
            // TODO: 4/20/2021  这里再 spider 里面 创建 spider 感觉 thread 会有问题 所以这个代码 用 false && 屏蔽了
            if(false && spiderSetting.autoBook
                    && SinglePageClassifyPageProcessor.belongsToBook(page.getHtml().toString())
                    &&diskService.getParentPath(artworkInfo,"B",artworkInfo.storePlace).
                    contains(spiderSetting.getBookParentArtistBase().substring(spiderSetting.getBookParentArtistBase().lastIndexOf("/")))
            ){ // 1. 启动了 autobook, 2.当前作品属于book，3.当前作者在 book目录中
                String prefix = "https://beta.sankakucomplex.com/wiki/en/";
                String urlName = artistName.replaceAll(" ", "_");
                String searchUrl = prefix + urlName;
                List<String> bookUrl = new ArrayList<>();
                ArtistBookListProcessor processor = new ArtistBookListProcessor(bookUrl, false, dataBaseService, diskService,spiderSetting.autoBookSkipPercent,spiderSetting.bookSkipPercent);
                Spider.create(processor).addUrl(searchUrl).run();

                for (String iUrl :
                        bookUrl) {
                    BookPageProcessor bookPageProcessor = new BookPageProcessor(artistName, false, dataBaseService, diskService);
                    bookPageProcessor.flexSite = BookPageProcessor.site;
                    Spider.create(bookPageProcessor).addUrl(iUrl).thread(spiderSetting.threadNum).run();
                }
            }
        }else{

        }
    }

    public static void main(String[] args) {
    }
}
