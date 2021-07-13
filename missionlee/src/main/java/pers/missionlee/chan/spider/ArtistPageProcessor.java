package pers.missionlee.chan.spider;

import com.alibaba.fastjson.JSON;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.spider.book.ArtistBookListProcessor;
import pers.missionlee.chan.spider.book.BookPageProcessor;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;

import java.util.*;

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
    boolean onlyTryTen;// 下载新作品的时候，只下载前十个，这要是 根据chrome书签下载有的作者作品其实一半
  boolean downloadAllTryBest = false;
  String saveName;
    public ArtistPageProcessor(boolean onlyTryTen, boolean autoNextPage, String artistName, DataBaseService dataBaseService, DiskService diskService,String saveName,SpiderSetting spiderSetting) {
        super(artistName, dataBaseService, diskService);
        this.spiderSetting = spiderSetting;
        this.onlyTryTen = onlyTryTen;
        this.autoNextPage = autoNextPage;
        this.artistName = artistName;
        this.downloaded = 0;
        this.saveName = saveName;
        this.storedSanCodes = initStoredSanCodes();
        this.storedFilesMd5 = initStoredFilesMd5();
    }

    public ArtistPageProcessor(boolean autoNextPage, String artistName, DataBaseService dataBaseService, DiskService diskService, SpiderSetting spiderSetting,String saveName) {
        super(artistName, dataBaseService, diskService);
        this.autoNextPage = autoNextPage;
        this.artistName = artistName;
        this.downloaded = 0;
        this.spiderSetting = spiderSetting;
        this.saveName = saveName;
        this.storedSanCodes = initStoredSanCodes();
        this.storedFilesMd5 = initStoredFilesMd5();

    }


    @Override
    public Set<String> initStoredSanCodes() {
        List<String> sanCodes = dataBaseService.getSanCodeByArtistName(artistName);
        HashSet<String> codes = new HashSet<>();
        codes.addAll(sanCodes);
        logger.info("现存[" + codes.size() + "]个相关SanCode" );
        return codes;
    }

    @Override
    public Map<String, String> initStoredFilesMd5() {
        return  spiderSetting.initAllRelatedStoredFilesMd5(diskService,this.saveName);
//        return diskService.getArtistFileMd5Path(this.saveName);
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
        if (url.contains("tags")) {
            if (autoNextPage) {
                // 开启auto next page 一半是更新，此时的 sleep 时间较短
                sleep(2);
            } else {
                // 没开启auto next page 可能是 强制下载全部文件，
                sleep(5);
            }
//            int added = extractUrlFromListPage(page);
            int added = extractUrlFromListPageWithFileNameFilter(page);
            if (added > 0 && autoNextPage) {
                if (onlyTryTen) {
                    logger.info("作品试下载功能启动（每个作者尝试下载10个作品）");
                    if (toBeDownloadSanCodes.size() < 5)
                        addNextPageAsTarget(page);
                } else {
                    addNextPageAsTarget(page);
                }
            }
        } else if (url.contains("/show/")) {
            String pageString = page.getHtml().toString();
            if (pageString.contains("这个帖子已经删除")
                    || pageString.contains("This post was deleted")
                    || pageString.contains("您没有查看该内容所需要的访问权限")
                    || pageString.contains("You lack the access rights required to view this content")
            ) {
                logger.info("文件被删除，或者没有访问权限，跳过这个作品："+page.getUrl().toString());
            } else {
                AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
                ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
                extractArtistFileByArtworkInfo(artworkInfo);
                artworkInfo.aimName = saveName;
                artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
                artworkInfo.fileSaveName = artworkInfo.fileName;
                boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
                artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
                artworkInfo.isSingle = false;
                if (download) {
                    dataBaseService.saveArtworkInfo(artworkInfo);
                    downloaded++;
                }
            }

        } else {

        }
    }

    public static void main(String[] args) {
String x = "{\"source\":\"Doujinshi &quot;sister&#65290;sisters&quot;\"}";
        Map<String,Object> xxx = (Map<String, Object>) JSON.parse(x);
        System.out.println(xxx.get("source"));

    }
}
