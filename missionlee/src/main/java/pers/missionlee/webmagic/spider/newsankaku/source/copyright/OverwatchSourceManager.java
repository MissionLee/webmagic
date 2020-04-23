package pers.missionlee.webmagic.spider.newsankaku.source.copyright;

import pers.missionlee.webmagic.spider.newsankaku.source.copyright.AbstractCopyRightAndCharacterSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-22 11:05
 */
public class OverwatchSourceManager extends AbstractCopyRightAndCharacterSourceManager {
    public OverwatchSourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }
    public static final String PATH_OVERWATCH_SERIES = "sankaku_overwatch_series";

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return getAimDicOfCopyRight(controller,info,PATH_OVERWATCH_SERIES);
    }
}
