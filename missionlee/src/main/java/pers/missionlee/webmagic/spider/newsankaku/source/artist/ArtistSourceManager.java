package pers.missionlee.webmagic.spider.newsankaku.source.artist;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.SpecialSpiderManager;
import pers.missionlee.webmagic.spider.newsankaku.dao.LevelInfo;
import pers.missionlee.webmagic.spider.newsankaku.source.AbstractSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
        super(baseRoot, roots);
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
            String addPoot = PathUtils.buildPath(addRoots[i], DIR_SANKAKU);
            File[] addRootChildrenFiles = new File(addPoot).listFiles(PathUtils.aimFileFilter());
            System.out.println("下面是："+addPoot+"n欸不文件提取");
            extractPathInfo(addRootChildrenFiles, pics, vids);
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
     */
    public void touchArtist(String name) {
        sourceService.touchArtist(name);
    }

    public void touchArtist(String name, Long nextUpdateTime) {
        sourceService.updateArtist(name, nextUpdateTime);
    }

    /**
     * 获取作者已经收录的作品数量
     */
    @Override
    public int getStoredNum(TaskController controller) {
        AimType aimType = controller.getAimType();
        String artistName = controller.getAimKeys()[0];
        return getArtworkNumOfArtist(aimType, artistName);
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        String artistName = controller.getAimKeys()[0];
        if (sanCodes != null) {
            return sanCodes;
        } else {
            sanCodes = new HashSet<>(sourceService.getSanCodeByArtist(artistName));
            System.out.println(artistName + " 现有作品CODE: " + sanCodes);
            return sanCodes;
        }
    }

//    public Set<String> getStoredSanCodeOfStoreType(TaskController controller,int storeType){
//        String artistName = controller.getAimKeys()[0];
//        if(sanCodes != null){
//            return sanCodes;
//        }else{
//            sanCodes
//        }
//    }
    public int getArtworkNumOfArtist(AimType aimType, String name) {
        if (aimType == AimType.ARTIST) {
            return sourceService.getArtistWorkNum(name);
        } else if (aimType == AimType.COPYRIGHTL) {
            throw new RuntimeException("代码没写完");

        } else {
            throw new RuntimeException("代码没写完");
        }

    }

    /**
     * 获取直接放在作者名字命名文件夹中的作品数量： 用于快速判断这个作者需要在 新下载工作中进行下载嘛
     */
    public int getArtworkNumOfArtistDirectly(String artistName) {

        String picPath = getArtworkDicOfAimArtist(AimType.ARTIST, "a.jpg", artistName);
        String vidPath = getArtworkDicOfAimArtist(AimType.ARTIST, "a.mp4", artistName);
        int pic = new File(picPath).exists() ? new File(picPath).listFiles().length : 0;
        int vid = new File(vidPath).exists() ? new File(vidPath).listFiles().length : 0;
        return pic + vid;
    }

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        System.out.println("获取目标路径 "+controller.getClass());
        String aimName = controller.getAimKeys()[0];
        System.out.println("作者："+aimName);
        String artworkName = info.getFileName();
        System.out.println("作品："+info.getFileName());
        return getArtworkDicOfAimArtist(AimType.ARTIST, artworkName, aimName);
    }
