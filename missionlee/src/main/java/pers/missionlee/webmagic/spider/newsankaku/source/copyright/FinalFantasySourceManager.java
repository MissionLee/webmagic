package pers.missionlee.webmagic.spider.newsankaku.source.copyright;

import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-23 10:44
 */
public class FinalFantasySourceManager extends AbstractCopyRightAndCharacterSourceManager {
    public FinalFantasySourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }
    public static final String PATH_FINAL_FANTASY = "sankaku_final_fantasy_series";
    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return null;
    }
}
