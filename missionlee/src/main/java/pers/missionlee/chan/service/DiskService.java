package pers.missionlee.chan.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.newsankaku.dao.LevelInfo;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-10 18:33
 */
public class DiskService {


    Logger logger = LoggerFactory.getLogger(DiskService.class);
    String PIC = "pic";
    String VID = "vid";
    SpiderSetting spiderSetting;
    String tempPath;
    // ===================  artist 目录信息  包括： 1. 普通artist目录 2. 整理后 artist 目录 3.  artist parent book 目录
    private String CHAN_ARTIST_BASE;
    private String CHAN_ARTIST_DEFAULT_PIC;
    private String CHAN_ARTIST_DEFAULT_VID;
    private Map<String, List<String>> CHAN_ARTIST_PICS;
    private Map<String, List<String>> CHAN_ARTIST_VIDS;
    private Map<String, String> namePairs;
    private String CHAN_ERROR_BASE;

    public DiskService(SpiderSetting spiderSetting) {
        this.spiderSetting = spiderSetting;
        this.tempPath = PathUtils.buildPath(spiderSetting.getArtistBase(), "tmp");
        initArtistInfo();
        initNamePairsInfo();
    }

    private void initNamePairsInfo() {
        this.namePairs = new HashMap<>();
        if (StringUtils.isEmpty(spiderSetting.getNamePairs())) {

        } else {
            try {
                String namePairs = FileUtils.readFileToString(new File(spiderSetting.getNamePairs()), "UTF-8");
                String[] pairs = namePairs.split("\\r\\n");
                for (int i = 0; i < pairs.length; i++) {
                    if (!pairs[i].startsWith("#")) {
                        this.namePairs.put(pairs[i].split("~")[0], pairs[i].split("~")[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("初始化特殊作者名称完成  key 是真是名字， value 是路径名字");
    }
    public void cleanDelPath() throws InterruptedException {
        String delPath = spiderSetting.delPath;
        logger.warn("----将要清空"+delPath+"下的所有文件");
        logger.warn("----距离操作开始还有60s");
        Thread.sleep(30000);
        logger.warn("----距离操作开始还有30s");
        Thread.sleep(20000);
        logger.warn("----距离操作开始还有10s");
        Thread.sleep(10000);
        logger.warn("----开始执行");
        doDelFile(new File(delPath));
    }
    private void doDelFile(File rootDir){
        File[] files = rootDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File thisFile = files[i];
            if(thisFile.isDirectory()){
                logger.info(thisFile.getPath()+thisFile.getName()+"是目录，递归清空其中文件");
                doDelFile(thisFile);
            }else{
                logger.info(thisFile.getPath()+thisFile.getName()+" 执行删除");
                thisFile.delete();
            }
        }
    }
    private void initArtistInfo() {
        this.CHAN_ARTIST_BASE = PathUtils.buildPath(spiderSetting.getArtistBase());
        this.CHAN_ARTIST_DEFAULT_PIC = PathUtils.buildPath(this.CHAN_ARTIST_BASE, PIC);
        this.CHAN_ARTIST_DEFAULT_VID = PathUtils.buildPath(this.CHAN_ARTIST_BASE, VID);
        // 初始化默认路径 和 分类路径，此处包括 book parent 部分
        Map pics = new HashMap();
        Map vids = new HashMap();
        // 提取默认路径下的 作者分布信息
        File[] baseRootChildrenFiles = new File(CHAN_ARTIST_BASE).listFiles(PathUtils.aimFileFilter());
        extractPathInfo(baseRootChildrenFiles, pics, vids);
        // 提取 作者分级路径下的 作者分布信息
        for (int i = 0; i < spiderSetting.getNormalAddArtistBases().length; i++) {
            String addPoot = PathUtils.buildPath(spiderSetting.getNormalAddArtistBases()[i]);
            System.out.println("###### "+addPoot);
            File[] addRootChildrenFiles = new File(addPoot).listFiles(PathUtils.aimFileFilter());
            extractPathInfo(addRootChildrenFiles, pics, vids);
        }
        // 提取 作者 book parent 路径下的 作者分布信息
        File[] artistBookParentFiles = new File(spiderSetting.getBookParentArtistBase()).listFiles(PathUtils.aimFileFilter());
        if (artistBookParentFiles != null && artistBookParentFiles.length > 0) {
            extractPathInfo(artistBookParentFiles, pics, vids);
        }
        this.CHAN_ARTIST_PICS = pics;
        this.CHAN_ARTIST_VIDS = vids;
        logger.info("初始化作者信息完成");
    }

    public void touchArtist(String name) {
        String path = getCommonArtistParentPath(name, "1.jpg");
        new File(path).setLastModified(System.currentTimeMillis());
    }

    public void findArtistUnTargeted(DataBaseService dataBaseService) {
        logger.info("检查磁盘上的作者，在数据库中没有标记为 is_target 的  和关联作品数量为0的");
        CHAN_ARTIST_VIDS.forEach((String path, List<String> pathNames) -> {
            pathNames.forEach((String pathName) -> {
                String realName = pathName;
                if (namePairs.containsValue(pathName)) {
                    realName = getKey(namePairs, pathName);
                }
                boolean isTarget = dataBaseService.checkArtistIsTarget(realName);
                if (isTarget) {

                } else {
                    logger.info("作者" + realName + "  没有被标记为目标：" + path + pathName);
                    Map<String, String> params = new HashMap<>();
                    params.put("name", realName);
                    logger.info("数据库中创建：" + realName);
                    try {
                        dataBaseService.sqlSession.insert("san.insertArtistOnlyName", params);
                    } catch (Exception e) {
                        logger.info("发现了某个文件夹对应的作者不存在，或者非 isTarget，创建作者时候报错，此错误可能忽略，因为可能是isTarget情况");
                    }
                    dataBaseService.touchArtist(realName, System.currentTimeMillis());
                }
            });
        });
        CHAN_ARTIST_PICS.forEach((String path, List<String> pathNames) -> {
            pathNames.forEach((String pathName) -> {
                String realName = pathName;
                if (namePairs.containsValue(pathName)) {
                    realName = getKey(namePairs, pathName);
                }
                boolean isTarget = dataBaseService.checkArtistIsTarget(realName);
                if (isTarget) {

                } else {
                    logger.info("作者" + realName + "  没有被标记为目标：" + path + pathName);
                    Map<String, String> params = new HashMap<>();
                    params.put("name", realName);
                    logger.info("数据库中创建：" + realName);
                    try {
                        dataBaseService.sqlSession.insert("san.insertArtistOnlyName", params);
                    } catch (Exception e) {
                        logger.info("发现了某个文件夹对应的作者不存在，或者非 isTarget，创建作者时候报错，此错误可能忽略，因为可能是isTarget情况");
                    }
                    dataBaseService.touchArtist(realName, System.currentTimeMillis());
                }
            });
        });
    }

    public static String getKey(Map<String, String> map, Object value) {
        String key = "";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                key = entry.getKey();
                continue;
            }
        }
        return key;
    }

    /**
     * srcDirPath:
     * destDirPath:
     */
    public void moveAllSubFiles(String srcDirPath, String destDirPath) throws IOException {
        File src = new File(srcDirPath);
        File dest = new File(destDirPath);
        File[] files = src.listFiles();
        if (null != files)
            for (int i = 0; i < files.length; i++) {
                try {
                    if (files[i].isFile()) {
                        FileUtils.moveFileToDirectory(files[i], dest, true);
                    } else if (files[i].isDirectory()) {
                        FileUtils.moveDirectoryToDirectory(files[i], dest, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        src.delete();
    }

    public void findPathError() {
        logger.info("此功能用于查找有多个 作品路径的作者");
        logger.info("检查 Vid 大系列 和  Pic 大系列 有没有重复的情况");
        CHAN_ARTIST_VIDS.forEach((String iParentVidPath, List<String> iVidNames) -> {
            iVidNames.forEach((String iVidName) -> {
                Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_PICS.entrySet().iterator();
                while (iterator.hasNext()) {

                    Map.Entry<String, List<String>> entry = iterator.next();
                    String iParentPicPath = entry.getKey();
                    if (entry.getValue().contains(iVidName)) {
                        System.out.println("路径：" + iParentVidPath + iVidName + "_____路径：" + iParentPicPath + iVidName);
                    }
                }
            });
        });
        logger.info("检查 Pic 大系列内部有没有重复的情况");
        CHAN_ARTIST_PICS.forEach((String iParentVidPath, List<String> iVidNames) -> {
            iVidNames.forEach((String iVidName) -> {
                Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_PICS.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, List<String>> entry = iterator.next();
                    String iParentPicPath = entry.getKey();
                    if (entry.getValue().contains(iVidName) && !iParentPicPath.equals(iParentVidPath)) {
                        System.out.println("视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + iParentPicPath + iVidName);
                    }
                }
            });
        });
        logger.info("检查 VID 大系列内部有没有重复的情况");
        CHAN_ARTIST_VIDS.forEach((String iParentVidPath, List<String> iVidNames) -> {
            iVidNames.forEach((String iVidName) -> {
                Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_VIDS.entrySet().iterator();
                while (iterator.hasNext()) {

                    Map.Entry<String, List<String>> entry = iterator.next();
                    String iParentPicPath = entry.getKey();
                    if (entry.getValue().contains(iVidName) && !iParentPicPath.equals(iParentVidPath)) {
                        System.out.println("视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + iParentPicPath + iVidName);
                    }
                }
            });
        });
    }

    public List<String> getBookArtistByLevel(int lv, boolean onlyDaiDing) {
        List<String> artistNames = new ArrayList<>();
        Map<String, List<String>> bookArtistPaths = new HashMap<>();
        File[] artistBookParentFiles = new File(spiderSetting.getBookParentArtistBase()).listFiles(PathUtils.aimFileFilter());

        extractPathInfo(artistBookParentFiles, bookArtistPaths, bookArtistPaths);
        bookArtistPaths.forEach((String parentPath, List<String> artists) -> {
            boolean skip = false;
            if (onlyDaiDing && !parentPath.contains("待定")) {
                skip = true;
            }
            if (skip) {

            } else {
                Matcher matcher = priorityPattern.matcher(parentPath);
                if (matcher.find()) {
                    int level = Integer.valueOf(matcher.group(1));
                    if (level == lv) {
                        artists.forEach((String name) -> {
                            artistNames.add(transformPathToArtistName(name));
                        });
                    }
                }
            }

        });
        logger.info("获得等级为 " + lv + " 的book作者 = onlyDaiDing" + onlyDaiDing);
        logger.info(artistNames.toString());
        return artistNames;
    }

    public boolean fileExistsUnderArtist(String fileName, String artistName) {
        Map<String, String> files = getArtistFilePath(artistName);
        return files.containsKey(fileName);
    }

    public void mergePicVid() {
        System.out.println(" 开始  merge ");
        //  Map<String, List<String>>  CHAN_ARTIST_PICS  CHAN_ARTIST_VIDS
        // 1. 如果  图片和视频都在 3D 目录下面  把图片挪到 vid下面
        // 2. 如果 图片在 非3d 视频在 3d 挪到 非3d
        AtomicBoolean doooo = new AtomicBoolean(false);
        CHAN_ARTIST_VIDS.forEach((String iParentVidPath, List<String> iVidNames) -> {
            iVidNames.forEach((String iVidName) -> {
                Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_PICS.entrySet().iterator();
                while (iterator.hasNext()) {

                    Map.Entry<String, List<String>> entry = iterator.next();
                    String iParentPicPath = entry.getKey();
                    if (entry.getValue().contains(iVidName)) {
                        if (iParentVidPath.contains("/CHAN-ARTIST-3D/V-3-留档3d") || iParentVidPath.contains("/CHAN-ARTIST-BP/V-0-BP")) {
                            System.out.println("视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + entry.getKey() + iVidName);
                            try {
                                moveAllSubFiles(iParentVidPath + iVidName, iParentPicPath + iVidName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (iParentVidPath.contains("/CHAN-ARTIST-3D/V-0-升级")) {
                            System.out.println("视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + entry.getKey() + iVidName);

                            try {
                                moveAllSubFiles(iParentPicPath + iVidName, iParentVidPath + iVidName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
//                        else if (iParentVidPath.contains("CHAN-ARTIST-3DVID/") && iParentPicPath.contains("CHAN-ARTIST-3DPIC/")) {
//                            int picLevel = extractLevelFromPathString(iParentPicPath);
//                            int vidLevel = extractLevelFromPathString(iParentVidPath);
//                            if(picLevel<vidLevel){
//                                try {
//                                    moveAllSubFiles(iParentVidPath + iVidName, iParentPicPath + iVidName);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//
//                            }else{
//                                try {
//                                    moveAllSubFiles(iParentPicPath + iVidName, iParentVidPath + iVidName);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//
//                            }
//                        }
                        else {
                            System.out.println("仅仅展示还剩什么： 视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + entry.getKey() + iVidName);

                        }
//                        boolean moved = false;
//                        if (!moved &&
//                                iParentPicPath.contains("废弃") &&
//                                !iParentVidPath.contains("废弃")) {
//
//                            moved = true;
//                        }
//                        if (!moved &&
//                                !iParentPicPath.contains("废弃") &&
//                                iParentVidPath.contains("废弃")) {
//                            moved = true;
//
//                        }
//                        if (!moved &&
//                                iParentPicPath.contains("废弃") &&
//                                iParentVidPath.contains("废弃")) {
//                            moved = true;
//
//                        }
//                        if (!moved
//                                && iParentVidPath.contains("2D")) {
//                            moved = true;
//
//                        }
//                        if (!moved   // 视频在整理后的 视频中 ，图片是 3D内容
//                                && iParentVidPath.contains("CHAN-ARTIST-3DVID")
//                                && iParentPicPath.contains("3D")
//                        ) {
//                            System.out.println("视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + entry.getKey() + iVidName + "____移动到   XXXX 视频");
//                            moved = true;
//                        }
//
//                        if (!moved // 视频在 3D 中  图片在整理后的 图片中， 那么移动到图片中
//                                && iParentVidPath.contains("CHAN-ARTIST-3D")
//                                && (iParentPicPath.contains("整理") || iParentPicPath.contains("特别"))
//                        ) {
//                            System.out.println("视频路径：" + iParentVidPath + iVidName + "_____图片路径：" + entry.getKey() + iVidName + "____移动到 YYYY 图片");
//                            moved = true;
//
//                        }
//                        if (!moved // 图片没有真理过  视频整理过了 放到视频厘米那
//                                && iParentPicPath.contains("/pic/") && !iParentVidPath.contains("/vid/")
//                        ) {
//                            moved = true;
//
//                        }
//                        if (!moved // 图片整理过，视频没有整理过  放在图片里面
//                                && !iParentPicPath.contains("/pic/")
//                                && iParentVidPath.contains("/vid/")) {
//                            moved = true;
//
//                        }
//                        if (!moved  // 都在 临时路径，放到 pic里面去
//                                && iParentPicPath.contains("/pic/")
//                                && iParentVidPath.contains("/vid/")) {
//                            moved = true;
//
//                        }
////                        if()
                    }

                }
            });

        });
        System.out.println(" 000000000000000000000 end  000000000000000000");
        System.out.println(" 000000000000000000000 end  000000000000000000");
        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean copyFileFromRelatedArtist(ArtworkInfo artworkInfo) {
        List<String> names = artworkInfo.getTagArtist();
        if (null != names && names.size() > 0) {
            for (int i = 0; i < names.size(); i++) {
                String iName = names.get(i);
                if (!artworkInfo.aimName.equals(iName)) {
                    logger.info("发现不同的TagName =>  AimName：" + artworkInfo.aimName + "_tagName: " + iName);
                    Map<String, String> tagNameFileNames = getArtistFilePath(iName);
                    if (tagNameFileNames.containsKey(artworkInfo.fileName)) {
                        String nowPath = tagNameFileNames.get(artworkInfo.fileName);
                        logger.info("在:" + nowPath + "  找到文件");
//                        logger.info("找到了对应文件，下面进行复制或剪切：" + nowPath);
                        String tempPath = nowPath + ".tmp";
                        File nowFile = new File(nowPath);
                        if (nowPath.substring(nowPath.lastIndexOf("\\")).contains("_")
                                || artworkInfo.storePlace == ArtworkInfo.STORE_PLACE.COPYRIGHT.storePlace
                                || artworkInfo.storePlace == ArtworkInfo.STORE_PLACE.STUDIO.storePlace) {
                            logger.info("当前作品在其他作者特殊目录下，所以执行--复制");
                            File tempFile = new File(tempPath);
                            try {
                                FileUtils.copyFile(nowFile, tempFile);
                                saveFile(tempFile, artworkInfo, artworkInfo.PBPrefix, artworkInfo.fileSaveName);
                                return true;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            logger.info("当前作品在其他作者普通目录下，所以执行--剪切");
                            saveFile(nowFile, artworkInfo, artworkInfo.PBPrefix, artworkInfo.fileSaveName);
                            return true;

                        }


                    } else {
                        logger.info("关联作者：" + iName + " 名下没有这个文件");
                    }
                }
            }

        }
        return false;
    }

    public String getCopyrightParentPath(String copyright) {
        return PathUtils.buildPath(spiderSetting.copyrightBase, copyright);
    }

    public Map<String, String> getCopyRightFileMd5(String copyright) {
        Map<String, String> filePath = getCopyRightFilePath(copyright);
        Map<String, String> md5 = new HashMap<>();
        filePath.forEach((String fileName, String path) -> {
            md5.put(fileName.substring(0, fileName.indexOf(".")), path);
        });
        return md5;
    }

    public Map<String, String> getCopyRightFilePath(String copyright) {
        String copyrightPath = getCopyrightParentPath(copyright);
        File copyParentFile = new File(copyrightPath);
        Map<String, String> filePathMap = new HashMap<>();
        if (copyParentFile.exists()) {
            File[] files = copyParentFile.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {// 如果是目录
                        String[] artworks = files[i].list();
                        for (int j = 0; j < artworks.length; j++) {
                            String fullPath = copyrightPath + files[i].getName() + "/" + artworks[j];
                            String fileName = artworks[j].substring(5);
                            if (files[i].getName().contains("low"))
                                fileName = artworks[j];

                            filePathMap.put(fileName, fullPath);
                        }
                    } else {
                        filePathMap.put(files[i].getName(), copyrightPath + files[i].getName());
                    }
                }
            }
        }
        return filePathMap;
    }
    /**
     * ⭐ ⭐  获取作者的 所有相关路径
     * */
    public Map<String, String> getArtistFilePath(String artistName) {
        if(artistName.equals("")){
            logger.warn("出现了 artistname 为空的情况， 还没注意是什么问题，可能是 关联作者有时候关联出来“空”");
            return new HashMap<>();
        }
        String picBasePath = getCommonArtistParentPath(artistName, "1.jpg");
        String vidBasePath = getCommonArtistParentPath(artistName, "1.mp4");
        Map<String, String> filePath = new HashMap<>();
        // pic 部分
        File picbase = new File(picBasePath);
        if(picbase.exists()){
            findPathRecursion(filePath,picbase);
        }
//        File[] picFiles = picbase.listFiles();
//        if (null != picFiles) {
//            for (int i = 0; i < picFiles.length; i++) {
//                if (picFiles[i].isDirectory()) {// 如果是目录
//                    if (picFiles[i].getName().equals("del")||picFiles[i].getName().equals("zdel")) {
//                        logger.info("初始化--> 忽略"+picFiles[i].getName()+"中的文件");
//                    } else{
//                        String[] artworks = picFiles[i].list();
//                        for (int j = 0; j < artworks.length; j++) {
//
//                                String fullPath = picBasePath + picFiles[i].getName() + "/" + artworks[j];
//                                String fileName = artworks[j].substring(5);
//                                if (picFiles[i].getName().contains("low"))
//                                    fileName = artworks[j];
//
//                                filePath.put(fileName, fullPath);
//
//
//                        }
//                    }
//                } else {
//                    filePath.put(picFiles[i].getName(), picBasePath + picFiles[i].getName());
//                }
//            }
//        }

        // vid 部分
        File vidbase = new File(vidBasePath);
        if(vidbase.exists()){
            findPathRecursion(filePath,vidbase);
        }
//        File[] vidFiles = vidbase.listFiles();
//        if (null != vidFiles) {
//            for (int i = 0; i < vidFiles.length; i++) {
//                if (vidFiles[i].isFile()) {
//                    filePath.put(vidFiles[i].getName(), vidBasePath + vidFiles[i].getName());
//                }
//            }
//        }
        return filePath;
    }
    /**
     * 递归生成 文件名 和 路径信息，放入给定的map中
     * */
    public static void findPathRecursion(Map<String,String> map,File baseFile){
        if(baseFile.getName().equals("s.json")){
            return;
        }
        if(baseFile.isDirectory()){
            File[] subFile = baseFile.listFiles();
            for (int i = 0; i < subFile.length; i++) {
                findPathRecursion(map,subFile[i]);
            }
        }else{
            String fileName = baseFile.getName();
            //如果有序号的文件，去除序号
            if(fileName.contains("_"))
                fileName=fileName.substring(5);
//            System.out.println("xxx key"+fileName+"   value "+baseFile.getPath());
            map.put(fileName,baseFile.getPath());
        }
    }
    public Map<String, String> getArtistFileMd5Path(String artistName) {
        Map<String, String> fileNamePaht = getArtistFilePath(artistName);
        Map<String, String> md5 = new HashMap<>();
        fileNamePaht.forEach((String fileName, String path) -> {

            md5.put(fileName.substring(0, fileName.indexOf(".")), path);
        });
        return md5;
    }

    Map<String, Integer> artistPicLevel; // key 为 path name
    Map<String, Integer> artistVidLevel;// key 为 path name
    Map<String, String> artistPicPath;// key 为 path name
    Map<String, String> artistVidPath;// key 为 path name
    static Pattern priorityPattern = Pattern.compile("-([0-9])-");

    public int extractLevelFromPathString(String path) {
        Matcher matcher = priorityPattern.matcher(path);
        if (matcher.find()) return Integer.valueOf(matcher.group(1));
        return 100;
    }

    public void initArtistPathLevel() {
        if (null == artistPicLevel) {
            artistPicLevel = new HashMap<>();
            artistPicPath = new HashMap<>();
            CHAN_ARTIST_PICS.forEach((String path, List<String> artists) -> {
                Matcher matcher = priorityPattern.matcher(path);
                int level = 10;
                if (matcher.find())
                    level = Integer.valueOf(matcher.group(1));
                int finalLevel = level;
                artists.forEach((String name) -> {
                    artistPicLevel.put(name, finalLevel);
                    artistPicPath.put(name, path + name);
                });
            });
        }
        if (null == artistVidLevel) {
            artistVidPath = new HashMap<>();
            artistVidLevel = new HashMap<>();
            CHAN_ARTIST_VIDS.forEach((String path, List<String> artists) -> {
                Matcher matcher = priorityPattern.matcher(path);
                int level = 10;
                if (matcher.find())
                    level = Integer.valueOf(matcher.group(1));
                int finalLevel = level;
                artists.forEach((String name) -> {
                    artistVidLevel.put(name, finalLevel);
                    artistVidPath.put(name, path + name);
                });
            });
        }
    }

    public void cleanArtistBookParentBases(String name) {
        String picBasePath = getCommonArtistParentPath(name, "1.jpg");
        String vidBasePath = getCommonArtistParentPath(name, "1.mp4");
        List<String> bookOrParentedFiles = new ArrayList<>();
        File[] picSubFiles = new File(picBasePath).listFiles();
        if (null != picSubFiles) {
            for (int i = 0; i < picSubFiles.length; i++) {
                if (picSubFiles[i].isDirectory()) {
                    String[] artworks = picSubFiles[i].list();
                    for (int j = 0; j < artworks.length; j++) {
                        String filename = artworks[j].substring(5);
                        bookOrParentedFiles.add(filename);
                    }
                }
            }
        }
        if (null != picSubFiles) {
            for (int i = 0; i < picSubFiles.length; i++) {
                if (picSubFiles[i].isFile()) {
                    String fileName = picSubFiles[i].getName();
                    if (bookOrParentedFiles.contains(fileName)) {
                        System.out.println("发现重复" + fileName);
                        picSubFiles[i].delete();
                    }
                }
            }
        }
        File[] vidSubFiles = new File(vidBasePath).listFiles();
        if (null != vidSubFiles) {
            for (int i = 0; i < vidSubFiles.length; i++) {
                if (vidSubFiles[i].isFile()) {
                    String fileName = vidSubFiles[i].getName();
                    if (bookOrParentedFiles.contains(fileName)) {
                        System.out.println("发现重复" + fileName);
                        vidSubFiles[i].delete();
                    }
                }
            }
        }
    }

    public void updateArtistLevel(DataBaseService dataBaseService) {
        initArtistPathLevel();
        List<LevelInfo> dbLevelInfos = dataBaseService.getLevelInfos();
        dbLevelInfos.forEach((LevelInfo info) -> {
            String artistName = info.name;
            String pathName = transformArtistNameToPath(artistName);
            if (!artistVidLevel.containsKey(pathName) && !artistPicLevel.containsKey(pathName)) {
                // 如果硬盘上没有这个作者了，标记为 is_target = 0
//                logger.info("作者丢失（）：" + artistName);
//                logger.info("作者丢失，但是暂不标记为 丢失，因为出差，可能不连接额外硬盘，这里改的源码");
//                dataBaseService.makeArtistLost(artistName);
            } else {
                LevelInfo diskLevelInfo = new LevelInfo(artistName);
                diskLevelInfo.artistId = info.artistId;
                if (artistPicLevel.containsKey(pathName)) {
                    diskLevelInfo.picLevel = artistPicLevel.get(pathName);
                    diskLevelInfo.picPath = artistPicPath.get(pathName);

                } else {
                    diskLevelInfo.picLevel = 10;
                    diskLevelInfo.picPath = "";
                }
                if (artistVidLevel.containsKey(pathName)) {

                    diskLevelInfo.vidLevel = artistVidLevel.get(pathName);
                    diskLevelInfo.vidPath = artistVidPath.get(pathName);
                } else {
                    diskLevelInfo.vidPath = "";
                    diskLevelInfo.vidLevel = 10;
                }
                if (info.equals(diskLevelInfo)) {

                } else {
                    logger.info("发生变化：" + info.toString());
                    logger.info("发生变化：" + diskLevelInfo.toString());
                    dataBaseService.updateArtistPathLevel(diskLevelInfo);
                }
            }

        });
        try {
            logger.info("更新作者后 Sleep 10 秒");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void extractPathInfo(File[] childrenDir, Map picMap, Map vidMap) {

        for (int i = 0; i < childrenDir.length; i++) {
            String[] artists = childrenDir[i].list();
            String childDirName = childrenDir[i].getName();
            System.out.println(childDirName);
            if (childDirName.startsWith("pic")
                    || childDirName.startsWith("图-")
                    || childDirName.startsWith("T-"))
                picMap.put(PathUtils.buildPath(childrenDir[i].getPath()), Arrays.asList(artists));
            else if (childDirName.startsWith("vid")
                    || childDirName.startsWith("V-")
                    || childDirName.startsWith("视-"))
                vidMap.put(PathUtils.buildPath(childrenDir[i].getPath()), Arrays.asList(artists));
        }
    }

    public String getTempPath() {
        return tempPath;
    }

    public boolean saveFile(File tempFile, ArtworkInfo artworkInfo, String PBPrefix, String fileSaveName) {
        int storePlace = artworkInfo.storePlace;
        String parentPath = getParentPath(artworkInfo, PBPrefix, storePlace);
        try {
            FileUtils.moveFile(tempFile, new File(parentPath + fileSaveName));
            logger.info("文件成功保存到[" + fileSaveName + "]： " + parentPath.replace("//","/"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void copyFileToArtistBookPath(File tempFile, ArtworkInfo artworkInfo) throws IOException {
        String parentPath = getCommonArtistParentPath(artworkInfo.aimName, "1.jpg");
        String bookPath = parentPath + "booked/";
        FileUtils.copyFile(tempFile, new File(bookPath + artworkInfo.fileName));
    }

    public String getParentPath(ArtworkInfo artworkInfo, String PBPrefix, int storePlace) {
        String parentPath;
        if (storePlace == ArtworkInfo.STORE_PLACE.ARTIST.storePlace) {
            parentPath = getCommonArtistParentPath(artworkInfo.aimName, artworkInfo.fileName);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.COPYRIGHT.storePlace) {
            parentPath = getCopyrightParentPath(artworkInfo.aimName);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.STUDIO.storePlace) {
            parentPath = getStudioParentPath(artworkInfo.aimName);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.SINGLE.storePlace) {
            parentPath = getSingleParentPath(artworkInfo);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.ARTIST_PARENT_BOOK.storePlace) {
            parentPath = getArtistParentBookPathParentPath(artworkInfo, PBPrefix);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.SINGLE_PARENT_BOOK.storePlace) {
            parentPath = getSingleParentBookPathParentPath(artworkInfo, PBPrefix);
        } else {
            throw new RuntimeException("未知的Store Place");
        }
        System.out.println("因为parentpath和bookpath也有可能带有作者名（作者名字可能不能作为文件夹名字），所以在getParentPath阶段进行一次识别处理");
        for (String name: namePairs.keySet()
             ) {
            if(parentPath.substring(0,parentPath.lastIndexOf("/")).contains(name)){
                System.out.println("发现需要处理的 name："+name);
                parentPath=parentPath.replace(name,namePairs.get(name));
            }
        }
        return parentPath;
    }

    /**
     * 找到当前Artist作品存放的位置
     */
    public String getArtistArtworkNowPath(ArtworkInfo artworkInfo) {
        logger.info("验证作品在其 作者列表 中的存在情况,返回找到的位置 或 null");
        Set<String> possibleArtist = new HashSet<>();
        List<String> artistList = artworkInfo.getTagArtist();
        possibleArtist.addAll(artistList);
        possibleArtist.add(artworkInfo.aimName);
        logger.info("验证位置:" + possibleArtist);
        for (String name : possibleArtist) {
            String parentPath = getCommonArtistParentPath(name, artworkInfo.fileName);
            if (new File(parentPath + artworkInfo.fileName).exists()) {
                logger.info("文件已发现：" + parentPath + artworkInfo.fileName);
                return parentPath;
            }
        }


        return null;
    }

    private String getSingleParentBookPathParentPath(ArtworkInfo artworkInfo, String params) {
        String copyright = getCopyrightStringForPath(artworkInfo);
        String artistName = "UNKNOWN";
        if (!StringUtils.isEmpty(artworkInfo.aimName)) {
            artistName = artworkInfo.aimName;
        }
        if (null != artworkInfo.getTagArtist() && artworkInfo.getTagArtist().size() > 0) {
            artistName = "";
            List<String> artists = artworkInfo.getTagArtist();
            for (String name :
                    artists) {
                artistName = artistName + name + "&";
            }
            artistName = artistName.substring(0, artistName.length() - 1);
        }

        if (params.equals("B")) {
            return PathUtils.buildPath(spiderSetting.getSingleBookParentBase(), copyright, (params + "[" + artistName + "][" + artworkInfo.bookId + "]" + transformBookNameToPath(artworkInfo.bookName)));
        } else if (params.equals("P")) {
            return PathUtils.buildPath(spiderSetting.getSingleBookParentBase(), copyright, (params + "[" + artworkInfo.aimName + "][" + artworkInfo.parentId + "]"));
        } else {
            throw new RuntimeException("传入了错误的参数，保存BOOK/PARENT的时候，必须提供参数 B/P代表其类型");
        }
    }

    private String getArtistParentBookPathParentPath(ArtworkInfo artworkInfo, String PBPrefix) {
//        logger.info("为了统一管理，不管 视频book 还是 图片book 都放在作者图片文件夹里面 DiskService:334 行");
        String commonArtistParentPath = getCommonArtistParentPath(artworkInfo.aimName, "1.jpg");
        if (PBPrefix.equals("B")) {
            String bookPath = transformBookNameToPath(artworkInfo.bookName);
            return PathUtils.buildPath(commonArtistParentPath, (PBPrefix + "[" + artworkInfo.aimName + "][" + artworkInfo.bookId + "]" + bookPath));
        } else if (PBPrefix.equals("P")) {
            return PathUtils.buildPath(commonArtistParentPath, (PBPrefix + "[" + artworkInfo.aimName + "][" + artworkInfo.parentId) + "]");
        } else {
            throw new RuntimeException("传入了错误的参数，保存BOOK/PARENT的时候，必须提供参数 B/P代表其类型");
        }


    }

    public String transformBookNameToPath(String bookName) {
        // \，/，:，*，?，"，<，>，|
        return bookName
                .replaceAll("\\\\", "_")
                .replaceAll("/", "_")
                .replaceAll(":", "_")
                .replaceAll("\\*", "_")
                .replaceAll("\\?", "_")
                .replaceAll("\\\"", "_")
                .replaceAll("<", "_")
                .replaceAll(">", "_")
                .replaceAll("\\.", "_")
                .replaceAll("\\|", "~")
                .trim();

    }

    private String getSingleParentPath(ArtworkInfo artworkInfo) {
        // base/pic/copyright
        String basePath_1 = spiderSetting.getNormalSingleBase();
        String picvid_2 = PIC;
        if (PathUtils.isVideo(artworkInfo.fileName)) {
            picvid_2 = VID;
        }
        String copyright_3 = transformBookNameToPath(getCopyrightStringForPath(artworkInfo));
        String character_4 = transformBookNameToPath(getCharacterStringForPath(artworkInfo));
        System.out.println("xxxxxxxxxxxxxxxxxxxxxx");
        System.out.println(basePath_1);
        System.out.println(picvid_2);
        System.out.println(copyright_3);
        System.out.println(character_4);
        return PathUtils.buildPath(basePath_1, picvid_2, copyright_3, character_4);
    }

    public static String COPYRIGHT_UNKNOWN = "z_origin";

    public static String CHARACTER_UNKNOWN = "z_unmarked";
    public static String CHARACTER_MULTIPLE = "z_multiple";

    public int getBookStoredNum(ArtworkInfo artworkInfo) {
        String parentPath = getArtistParentBookPathParentPath(artworkInfo, artworkInfo.PBPrefix);
        File file = new File(parentPath);
        if (file.exists()) {
            return file.list().length;
        } else {
            return 0;
        }
    }

    private String getCopyrightStringForPath(ArtworkInfo artworkInfo) {
        List<String> copyrights = artworkInfo.getTagCopyright();
        if (null == copyrights || copyrights.size() == 0) { // 如果没有 设置为 原创
            return COPYRIGHT_UNKNOWN;
        } else if (copyrights.size() == 1) { // 如果有一个 直接返回
            return copyrights.get(0);
        } else { // 如果有多个，先优选一次，然后返回其中最长的
            List<String> cleanedCopyrights = cleanTags(copyrights);
            if (cleanedCopyrights.size() == 1) {
                return cleanedCopyrights.get(0);
            } else {
                String picked = cleanedCopyrights.get(0);
                for (int i = 1; i < cleanedCopyrights.size(); i++) {
                    if (cleanedCopyrights.get(i).length() > picked.length()) {
                        picked = cleanedCopyrights.get(i);
                    }
                }
                return picked;
            }
        }
    }

    private String getCharacterStringForPath(ArtworkInfo artworkInfo) {
        List<String> characters = artworkInfo.getTagCharacter();
        String characterString = CHARACTER_UNKNOWN;
        if (null == characters || characters.size() == 0) {

        } else if (characters.size() == 1) {
            characterString = characters.get(0);
        } else {
            List<String> cleaned = cleanTags(characters);
            if (cleaned.size() > 1) {
                characterString = CHARACTER_MULTIPLE;
            } else {
                characterString = cleaned.get(0);
            }
        }
        return characterString;
    }

    public List<String> cleanTags(List<String> tags) {
        List<String> cleaned = new ArrayList<>();
        cleaned.addAll(tags);
        // 1. 去除带有 特定 tag的，例如 2b 这个tag 但是 至少保留一个
        for (int i = 0; i < spiderSetting.getRemovedTagForPath().length; i++) {
            if (cleaned.size() <= 1) {
                break;
            }
            if (cleaned.size() > 1) {
                if (cleaned.contains(spiderSetting.getRemovedTagForPath()[i])) {
                    cleaned.remove(spiderSetting.getRemovedTagForPath()[i]);
                }
            }
        }
        // 首先去除带有 如果有  abc  和 abc (xxx) 的 去除带abc (series)
        //                     abc  和  abc de     的 去除 abc
        int x = cleaned.size();
        String[] stringTags = new String[x];
        cleaned.toArray(stringTags);

        List<Integer> markDelete = new ArrayList<>();
        for (int i = 0; i < stringTags.length; i++) {
            if (!markDelete.contains(i)) {
                String tagi = stringTags[i];
                for (int j = i + 1; j < stringTags.length; j++) {
                    if (!markDelete.contains(j)) {
                        String tagj = stringTags[j];
                        if (tagi.contains(tagj)) {
                            if (tagi.contains("(")) {
                                markDelete.add(i);
                            } else {
                                markDelete.add(j);
                            }
                        } else if (tagj.contains(tagi)) {
                            if (tagj.contains("(")) {
                                markDelete.add(j);
                            } else {
                                markDelete.add(i);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < markDelete.size(); i++) {
            cleaned.remove(stringTags[markDelete.get(i)]);
        }
        return cleaned;
    }

    private String getStudioParentPath(String studioName) {
        logger.warn("新设计的Studio模式，不再把 图片和 视频区分开了，全都放在一起");
        String studioPathName = transformArtistNameToPath(studioName);
        return PathUtils.buildPath(spiderSetting.getStudioBase(), studioPathName);
    }

    public String getCommonArtistParentPath(String artistName, String artworkName) {
        String artistPathName = transformArtistNameToPath(artistName);
//        if (PathUtils.isVideo(artworkName)) {
//            Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_VIDS.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, List<String>> entry = iterator.next();
//                // 如果这个文件夹下有这个作者名字 ， 作者名字+"."  ,
//                if (entry.getValue().contains(artistPathName) || entry.getValue().contains(artistPathName + "."))
//                    return PathUtils.buildPath(entry.getKey(), artistPathName);
//            }
//            return PathUtils.buildPath(CHAN_ARTIST_DEFAULT_VID, artistPathName);
//        } else {
//            Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_PICS.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<String, List<String>> entry = iterator.next();
//                if (entry.getValue().contains(artistPathName) || entry.getValue().contains(artistPathName + "."))
//                    return PathUtils.buildPath(entry.getKey(), artistPathName);
//            }
//            return PathUtils.buildPath(CHAN_ARTIST_DEFAULT_PIC, artistPathName);
//        }

        /**
         * 原本是 vid pic 分开放，现在一起放了，但是不想改变所有代码，所以 Artist 部分 找一个文件的 parentPath
         * 的时候，不再判断 文件类型， 而是 如果 pic有 返回 pic 如果 vid 有 就返回 vid
         * 如果都没有 返回默认 pic
         * */

        Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_VIDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            // 如果这个文件夹下有这个作者名字 ， 作者名字+"."  ,
            if (entry.getValue().contains(artistPathName) || entry.getValue().contains(artistPathName + "."))
                return PathUtils.buildPath(entry.getKey(), artistPathName);
        }
        Iterator<Map.Entry<String, List<String>>> iterator2 = CHAN_ARTIST_PICS.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator2.next();
            if (entry.getValue().contains(artistPathName) || entry.getValue().contains(artistPathName + "."))
                return PathUtils.buildPath(entry.getKey(), artistPathName);
        }
        return PathUtils.buildPath(CHAN_ARTIST_DEFAULT_PIC, artistPathName);
//            return PathUtils.buildPath(CHAN_ARTIST_DEFAULT_PIC, artistPathName);
    }

    public String transformArtistNameToPath(String name) {
        if (null != namePairs) {
            name = namePairs.containsKey(name) ? namePairs.get(name) : name;
        }
        return name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
    }

    public String transformPathToArtistName(String pathName) {
        AtomicReference<String> name = new AtomicReference<>();
        name.set(pathName);
        if (null != namePairs) {
            if (namePairs.values().contains(pathName)) {
                namePairs.forEach((String realName, String path) -> {
                    if (pathName.equals(path)) {
                        name.set(realName);
                    }
                });
            }
        }
        return name.get();
    }

    public boolean artworkExistOnDisk(ArtworkInfo artworkInfo, String PBPrefix) {
        if (artworkInfo.storePlace == ArtworkInfo.STORE_PLACE.ARTIST.storePlace) {
            Set<String> possibleArtist = new HashSet<>();
            List<String> artistList = artworkInfo.getTagArtist();
            possibleArtist.addAll(artistList);
            possibleArtist.add(artworkInfo.aimName);
            logger.info("验证所有可能的存放位置:" + possibleArtist);
            AtomicBoolean exists = new AtomicBoolean(false);
            for (String name : possibleArtist) {
                String parentPath = getCommonArtistParentPath(name, artworkInfo.fileName);
                if (new File(parentPath + artworkInfo.fileSaveName).exists()) {
                    logger.info("文件已发现：" + parentPath + artworkInfo.fileSaveName);
                    exists.set(true);
                }
            }
            return exists.get();
        } else {
            String parentPath = getParentPath(artworkInfo, PBPrefix, artworkInfo.storePlace);
            boolean exists = new File(parentPath + artworkInfo.fileSaveName).exists();
            if (exists) {
                logger.info("文件已发现：" + parentPath + artworkInfo.fileSaveName);
            }
            return exists;
        }
    }

    public void moveBookedFilesToBooked() {

    }

    public void moveAllFileInBookBackToPic() {
        System.out.println(CHAN_ARTIST_PICS);
        List<String> artists = CHAN_ARTIST_PICS.get("F:/CHAN_ARTIST_BP/T-0-BP/");
        artists.forEach((String name) -> {
            String path = "F:/CHAN_ARTIST_BP/T-0-BP/" + name;
            File artistBase = new File(path);
            File[] artistsssss = artistBase.listFiles();
            for (int i = 0; i < artistsssss.length; i++) {
                File book = artistsssss[i];
                if (book.isDirectory()) {
                    String[] files = book.list();
                    for (int j = 0; j < files.length; j++) {
                        System.out.println(files[j]);
                        System.out.println(path + "/" + book.getName() + "/" + files[j]);
                        System.out.println(path + "/" + files[j].substring(5));
                        File sourceFile = new File(path + "/" + book.getName() + "/" + files[j]);
                        File aimFile = new File(path + "/" + files[j].substring(5));
                        if (aimFile.exists()) {
                            sourceFile.delete();
                        } else {
                            try {
                                FileUtils.moveFile(sourceFile, aimFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
//                        if(new File(path+"/"))
                    }
                }
            }
        });
//        String[] names = base.list();
//        for (int i = 0; i < names.length; i++) {
//            System.out.println(names[i]);
//        }
    }

    public String getRealNameFromPathName(String pathName) {
        AtomicReference<String> realName = new AtomicReference<>(pathName);
        if (namePairs.values().contains(pathName)) {
            namePairs.forEach((String iRealName, String iPathName) -> {
                if (iPathName.equals(pathName)) {
                    realName.set(iRealName);
                }
            });
            return realName.get();
        } else {
            return pathName;
        }
    }

    public void checkBookArtistPath(DataBaseService dataBaseService) {
        Pattern idPattern = Pattern.compile("\\[(\\d+)\\]");
        // 遍历 BP 下面 作者的 下面的 book文件夹 然后找到 B 开头的，然后 分辨出来
        Set<String> artistsBase = CHAN_ARTIST_PICS.keySet();
        artistsBase.forEach((String iArtistBase) -> {
            if (iArtistBase.contains("BP")) {
                System.out.println(iArtistBase);
                File bookArtistBase = new File(iArtistBase);
                String[] artists = bookArtistBase.list();
                for (int i = 0; i < artists.length; i++) {
                    File artist = new File(PathUtils.buildPath(iArtistBase, artists[i]));
                    String realName = getRealNameFromPathName(artists[i]);
                    System.out.println(realName);
                    File[] files = artist.listFiles();
                    for (int j = 0; j < files.length; j++) {
                        File thisFile = files[j];
                        if (thisFile.isDirectory()) {
                            String fullName = thisFile.getName();
                            Matcher m = idPattern.matcher(fullName);
                            if (m.find()) {
                                String bookId = m.group(1);
                                dataBaseService.updateBookArtist(bookId, realName);
                            }

                        }
                    }
                }
            }
        });

    }

    public static void main(String[] args) {
//        DiskService diskService = new DiskService(SpiderSetting.buildSetting());
//        diskService.checkBookArtistPath(new DataBaseService());
//        diskService.moveAllFileInBookBackToPic();
//        diskService.initArtistPathLevel();
//        diskService.updateArtistLevel(new DataBaseService());
        String fullName = "I:/CHAN-ARTIST-特别/T-1-特别-对魔忍风/butcha-u/525048537a0b72107bc722dc9ad13ac1.jpg";
        System.out.println(fullName.substring(fullName.lastIndexOf("/")).contains("_"));
//        Pattern idPattern = Pattern.compile("\\[(\\d+)\\]");
//        Matcher matcher = idPattern.matcher(fullName);
//        if (matcher.find()) {
//
//            System.out.println(matcher.group(1));
//        }
//        String bookId = fullName.substring(fullName.lastIndexOf("[")+1,fullName.lastIndexOf("]"));
//        System.out.println(bookId);
    }

}