//    public String getAimDic(String artworkName,String aimName){
//        return  getArtworkDicOfAimArtist(AimType.ARTIST,artworkName,aimName);
//    }

    /**
     * 获取作品应该放在的路径
     * 1.已经存在，返回当前位置 2.返回默认路径
     */
    public String getArtworkDicOfAimArtist(AimType aimType, String artworkName, String aimName) {
        String aimFileName = transformAimToFile(aimName);
        // 遍历已收录，看看是不是已经存在
        if (PathUtils.isVideo(artworkName)) {
            Iterator<Map.Entry<String, List<String>>> iterator = PATH_SANKAKU_VIDS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entry = iterator.next();
                // 如果这个文件夹下有这个作者名字 ， 作者名字+"."  ,
                if (entry.getValue().contains(aimFileName)||entry.getValue().contains(aimFileName+"."))
                    return PathUtils.buildPath(entry.getKey(), aimFileName);
            }
            return PathUtils.buildPath(PATH_SANKAKU_DEFAULT_VID, aimFileName);
        } else {
            Iterator<Map.Entry<String, List<String>>> iterator = PATH_SANKAKU_PICS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entry = iterator.next();
                if (entry.getValue().contains(aimFileName)||entry.getValue().contains(aimFileName+"."))
                    return PathUtils.buildPath(entry.getKey(), aimFileName);
            }
            return PathUtils.buildPath(PATH_SANKAKU_DEFAULT_PIC, aimFileName);
        }
    }

    /**
     * 获取作者 Level
     */
    static Pattern priorityPattern = Pattern.compile("-([0-9])-");

    public int getArtistLevel(String artistName) {
        int priority = 10;
        String parentPathPic = getArtworkDicOfAimArtist(AimType.ARTIST, ".jpg", artistName);
        System.out.println(parentPathPic);
        String parentPathVid = getArtworkDicOfAimArtist(AimType.ARTIST, ".mp4", artistName);
        System.out.println(parentPathVid);
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
        System.out.println(artistName+" 等级："+priority);
        return priority;
    }

    public List<String> getArtistsByMaxLevel(boolean onlyNeedUpdate, int maxLevel) {
        return sourceService.getArtists(onlyNeedUpdate, maxLevel);
    }

    // ========= 工具类 =========
    // 有些作者名字叫 a. 最终落盘文件名称为 a，有些作者名字里有不能出现在文件夹名称的字符，所以需要下面两个转换
    public static Map<String, String> specialName = new HashMap<>();

    static {
        System.out.println("读取特殊作者名称===============ArtistSourceManager 220 行 来自 resource/name-changes.txt");
        String filePath = "name-change.txt";
        try {
            String namePairs =IOUtils.toString(ArtistSourceManager.class.getClassLoader().getResourceAsStream(filePath), Charset.forName("utf8"));
            System.out.println(namePairs);
            String[] pairs = namePairs.split("\\r\\n");
            for (int i = 0; i < pairs.length; i++) {
                System.out.println("未拆分："+pairs[i]);
                if(!pairs[i].startsWith("#")){
                    specialName.put(pairs[i].split("~")[0],pairs[i].split("~")[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(specialName);
    }

    /**
     * 将目标名称 转换为 文件名，其中需要处理一些特殊名字，例如作者的名字存在不能在文件夹中出现的字符
     */
    public String transformAimToFile(String aim) {
        aim = specialName.containsKey(aim) ? specialName.get(aim) : aim;
        return aim.endsWith(".")?aim.substring(0,aim.length()-1):aim;
    }

    /***
     * 更新作者等级与路径
     */
    public static List<String> chromeDirs = new ArrayList<>();

    static {
        chromeDirs.add("san1");
        chromeDirs.add("san2");
        chromeDirs.add("san3");
        chromeDirs.add("san4");
        chromeDirs.add("san5");
        chromeDirs.add("san6");
        chromeDirs.add("san7");
        chromeDirs.add("san8");
    }


    public void updateArtistPathAndLevel() throws IOException {
//        if(true)
//        throw new RuntimeException("还没处理，从这边获取的路径信息，和爬虫保存的路径信息不一样的问题，这里报错的是全前缀，不带作者名字，爬虫保存的是相对路径带名字");
        Collection<String> speicals = specialName.values();
        // 1. 谷歌的书签中：从 作者名称列表中 获取或者名称
        ChromeBookmarksReader reader = new ChromeBookmarksReader(SpecialSpiderManager.settings.get("chromePath"));
        List<String> chromeNames = new ArrayList<>();
        for (String dir :
                chromeDirs) {
            List<Map> artistList = reader.getBookMarkListByDirName(dir);
            for (Map bookmark :
                    artistList) {
                String artistName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
                chromeNames.add(artistName);
            }
        }

//        System.out.println(chromeNames);
//        System.out.println("======================");
        // 2. 从本地硬盘获取最新的 等级信息
        Map<String, LevelInfo> diskLevelInfo = new HashMap<>();
        for (Map.Entry<String, List<String>> e :
                PATH_SANKAKU_PICS.entrySet()) {
            String path = e.getKey();
            Matcher matcher = priorityPattern.matcher(path);
            int level = 10;
            if (matcher.find())
                level = Integer.valueOf(matcher.group(1));
            for (String name :
                    e.getValue()) {
                LevelInfo levels = new LevelInfo(name);
                levels.picLevel = level;
                levels.picPath = PathUtils.buildPath(path,transformAimToFile(name));
                diskLevelInfo.put(name, levels);
            }
        }
        for (Map.Entry<String, List<String>> e : PATH_SANKAKU_VIDS.entrySet()) {
            String path = e.getKey();
            Matcher matcher = priorityPattern.matcher(path);
            int level = 10;
            if (matcher.find())
                level = Integer.valueOf(matcher.group(1));
            for (String name :
                    e.getValue()) {
                if (diskLevelInfo.containsKey(name)) {
                    LevelInfo levels = diskLevelInfo.get(name);
                    levels.vidPath = PathUtils.buildPath(path,transformAimToFile(name));
                    levels.vidLevel = level;
                } else {
                    LevelInfo levels = new LevelInfo(name);
                    levels.vidLevel = level;
                    levels.vidPath = PathUtils.buildPath(path,transformAimToFile(name));
                    diskLevelInfo.put(name, levels);
                }

            }

        }
//
        // 从数据库获取现有的等级信息
        List<LevelInfo> dbLevelInfos = sourceService.getLevelInfos();
        Map<String, LevelInfo> dbLevelInfoMap = new HashMap<>();
        for (LevelInfo info :
                dbLevelInfos) {
            dbLevelInfoMap.put(info.name, info);
        }
        int same = 0;
        List<String> sameList = new ArrayList<>();

        int dif = 0;
        List<String> difList = new ArrayList<>();
        int need = 0;
        List<String> targetList = new ArrayList<>();
//        List<String> something = new ArrayList<>();
//        something.add("doumou");
//        something.add("london delly & burry");
        for (Map.Entry<String, LevelInfo> diskInfoEntry : diskLevelInfo.entrySet()
        ) {
            String diskName = diskInfoEntry.getKey();
            String realName = diskName;

            // 1. 根据特殊名称表 修正特殊名称
            if (speicals.contains(diskName)) {
                for (Map.Entry<String, String> e : specialName.entrySet()) {
                    //
                    if (e.getValue().equals(diskName)) realName = e.getKey();
                }
//                realName = specialName.get(diskName);
                System.out.println("特殊名字要变换的： 【磁盘】"+diskName+"=>【数据库/真实】"+realName);
            }
            LevelInfo diskInfo = diskInfoEntry.getValue();
            // 2.根据书签记录 修正 带. 的名称
            if (chromeNames.contains(realName + ".")) {
                realName = realName + ".";
                System.out.println("需要添加一个点的： "+realName);
            }
            // 2.根据数据库记录，修正带 . 的名称
            if (dbLevelInfoMap.containsKey(realName + ".")) {
                realName = realName + ".";
                System.out.println("需要添加一个点的： "+realName);
            }
            diskInfo.name = realName;
            // 3. 如果当前路径信息 与 数据库记录不一样，就更新
            if (dbLevelInfoMap.containsKey(realName)) {
                // 对比信息，需要更新的更新
                if (dbLevelInfoMap.get(realName).equals(diskInfo)) {
                    same++;
                    sameList.add(realName);
                } else {
                    System.out.println("发现一个不同：\n"+diskInfo.name+"磁盘信息 "+ JSON.toJSONString(diskInfo)+"\n"+"数据记录 "+JSON.toJSONString(dbLevelInfoMap.get(realName)));
                    sourceService.updateArtistPathAndLevel(diskInfo.name, diskInfo.picLevel, diskInfo.picPath, diskInfo.vidLevel, diskInfo.vidPath);
                    dif++;
                    difList.add(realName);
                }
            } else {// is_target !=1 的情况（根据爬虫逻辑，必定有这个 作者）
                System.out.println("XXXXXX 注意 如果数据库中某个作者不存在（或is_target）不再将其等级信息更新，因为可能是原本错误记录的copyright:"+realName+" 全路径："+JSON.toJSONString(diskInfoEntry));
                need++;
//                if(sourceService.updateArtistPathAndLevel(dicLevelInfo.name,dicLevelInfo.picLevel,dicLevelInfo.picPath,dicLevelInfo.vidLevel,dicLevelInfo.vidPath) == 0){
//                    System.out.println(realName);
//                }
                targetList.add(realName);

            }
//            for(Map.Entry<String,LevelInfo> e:dbLevelInfoMap.entrySet()){
//                if(!diskLevelInfo.containsKey(e.getKey())){
//                    System.out.println("磁盘上缺少数据库中的："+e.getKey());
//                }
//            }

        }
        System.out.println("没变：" + same + " 变了：" + dif + " 缺失：" + need);
//        System.out.println(sameList);
//        System.out.println(difList);
//        System.out.println(targetList);
    }

    // 查询作者有没有倍放到两个文件夹里面
    public void findTwo() {
        System.out.println("开始筛查重复作品");
        Map<String, String> piccc = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : PATH_SANKAKU_PICS.entrySet()) {
            for (String artistName : entry.getValue()) {
                if (!piccc.containsKey(artistName)){
                    piccc.put(artistName,entry.getKey());
                }else{
                    System.out.println("xxxxx: "+artistName+" | "+piccc.get(artistName)+" | "+entry.getKey());

                }

            }
        }
        Map<String,String> viddd = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : PATH_SANKAKU_VIDS.entrySet()) {
            for (String artistName : entry.getValue()) {
                if (!viddd.containsKey(artistName)){
                    viddd.put(artistName,entry.getKey());
                }else{
                    System.out.println("xxxxx: "+artistName+" | "+viddd.get(artistName)+" | "+entry.getKey());

                }

            }
        }
    }
    @Override
    public void extractAllFileNames(Set<String> names) {
        System.out.println("ArtistSourceManager 提取作品名称----");
        Long timestart = System.currentTimeMillis();
        PATH_SANKAKU_VIDS.forEach((path,nameList)->{

                nameList.forEach(artistName->{
                    String fullPath = path+artistName;
                    String[] artworkNames = new File(fullPath).list();
                    System.out.println(JSON.toJSONString(artworkNames));
                    for (int i = 0; i < artworkNames.length; i++) {
                        names.add(artworkNames[i]);
                    }

                });

        });
        PATH_SANKAKU_PICS.forEach((path,nameList)->{

                nameList.forEach(artistName->{
                    String fullPath = path+artistName;
                    String[] artworkNames = new File(fullPath).list();
                    System.out.println(JSON.toJSONString(artworkNames));
                    for (int i = 0; i < artworkNames.length; i++) {
                        names.add(artworkNames[i]);
                    }

                });

        });
        System.out.println(names.size());
        System.out.println("10秒");
        System.out.println("用时："+(System.currentTimeMillis()-timestart)/1000+"秒");
        try {
            Thread.sleep(1000*10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * 通过文件目录，判断一个artist是否存在
     * */
    public boolean pathNameExists(String artistName){
        String pathName = transformAimToFile(artistName);
        for (String levelPath :
                PATH_SANKAKU_PICS.keySet()) {
            List<String> names = PATH_SANKAKU_PICS.get(levelPath);
            if(names.contains(pathName)) return true;
        }
        for (String levelPath :
                PATH_SANKAKU_VIDS.keySet()) {
            List<String> names = PATH_SANKAKU_VIDS.get(levelPath);
            if(names.contains(pathName)) return true;
        }
        return false;
    }
    public static void main(String[] args) throws IOException {
//        LevelInfo l1 = new LevelInfo("hello");
//        LevelInfo l2 = new LevelInfo("hello");
//        System.out.println(l1.equals(l2) );
//        ArtistSourceManager sourceManager = new ArtistSourceManager("H:\\ROOT", "G:\\ROOT");
//        sourceManager.findTwo();
//        sourceManager.updateArtistPathAndLevel();
//        sourceManager.lll();
//        SpecialSpiderManager manager = new SpecialSpiderManager(new ArtistSourceManager("G:\\ROOT", "H:\\ROOT"));
//        manager.downloadArtist("aestheticc-meme", WorkMode.NEW);
//        manager.downLoadArtistByLevel(20,true,false,WorkMode.UPDATE);
//        manager.downLoadChromeArtistDir("san1");
//        manager.downLoadChromeArtistDir("san2");
//        manager.downLoadChromeArtistDir("san3");
//        manager.downLoadChromeArtistDir("san4");
//        manager.downLoadChromeArtistDir("san5");
//        manager.downLoadChromeArtistDir("san6");
//        manager.downLoadChromeArtistDir("san7");
//        manager.downLoadChromeArtistDir("san8");
    }

}
