package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-14 10:54
 */
public class OfficialTaskController extends AbstractTaskController{
    public OfficialTaskController(NewSourceManager newSourceManager) {
        super(newSourceManager);
    }

    @Override
    public String[] getStartUrls() {
        return new String[0];
    }

    @Override
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo, boolean infoOnly) {
        return false;
    }

    @Override
    public Boolean existOnDisk(String filename) {
        return null;
    }
}
