package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;

import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-15 19:17
 */
public class StudioPageProcessor extends AbstractTagPageProcessor {
    public StudioPageProcessor(List<String> tags, DataBaseService dataBaseService, DiskService diskService) {
        super(tags, dataBaseService, diskService);
    }

    @Override
    public Set<String> initStoredSanCodes() {
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

    }
}
