package pers.missionlee.webmagic.spider.newsankaku.source;

import org.apache.ibatis.session.SqlSession;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.BookParentInfo;

import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-25 23:28
 */
public class FakeSourceManager implements SourceManager{
    @Override
    public String getTempPath() {
        return null;
    }

    @Override
    public void saveArtworkInfo(ArtworkInfo info) {

    }

    @Override
    public void postProcess(TaskController controller) {

    }

    @Override
    public int getStoredNum(TaskController controller) {
        return 0;
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        return null;
    }

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return null;
    }

    @Override
    public SqlSession getSqlSession() {
        return null;
    }

    @Override
    public void clearSanCodes() {

    }

    @Override
    public void extractAllFileNames(Set<String> names) {

    }

    @Override
    public boolean fileNameExist(String file_name) {
        return false;
    }

    @Override
    public int saveBookInfo(BookParentInfo bookParentInfo) {
        return 0;
    }

    @Override
    public int sanCodeExist(String sanCodee) {
        return 0;
    }
}
