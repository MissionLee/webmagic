package pers.missionlee.webmagic.spider.newsankaku.source;

import org.apache.ibatis.session.SqlSession;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.BookParentInfo;

import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-15 11:00
 */
public interface SourceManager {
    // 获取临时文件路径
    public String getTempPath();
    // 保存作品信息
    public void saveArtworkInfo(ArtworkInfo info);
    // 爬虫运行完成后续处理
    public void postProcess(TaskController controller);
    // 获取当前爬虫爬取条件下，已经存储的目标数量
    public int getStoredNum(TaskController controller);
    // 获取当前爬虫爬取条件下，已经存储的目标集
    public Set<String> getStoredSanCode(TaskController controller);
    // 获取缓存的文件，应该放置到的位置
    public String getAimDic(TaskController controller,ArtworkInfo info);

    public SqlSession getSqlSession();

    public void clearSanCodes();
    // 将所有物理文件的名称，写入指定的 Set中
    public void extractAllFileNames(Set<String> names);

    public boolean fileNameExist(String file_name);

    // 保存
    public int saveBookInfo(BookParentInfo bookParentInfo);

    public int sanCodeExist(String sanCodee);
}
