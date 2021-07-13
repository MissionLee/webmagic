package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;

import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-15 19:17
 */
public class CopyrightPageProcessor extends AbstractTagPageProcessor {
//    List<String> storedFileNames;
    String copyright;
    boolean autoNextPage;

    public CopyrightPageProcessor(boolean autoNextPage, List<String> tags, DataBaseService dataBaseService, DiskService diskService) {
        super(tags, dataBaseService, diskService);
        this.copyright = tags.get(0);
        this.autoNextPage = autoNextPage;
        init();
    }

    public void init() {
//        this.storedFileNames = diskService.getCopyRightFileNames(copyright);
        this.storedFilesMd5 = diskService.getCopyRightFileMd5(copyright);
//        this.storedSanCodes = new HashSet<>();
    }

    @Override
    public Set<String> initStoredSanCodes() {
        return new HashSet<>();
    }

    @Override
    public Map<String, String> initStoredFilesMd5() {
        return null;
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
//            String pageNum = url.substring(url.lastIndexOf("=")+1);
//            int pageN = Integer.valueOf(pageNum);
            if (autoNextPage) {
                sleep(2);
            } else {
                sleep(5);
            }
            int added = extractUrlFromListPageWithFileNameFilter(page);
            if ( added>0 && autoNextPage ) {
                addNextPageAsTarget(page);
            }
        } else if (url.contains("/show/")) {
            String pageString = page.getHtml().toString();
            if (pageString.contains("这个帖子已经删除")
                    || pageString.contains("This post was deleted")
                    || pageString.contains("您没有查看该内容所需要的访问权限")
                    || pageString.contains("You lack the access rights required to view this content")
            ) {
                logger.info("文件被删除，或者没有访问权限，跳过这个作品：" + page.getUrl().toString());
            } else {
                AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
                ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
//                extractArtistFileByArtworkInfo(artworkInfo);
                // // TODO: 6/14/2021  隐藏了上面这一行，因为我们希望，即使存在别的作者哪里，也复制一份过来 
                artworkInfo.aimName = copyright;
                artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.COPYRIGHT.storePlace;
                artworkInfo.fileSaveName = artworkInfo.fileName;
                boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
                artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.ARTIST.artistType;
                artworkInfo.isSingle = false;
                if (download) {
                    dataBaseService.saveArtworkInfo(artworkInfo);
                }

            }
        }
    }
}
