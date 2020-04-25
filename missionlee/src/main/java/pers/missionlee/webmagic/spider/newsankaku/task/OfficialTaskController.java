package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.source.ArtistSourceManager;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-14 10:54
 */
public class OfficialTaskController extends AbstractTaskController{
    public OfficialTaskController(ArtistSourceManager artistSourceManager) {
        super(artistSourceManager);
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
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo, boolean infoOnly,boolean storeOnly) {
        return false;
    }

    @Override
    public String getNumberCheckUrl() {
        return null;
    }

    @Override
    public Boolean existOnDisk(ArtworkInfo artworkInfo) {
        return null;
    }


}
