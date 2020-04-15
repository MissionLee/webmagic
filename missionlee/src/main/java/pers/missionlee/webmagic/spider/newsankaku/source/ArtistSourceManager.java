package pers.missionlee.webmagic.spider.newsankaku.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-29 17:08
 */
public class ArtistSourceManager extends AbstractSourceManager {
    static Logger logger = LoggerFactory.getLogger(ArtistSourceManager.class);



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

    public ArtistSourceManager(String baseRoot, String... roots) {
        super(baseRoot,roots);
        init();
    }

    /**
     * 初始化本地文件路径数据
     */
    private void init() {
        this.PATH_SANKAKU_BASE_ROOT = PathUtils.buildPath(baseRoot + DIR_SANKAKU);
        this.PATH_SANKAKU_DEFAULT_PIC = PathUtils.buildPath(PATH_SANKAKU_BASE_ROOT, DIR_PIC);
        this.PATH_SANKAKU_DEFAULT_VID = PathUtils.buildPath(PATH_SANKAKU_BASE_ROOT, DIR_VID);
        // 初始化默认路径 和 已经分类的特殊路径
        Map pics = new HashMap();
        Map vids = new HashMap();
        File[] baseRootChildrenFiles = new File(PATH_SANKAKU_BASE_ROOT).listFiles(PathUtils.aimFileFilter());
        extractPathInfo(baseRootChildrenFiles, pics, vids);
        for (int i = 0; i < addRoots.length; i++) {
            String addPoot = PathUtils.buildPath(addRoots[i],DIR_SANKAKU);
            File[] addRootChildrenFiles = new File(addPoot).listFiles(PathUtils.aimFileFilter());
            extractPathInfo(addRootChildrenFiles,pics,vids);
        }
        PATH_SANKAKU_PICS = pics;
        PATH_SANKAKU_VIDS = vids;

    }

    private void extractPathInfo(File[] childrenDir, Map picMap, Map vidMap) {
        System.out.println(childrenDir.length);
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
                vidMap.put(PathUtils.buildPath(childrenDir[i].getPath()), Arrays.asList(artists));
        }
        System.out.println("------------");
        System.out.println(picMap);
        System.out.println(vidMap);
    }

    /**
     * touch 作者信息
     * */
    public void touchArtist(String name){
        sourceService.touchArtist(name);
    }
    public void touchArtist(String name,Long nextUpdateTime){
        sourceService.updateArtist(name,nextUpdateTime);
    }

    /**
     * 获取作者已经收录的作品数量
     * */
    @Override
    public int getStoredNum(TaskController controller) {
        AimType aimType = controller.getAimType();
        String artistName = controller.getAimKeys()[0];
        return getArtworkNumOfArtist(aimType,artistName);
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        String artistName = controller.getAimKeys()[0];
        if(sanCodes!=null){
            return sanCodes;
        }
        else{
            sanCodes =new HashSet<>(sourceService.getSanCodeByArtist(artistName)) ;
            return sanCodes;
        }
    }


    public int getArtworkNumOfArtist(AimType aimType, String name){
        if(aimType == AimType.ARTIST){
            return sourceService.getArtistWorkNum(name);
        }else if(aimType == AimType.COPYRIGHTL){
            throw new RuntimeException("代码没写完");

        }else{
            throw new RuntimeException("代码没写完");
        }

    }
    /**
     * 获取直接放在作者名字命名文件夹中的作品数量： 用于快速判断这个作者需要在 新下载工作中进行下载嘛
     * */
    public int getArtworkNumOfArtistDirectly(String artistName){

        String picPath = getArtworkDicOfAimArtist(AimType.ARTIST,"a.jpg",artistName);
        String vidPath = getArtworkDicOfAimArtist(AimType.ARTIST,"a.mp4",artistName);
        int pic = new File(picPath).exists()?new File(picPath).listFiles().length:0;
        int vid = new File(vidPath).exists()?new File(vidPath).listFiles().length:0;
        return pic+vid;
    }

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        String aimName = controller.getAimKeys()[0];
        String artworkName = info.getName();
        return getArtworkDicOfAimArtist(AimType.ARTIST,artworkName,aimName);
    }

    /**
     * 获取作品应该放在的路径
     * 1.已经存在，返回当前位置 2.返回默认路径
     * */
    private String getArtworkDicOfAimArtist(AimType aimType, String artworkName, String aimName){
        String aimFileName = transformAimToFile(aimName);
        // 遍历已收录，看看是不是已经存在
        if(PathUtils.isVideo(artworkName)){
            Iterator<Map.Entry<String, List<String>>> iterator = PATH_SANKAKU_VIDS.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, List<String>> entry = iterator.next();
                if(entry.getValue().contains(aimFileName))
                    return PathUtils.buildPath(entry.getKey(),aimFileName);
            }
            return PathUtils.buildPath(PATH_SANKAKU_DEFAULT_VID,aimFileName);
        }else{
            Iterator<Map.Entry<String, List<String>>> iterator = PATH_SANKAKU_PICS.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, List<String>> entry = iterator.next();
                if(entry.getValue().contains(aimFileName))
                    return PathUtils.buildPath(entry.getKey(),aimFileName);
            }
            return PathUtils.buildPath(PATH_SANKAKU_DEFAULT_PIC,aimFileName);
        }
    }
    /**
     * 获取作者 Level
     * */
    static Pattern priorityPattern = Pattern.compile("-([0-9])-");
    public int getArtistLevel(String artistName){
        int priority = 10;
        String parentPathPic = getArtworkDicOfAimArtist(AimType.ARTIST, ".jpg", artistName);
        String parentPathVid = getArtworkDicOfAimArtist(AimType.ARTIST, ".mp4", artistName);
        Matcher matcherPic = priorityPattern.matcher(parentPathPic);
        Matcher matcherVid = priorityPattern.matcher(parentPathVid);
        if (matcherPic.find()) {
            int tmpPriority = Integer.valueOf(matcherPic.group(1));
            priority = priority > tmpPriority ? tmpPriority : priority;
        }
        if (matcherVid.find()) {
            int tmpPriority = Integer.valueOf(matcherVid.group(1));
            priority = priority > tmpPriority ? tmpPriority : priority;
        }
        return priority;
    }
    public List<String> getArtistsByMaxLevel(boolean onlyNeedUpdate,int maxLevel){
        return sourceService.getArtists(onlyNeedUpdate,maxLevel);
    }
    // ========= 工具类 =========
    // 有些作者名字叫 a. 最终落盘文件名称为 a，有些作者名字里有不能出现在文件夹名称的字符，所以需要下面两个转换
    public static Map<String,String> specialName = new HashMap<>();
    static {
        specialName.put("xiao shei..","xiao shei__");
    }
    /**
     * 将目标名称 转换为 文件名，其中需要处理一些特殊名字，例如作者的名字存在不能在文件夹中出现的字符
     * */
    public  String transformAimToFile(String aim){
        return specialName.containsKey(aim)?specialName.get(aim):aim;
    }

}
