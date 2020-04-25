package pers.missionlee.webmagic.spider.newsankaku.source.copyright;

import pers.missionlee.webmagic.spider.newsankaku.source.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.copyright.AbstractCopyRightAndCharacterSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-15 14:00
 */
public class DOASourceManager extends AbstractCopyRightAndCharacterSourceManager {
    public DOASourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }

    @Override
    public void moveDownloadedFromArtist(ArtistSourceManager artistSourceManager) {

    }

    /**
     * ROOT/sankaku_doa_series/
     * - pic 与vid策略相同
     * - vid
     * - kasumi (dead or alive)      角色名 单个角色的作品
     * - z_unknown 没有标注角色的作品
     * - z_two_girl
     * - z_three_girl
     * - z_multiple_girl
     */
    public static final String PATH_DOA_SERIES = "sankaku_doa_series";

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return getAimDicOfCopyRight(controller,info,PATH_DOA_SERIES);

    }
}
