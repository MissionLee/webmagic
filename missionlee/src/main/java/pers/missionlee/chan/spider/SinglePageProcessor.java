package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-11 10:17
 */
public class SinglePageProcessor extends AbstractPageProcessor {
    public SinglePageProcessor(DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
    }

    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void doProcess(Page page) {
        AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.SINGLE.storePlace;
        List<String> artists = artworkInfo.getTagArtist();
        boolean needdownload = true;
        for (int i = 0; i < artists.size(); i++) {
            // 如果
            if(dataBaseService.checkArtistIsTarget(artists.get(i))){
                needdownload = false;
            }
        }
        if(needdownload){
            boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
            artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.UNKNOWN.artistType;
            artworkInfo.isSingle = true;
            if (download) {
                dataBaseService.saveArtworkInfo(artworkInfo);
            }
        }else{
            logger.info("这个作品属于某个已经收藏的作者，所以跳过");
        }

    }

    public static void main(String[] args) {
        Spider.create(new SinglePageProcessor(new DataBaseService(),new DiskService(SpiderSetting.buildSetting()))).addUrl("https://chan.sankakucomplex.com/post/show/7080991").thread(1).run();
    }
}
