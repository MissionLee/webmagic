package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-25 23:10
 */
public class FakeTaskController
        extends AbstractTaskController {


    public FakeTaskController(SourceManager artistSourceManager) {
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
    public String getNumberCheckUrl() {
        return null;
    }


}
