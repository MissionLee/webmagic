package pers.missionlee.webmagic.spider.newsankaku.source;

import org.apache.ibatis.session.SqlSession;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.BookParentInfo;

import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-15 11:02
 */
public abstract class AbstractSourceManager  implements SourceManager{
    /**
     * baseRoot：基础目录（权限目标的作品存储默认位置）
     * roots:分盘存储目录
     */
    public String baseRoot;
    public String[] addRoots;
    /**
     * 初始化时候已经存储的 sanCodes
     * */
    public Set<String> sanCodes;
    /**
     * 数据库服务
     * */
    public SourceService sourceService = new SourceService();
    public AbstractSourceManager(String baseRoot,String... roots) {
        this.baseRoot = PathUtils.formatPath(baseRoot);
        this.addRoots = PathUtils.formatPaths(roots);
    }

    /**
     * 获取临时文件路径
     * */
    @Override
    public String getTempPath(){
        return PathUtils.buildPath(baseRoot,"tmp");
    }

    /**
     * 写入作品信息
     * */
    @Override
    public void saveArtworkInfo(ArtworkInfo info){
        sourceService.addArtworkInfo(info);
    }
    /**
     * 爬虫运行完成，后续处理，默认什么都不做
     * */
    @Override
    public void postProcess(TaskController controller) {

    }

    @Override
    public SqlSession getSqlSession() {
        return sourceService.sqlSession;
    }

    @Override
    public void clearSanCodes() {
        this.sanCodes = null;
    }

    @Override
    public boolean fileNameExist(String file_name) {
        return sourceService.fileNameExist(file_name);
    }

    @Override
    public int saveBookInfo(BookParentInfo bookParentInfo) {
        return sourceService.saveBookInfo(bookParentInfo);
    }

    @Override
    public int sanCodeExist(String sanCodee) {
        return sourceService.sanCodeExist(sanCodee);
    }
}
