package pers.missionlee.webmagic.spider.newsankaku.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-29 17:08
 */
public class NewSourceManager {
    static Logger logger = LoggerFactory.getLogger(NewSourceManager.class);
    /**
     * baseRoot：基础目录（权限目标的作品存储默认位置）
     * roots:分盘存储目录
     */
    private String baseRoot;
    private String[] addRoots;
    /**
     * 约定的路径名称
     */
    private static final String DIR_SANKAKU = "sankaku";
    private static final String DIR_IDOL = "idol";
    private static final String DIR_TMP = "tmp";
    private static final String DIR_PIC = "pic";
    private static final String DIR_VID = "vid";
    private static final String DIR_INFO = "info";
    /**
     * 分类路径 ： Sankaku 路径
     */
    // 默认路径，新目标下载需要用
    private String PATH_SANKAKU_BASE_ROOT;
    private String PATH_SANKAKU_DEFAULT_PIC;
    private String PATH_SANKAKU_DEFAULT_VID;
    // 新目标路径文件
    private File FILE_DEFAULT_PIC;
    private File FILE_DEFAULT_VID;
    // 已经分类的作者（包含 默认路径）
    private Map<String, List<String>> PATH_SANKAKU_PICS;
    private Map<String, List<String>> PATH_SANKAKU_VIDS;

    public NewSourceManager(String baseRoot, String... roots) {
        this.baseRoot = PathUtils.formatPath(baseRoot);
        this.addRoots = PathUtils.formatPaths(roots);
        init();
    }

    /**
     * 初始化本地文件路径数据
     */
    private void init() {
        this.PATH_SANKAKU_BASE_ROOT = PathUtils.buildPath(baseRoot + DIR_SANKAKU);
        this.PATH_SANKAKU_DEFAULT_PIC = PathUtils.buildPath(baseRoot, DIR_PIC);
        this.PATH_SANKAKU_DEFAULT_VID = PathUtils.buildPath(baseRoot, DIR_VID);
        // 初始化默认路径 和 已经分类的特殊路径
        Map pics = new HashMap();
        Map vids = new HashMap();
        File[] baseRootChildrenFiles = new File(baseRoot).listFiles(PathUtils.aimFileFilter());
        extractPathInfo(baseRootChildrenFiles, pics, vids);
        for (int i = 0; i < addRoots.length; i++) {
            String addPoot = PathUtils.buildPath(addRoots[i],DIR_SANKAKU);
            File[] addRootChildrenFiles = new File(addPoot).listFiles(PathUtils.aimFileFilter());
            extractPathInfo(addRootChildrenFiles,pics,vids);
        }

    }

    private void extractPathInfo(File[] childrenDir, Map picMap, Map vidMap) {
        for (int i = 0; i < childrenDir.length; i++) {
            String[] artists = childrenDir[i].list();
            String childDirName = childrenDir[i].getName();
            if (childDirName.startsWith("pic")
                    || childDirName.startsWith("图-")
                    || childDirName.startsWith("T-"))
                picMap.put(PathUtils.buildPath(childrenDir[i].getPath()), Arrays.asList(artists));
            else if (childDirName.startsWith("vid")
                    || childDirName.startsWith("V-")
                    || childDirName.startsWith("视-"))
                picMap.put(PathUtils.buildPath(childrenDir[i].getPath()), Arrays.asList(artists));
        }
    }
    /**
     * 获取作者已经收录的作品数量
     * */
    public int getArtworkNumOfArtist(AimType aimType, String name){
        if(aimType == AimType.ARTIST){
            return 10;

        }else if(aimType == AimType.COPYRIGHTL){
            return 10;
        }
        return 10;

    }
    /**
     * 获取作品应该放在的路径
     * 1.已经存在，返回当前位置 2.返回默认路径
     * */
    public String getArtworkPathOfAim(AimType aimType, String artworkName,String aimName){
        String aimFileName = transformAimToFile(aimName);
        // 遍历已收录，看看是不是已经存在
        if(PathUtils.isVideo(artworkName)){
            Iterator<Map.Entry<String, List<String>>> iterator = PATH_SANKAKU_VIDS.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, List<String>> entry = iterator.next();
                if(entry.getValue().equals(aimFileName))
                    return PathUtils.buildPath(entry.getKey(),artworkName);
            }
            return PathUtils.buildPath(PATH_SANKAKU_DEFAULT_VID,aimFileName);
        }else{
            Iterator<Map.Entry<String, List<String>>> iterator = PATH_SANKAKU_PICS.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, List<String>> entry = iterator.next();
                if(entry.getValue().equals(aimFileName))
                    return PathUtils.buildPath(entry.getKey(),artworkName);
            }
            return PathUtils.buildPath(PATH_SANKAKU_DEFAULT_PIC,aimFileName);
        }
    }
    // ========= 工具类 =========
    // 有些作者名字叫 a. 最终落盘文件名称为 a，有些作者名字里有不能出现在文件夹名称的字符，所以需要下面两个转换
    public  String transformAimToFile(String aim){
        return aim;
    }
    public  String transformFileToAim(String file){
        return file;
    }
}
