package pers.missionlee.chan.spider;

import com.alibaba.fastjson.JSON;
import pers.missionlee.chan.pojo.ArtistPathInfo;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;

import java.io.IOException;
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
    ArtistPathInfo artistPathInfo;

    public ArtistPageProcessor(boolean onlyTryTen, boolean autoNextPage, String artistName, DataBaseService dataBaseService, DiskService diskService, String saveName, SpiderSetting spiderSetting) {
        super(artistName, dataBaseService, diskService);
        this.spiderSetting = spiderSetting;
        this.onlyTryTen = onlyTryTen;
        this.autoNextPage = autoNextPage;
        this.artistName = artistName;
        this.downloaded = 0;
        this.saveName = saveName;
        this.storedSanCodes = initStoredSanCodes();
        this.storedFilesMd5 = initStoredFilesMd5();
        this.initSettingInfo(diskService.getParentPath(ArtworkInfo.getArtistPicPathInfo(artistName), "", ArtworkInfo.STORE_PLACE.ARTIST.storePlace));

    }
    @Deprecated
    public ArtistPageProcessor(boolean autoNextPage, String artistName, DataBaseService dataBaseService, DiskService diskService, SpiderSetting spiderSetting, String saveName) {
        super(artistName, dataBaseService, diskService);
        this.autoNextPage = autoNextPage;
        this.artistName = artistName;
        this.downloaded = 0;
        this.spiderSetting = spiderSetting;
        this.saveName = saveName;
        this.storedSanCodes = initStoredSanCodes();
        this.storedFilesMd5 = initStoredFilesMd5();
        this.initSettingInfo(diskService.getParentPath(ArtworkInfo.getArtistPicPathInfo(artistName), "", ArtworkInfo.STORE_PLACE.ARTIST.storePlace));

    }

    public void initSettingInfo(String artistFilePath) {
        try {
            logger.info("清理作者作品路径");
            // 处理过期页面，目前是大小为 12.6K的文件
            this.artistPathInfo = ArtistPathInfo.refreshInfo(artistFilePath);
            this.delFileName = this.artistPathInfo.delFileMD5;
            logger.info("处理已经被归为Parent Book但是仍有单个文件的清空");
            diskService.cleanArtistBookParentBases(artistName);
//            diskService.renameBookFolderName(artistName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> initStoredSanCodes() {
        List<String> sanCodes = dataBaseService.getSanCodeByArtistName(artistName);
        HashSet<String> codes = new HashSet<>();
        codes.addAll(sanCodes);
        logger.info("现存[" + codes.size() + "]个相关SanCode");
        return codes;
    }

    @Override
    public Map<String, String> initStoredFilesMd5() {
        return spiderSetting.initAllRelatedStoredFilesMd5(diskService, this.saveName);
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
                sleep(8);
            }
//            int added = extractUrlFromListPage(page);
            int added = extractUrlFromListPageWithFileNameFilter(page);
            logger.info("当前页面有 ["+added+"] 个页面加入 /自动下一页["+autoNextPage+"]/扫荡模式["+spiderSetting.downloadAllTryBest+"]");
            if ((added > 0 && autoNextPage) || (spiderSetting.downloadAllTryBest)) {
                logger.info("将下一页加入队列");
                if (onlyTryTen) {
                    logger.info("因为 onlyTryTen 功能 不再添加下一页");
//                    if (toBeDownloadSanCodes.size() < 5){
//                        logger.info("因为 onlyTry功能，但是当前只采集不足5个页面，");
//
//                        addNextPageAsTarget(page);
//                    }else{
//                        logger.info("因为 onlyTry功能 不在添加下一页");
//                    }

                } else {
                    addNextPageAsTarget(page);
                }
            }else{
                logger.info("不再加载下一页");
            }
        } else if (url.contains("/show/") || url.contains("/posts/")) {
            String pageString = page.getHtml().toString();
            if (pageString.contains("这个帖子已经删除")
                    || pageString.contains("This post was deleted")
                    || pageString.contains("您没有查看该内容所需要的访问权限")
                    || pageString.contains("You lack the access rights required to view this content")
            ) {
                logger.info("因[文件已删除||无访问权限]跳过 " + page.getUrl().toString());
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
                    logger.info("[" + downloaded + "/" + toBeDownloadSanCodes.size() + "]");
                }
            }

        } else {

        }
    }

    public static void main(String[] args) {
        String x = "{\"source\":\"Doujinshi &quot;sister&#65290;sisters&quot;\"}";
        Map<String, Object> xxx = (Map<String, Object>) JSON.parse(x);
        System.out.println(xxx.get("source"));

    }
}
