package pers.missionlee.webmagic.spider.sankaku.manager;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.dbbasedsankaku.SankakuDBSourceManager;
import pers.missionlee.webmagic.spider.sankaku.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.AutoDeduplicatedArrayList;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * 本地文件管理器
 * @create: 2019-04-12 08:50
 * <p>
 * 2020-01-11 ： 支持多个路径的Root ， 我的一个移动硬盘只有2t，放不下了
 * <p>
 */
public class SourceManager {
    public SankakuDBSourceManager sankakuDBSourceManager;
    static Logger logger = LoggerFactory.getLogger(SourceManager.class);

    public enum SourceType {
        SANKAKU("SANKAKU"),
        SANKAKU_OFFICIAL("SANKAKU_OFFICIAL"),
        IDOL("IDOL"),
        IDOL_OFFICIAL("IDOL_OFFICIAL");
        String desc;

        SourceType(String desc) {
            this.desc = desc;
        }

    }

    private static final String DIR_SANKAKU = "sankaku";
    private static final String DIR_IDOL = "idol";
    private static final String DIR_TMP = "tmp";
    private static final String DIR_PIC = "pic";
    private static final String DIR_VID = "vid";
    private static final String DIR_INFO = "info";
    @Deprecated
    private static final String FILE_SUFFIX_ARTWORK = ".jsonline";
    @Deprecated
    private static final String FILE_SUFFIX_ARTIST = ".json";
    private static final String[] ENPTY_STRING_ARRAY = new String[0];
    // 更新过期时间 1天
    private static long ONE_DAY_TIME_MILLIS = 7 * 24 * 60 * 60 * 1000L;


    private String rootPath;
    private String[] additionalRootPath;
    // sankaku
    private String sankakuRootPath;
    private String sankakuDefaultPicPath;
    private String sankakuDefaultVidPath;
    private String sankakuDefaultInfoPath;
    private PathList sankakuDefaultPathList;
    private File sankakuDefaultPics;
    private File sankakuDefaultVids;
    private Map<String, Long> sankakuUpdateInfo;
    // key 为 路径， list 存放特别存放的作者名称
    // 例如 D:\ROOT\sankaku\pic-top\  对应list 存放这个目录下面作者(文件夹的名称)
    private Map<String, List<String>> specialSelectedPicArtistPathMap;
    private Map<String, List<String>> specialSelectedVidArtistPathMap;
    // idol
    private String idolRootPath;
    private String idolPicPath;
    private String idolVidPath;
    private String idolInfoPath;
    private PathList idolPathList;
    // tmp
    public String tmpPath;
    // 初始化时 分析出以下内容
    private Map<String, Integer> sankakuArtists;
    private Map<String, Integer> idolArtists;
    // TODO: 2019-04-13 official 部分暂不支持
    private Map<String, Integer> sankakuOfficaialArtists;
    private Map<String, Integer> idolOfficialArtists;

    FileFilter artistInfoFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(FILE_SUFFIX_ARTIST);
        }
    };
    FilenameFilter jsonFilenameFiler = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".json");
        }
    };
    FileFilter selectedFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().startsWith("pic-")
                    || pathname.getName().startsWith("vid-")
                    || pathname.getName().startsWith("图-")
                    || pathname.getName().startsWith("视-")
                    || pathname.getName().startsWith("T-")
                    || pathname.getName().startsWith("V-")
                    || pathname.getName().startsWith("pic")
                    || pathname.getName().startsWith("vid");
        }
    };
    FilenameFilter jsonlineFilenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".jsonline");
        }
    };

    @Deprecated
    public SourceManager(String rootPath) {
         sankakuDBSourceManager = new SankakuDBSourceManager(this);
        if (!rootPath.toLowerCase().contains("root")) {
            throw new RuntimeException("约定根目录名称为某个目录下的 root/ROOT 目录，请检查");
        }
        logger.warn("当前SourceManager仅针对Sankaku功能进行了实现");
        this.rootPath = formatPath(rootPath);
        this.tmpPath = buildPath(rootPath, DIR_TMP);
        initSankakuPathInfo();

    }

    public SourceManager(String rootPath, String... additionalRootPath) {
        this(rootPath);
        initAdditionPathInfo(additionalRootPath);

        StringBuffer buffer = new StringBuffer();
        buffer.append("SANKAKU 根路径：" + sankakuRootPath + "\n");
        buffer.append("SANKAKU 信息路径：" + sankakuDefaultInfoPath + "\n");
        buffer.append("SANKAKU 图片路径：" + sankakuDefaultPicPath + "\n");
        buffer.append("SANKAKU 视频路径：" + sankakuDefaultVidPath + "\n");
        logger.debug("\n" + buffer.toString());
        logger.info(specialSelectedPicArtistPathMap.toString());
        logger.info(specialSelectedVidArtistPathMap.toString());
        logger.info("初始化完成");
    }

    public boolean includedUnderArtist(String url, String artistName) {
        String sanCode = url.substring(url.lastIndexOf("/") + 1);
        List<Map<String, Object>> artists = sankakuDBSourceManager.getArtistsOfArtworkSanCode(sanCode);
        if (artists != null && artists.size() > 0) {
            for (int j = 0; j < artists.size(); j++) {
                Map<String, Object> art = artists.get(i);
                if (art.get("name").toString().equals(artistName)) return true;
            }
            return false;
        } else {
            return false;
        }
    }

    public boolean included(String url) {
        String sanCode = url.substring(url.lastIndexOf("/") + 1);
        List<Map<String, Object>> artists = sankakuDBSourceManager.getArtistsOfArtworkSanCode(sanCode);
        if (artists != null && artists.size() > 0) {
            logger.debug("作品已收录：" + sanCode);
            return true;
        } else {
            logger.debug("作品未收录：" + sanCode);
            return false;
        }
    }

    public int getArtworkNumOfDB(String artistName) {
        return sankakuDBSourceManager.getArtistWorkNum(artistName);
    }

    /**
     * SourceManager初始化后，生成（sankaku）default路径，并分析文件结构，得到实施的文件结构
     */
    private void initSankakuPathInfo() {

        this.sankakuRootPath = buildPath(rootPath, DIR_SANKAKU);
        this.sankakuDefaultInfoPath = buildPath(sankakuRootPath, DIR_INFO);
        this.sankakuDefaultPicPath = buildPath(sankakuRootPath + DIR_PIC);
        this.sankakuDefaultVidPath = buildPath(sankakuRootPath + DIR_VID);
        this.sankakuDefaultPics = new File(sankakuDefaultPicPath);
        this.sankakuDefaultVids = new File(sankakuDefaultVidPath);
        this.sankakuDefaultPathList = new PathList(sankakuDefaultPicPath, sankakuDefaultVidPath, sankakuDefaultInfoPath);
        File[] files = new File(sankakuRootPath).listFiles(selectedFileFilter); // TODO: 2019-04-28
        Map<String, List<String>> specialPicList = new HashMap<String, List<String>>();
        Map<String, List<String>> specialVidList = new HashMap<String, List<String>>();
        for (int i = 0; i < files.length; i++) {
            logger.info("初始化：" + files[i].getName());
            String[] artist = files[i].list();
            if (files[i].getName().startsWith("pic-")
                    || files[i].getName().startsWith("图-")
                    || files[i].getName().startsWith("T-")
                    || files[i].getName().startsWith("pic")
            )
                specialPicList.put(buildPath(files[i].getPath()), Arrays.asList(artist));

            else if (files[i].getName().startsWith("vid-")
                    || files[i].getName().startsWith("视-")
                    || files[i].getName().startsWith("V-")
                    || files[i].getName().startsWith("vid")
            )
                specialVidList.put(buildPath(files[i].getPath()), Arrays.asList(artist));
        }
        this.specialSelectedPicArtistPathMap = specialPicList;
        this.specialSelectedVidArtistPathMap = specialVidList;

    }

    private void initAdditionPathInfo(String[] additionalRootPath) {
        this.additionalRootPath = additionalRootPath;
        for (int i = 0; i < additionalRootPath.length; i++) {
            String adpath = buildPath(additionalRootPath[i], DIR_SANKAKU);
            System.out.println(adpath);
            File[] files = new File(adpath).listFiles(selectedFileFilter);
            System.out.println(files);
            for (int j = 0; j < files.length; j++) {
                logger.info("初始化：" + files[j].getName());
                String[] artist = files[j].list();
                if (files[j].getName().startsWith("pic-")
                        || files[j].getName().startsWith("图-")
                        || files[j].getName().startsWith("T-")
                        || files[j].getName().startsWith("pic")
                )
                    this.specialSelectedPicArtistPathMap.put(buildPath(files[j].getPath()), Arrays.asList(artist));

                else if (files[j].getName().startsWith("vid-")
                        || files[j].getName().startsWith("视-")
                        || files[j].getName().startsWith("V-")
                        || files[j].getName().startsWith("vid")
                )
                    specialSelectedVidArtistPathMap.put(buildPath(files[j].getPath()), Arrays.asList(artist));

            }
        }

    }

    @Deprecated
    private void initIdolPathInfo() {
        this.idolRootPath = buildPath(rootPath + DIR_IDOL);
        this.idolInfoPath = buildPath(idolRootPath, DIR_INFO);
        this.idolPicPath = buildPath(idolRootPath, DIR_PIC);
        this.idolVidPath = buildPath(idolRootPath, DIR_VID);
        this.idolPathList = new PathList(idolPicPath, idolVidPath, idolInfoPath);
    }

    /**
     * 增加对 分级目录的兼容
     */
    @Deprecated
    public Map<String, Integer> getSankakuArtistsListByDir() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        Set<String> picKey = specialSelectedPicArtistPathMap.keySet();
        for (String key : picKey
        ) {
            List<String> artists = specialSelectedPicArtistPathMap.get(key);
            for (String name : artists) {
                map.put(name, 1);
            }
        }
        Set<String> vidKey = specialSelectedVidArtistPathMap.keySet();
        for (String key :
                vidKey) {
            List<String> artists = specialSelectedVidArtistPathMap.get(key);
            for (String name :
                    artists) {
                map.put(name, 1);
            }
        }
        return map;
    }

    @Deprecated
    public Map<String, Integer> getSankakuArtistListByJson() {
        if (true)
            throw new RuntimeException("Deprecated");
//        // TODO: 2019-07-25 数据没有迁移完成，可以考虑两者合一？
        if (true)
            return sankakuDBSourceManager.getArtists();
        Map themap = getArtistListByJson(SourceType.SANKAKU);
        return themap;
    }

    private class PathList {
        String PicPath;
        String VidPath;
        String InfoPath;

        public PathList(String picPath, String vidPath, String infoPath) {
            PicPath = picPath;
            VidPath = vidPath;
            InfoPath = infoPath;
        }
    }

    private PathList getPathList(SourceType sourceType) {
        if (sourceType == SourceType.SANKAKU) {
            return sankakuDefaultPathList;
        } else {
            return idolPathList;
        }
    }

    /**
     * 两个工具方法
     */
    private static String formatPath(String rootPath) {
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        return rootPath;
    }

    public static String buildPath(String... paths) {
        String aimPath = "";
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].startsWith("/"))
                paths[i] = paths[i].substring(1);
            aimPath += formatPath(paths[i]);
        }
        return aimPath.replaceAll("\\\\", "/");
    }

    /**
     * 用于去除 作者路径名中的 空格和最后一个 .
     * 理论上传进来的路径是正确的，此处多一重保证
     */
    private static String cleanArtistFileName(String artistFileName) {
        return artistFileName;
//        artistFileName = artistFileName.trim();
//        return artistFileName.endsWith(".") ? artistFileName.substring(0, artistFileName.length() - 1) : artistFileName;
    }

    @Deprecated
    public static boolean isMetaFile(String fileName) {
        return fileName.toLowerCase().endsWith(".json") || fileName.toLowerCase().endsWith(".jsonline");
    }

    public static boolean isPicture(String fileName) {
        String nameLow = fileName.toLowerCase();
        if (nameLow.endsWith("jpg")
                || nameLow.endsWith("jpeg")
                || nameLow.endsWith("png")
                || nameLow.endsWith("fig")
                || nameLow.endsWith("webp")
                || nameLow.endsWith("fpx")
                || nameLow.endsWith("svg")
                || nameLow.endsWith("bmp")
        )
            return true;
        return false;
    }

    public static boolean isVideo(String fileName) {
        String nameLow = fileName.toLowerCase();
        if (nameLow.endsWith(".mp4")
                || nameLow.endsWith(".webm")
                || nameLow.endsWith(".avi")
                || nameLow.endsWith(".rmvb")
                || nameLow.endsWith(".flv")
                || nameLow.endsWith(".3gp")
                || nameLow.endsWith(".mov")
                || nameLow.endsWith(".swf")) {
            return true;
        }
        return false;
    }

    /**
     * 作品信息：读取本地文件，返回排重，排错后的作品信息列表
     *
     * @Deprecated 数据全都放在了数据库里面
     */
    @Deprecated
    public <T extends List<ArtworkInfo>> T getArtworkOfArtist(SourceType sourceType, String artistFileName) throws IOException {

        artistFileName = cleanArtistFileName(artistFileName);
        PathList pathList = getPathList(sourceType);
        T artworkInfoList = (T) new AutoDeduplicatedArrayList();
        File artworkInfoFile = new File(pathList.InfoPath + artistFileName + FILE_SUFFIX_ARTWORK);
        if (artworkInfoFile.exists()) {
            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "utf8");
            String[] artworkInfoStringArray = artworkInfos.split("\n");
            ArtworkInfo.convertArtworkStringToList(artworkInfoList, artworkInfoStringArray);
//            logger.info("以名称排重后剩余数量：" + artworkInfoList.size());
            // 处理 json记录与实际文件不符合的内容
//            File pics = new File(getArtistPath(sourceType, ".jpg", artistFileName));
//            File vids = new File(getArtistPath(sourceType, ".mp4", artistFileName));
//            //File pics = new File(pathList.PicPath + artistFileName);
//            //File vids = new File(pathList.VidPath + artistFileName);
//            String[] picNames = pics.list();
//            String[] vidNames = vids.list();
//            List<String> artworks = new ArrayList<String>();
//            int removed = 0;
//            if (picNames != null) {
//                for (int i = 0; i < picNames.length; i++) {
//                    artworks.add(picNames[i]);
//                }
//                logger.info("实际图片作品数量：" + picNames.length);
//            }
//
//
//            if (vidNames != null) {
//                for (int i = 0; i < vidNames.length; i++) {
//                    artworks.add(vidNames[i]);
//                }
//                logger.info("实际视频作品数量：" + vidNames.length);
//            }
//
//            Iterator iterator = artworkInfoList.iterator();
//            while (iterator.hasNext()) {
//                if (!artworks.contains(((ArtworkInfo) iterator.next()).getName())) {
//                    // TODO: 2019-07-25 remove 机制暂时取消
//                    System.out.println("remove 机制暂时取消掉了");
////                    iterator.remove();
////                    removed++;
//                }
//            }
//            if (removed > 0) {
//                logger.info("因为实际作品缺失，删除文本记录：" + removed);
//                // TODO: 2019-07-25 remove 机制暂时取消
//                // rebuildArtworkInfoFile(sourceType, artistFileName, artworkInfoList);
//            }
        }
        logger.debug(artistFileName + " 作品：" + artworkInfoList);
        return artworkInfoList;
    }

    /**
     * @Deprecated 因为单个作品在多个作者名下，这个方法没有实际意义，从数据库解析更准确
     */
    @Deprecated
    public Map<String, Integer> getRealAartworkNum(SourceType sourceType, String artist) {
        artist = cleanArtistFileName(artist);
        Map<String, Integer> numMap = new HashMap<>();
        numMap.put("saved_pic_num", 0);
        numMap.put("saved_vid_num", 0);

        if (sourceType == SourceType.SANKAKU) {
            File pics = new File(getArtistPath(sourceType, ".jpg", artist));
            File vids = new File(getArtistPath(sourceType, ".mp4", artist));
            if (pics.list() != null)
                numMap.put("saved_pic_num", pics.list().length);
            if (vids.list() != null)
                numMap.put("saved_vid_num", vids.list().length);
        }
        return numMap;
    }

    /**
     * 作品信息：清空原本的作品信息文件，写入新的内容
     */
    @Deprecated
    private void rebuildArtworkInfoFile(SourceType sourceType, String artistFileName, List<ArtworkInfo> artworkInfos) throws IOException {
        artistFileName = cleanArtistFileName(artistFileName);
        PathList pathList = getPathList(sourceType);
        File artworkInfoFile = new File(pathList.InfoPath + artistFileName + FILE_SUFFIX_ARTWORK);
        FileUtils.writeStringToFile(artworkInfoFile, "", "utf8", false);
        Iterator iterator = artworkInfos.iterator();
        while (iterator.hasNext()) {
            recordArtworkInfo(artworkInfoFile, (ArtworkInfo) iterator.next());
        }
        logger.debug("重写[" + artistFileName + "]作品信息：" + artworkInfos);
    }

    /**
     * 作品信息：向作品信息文件追加一条新的作品信息
     * <p>
     * ！important  原名appendArtworkInfoToFile，现在不再旺文本文件写入数据，改名
     */
    public synchronized void recordArtworkInfo(SourceType sourceType, String artistFileName, ArtworkInfo artworkInfo, String artistName) throws IOException {
        sankakuDBSourceManager.addArtworkInfo(artistName, artworkInfo);
        logger.debug("追加作品信息[" + artistFileName + "]:" + artworkInfo);
        // artistFileName = cleanArtistFileName(artistFileName);
        // PathList pathList = getPathList(sourceType);
        // File artworkInfoFile = new File(pathList.InfoPath + artistFileName + FILE_SUFFIX_ARTWORK);
        // FileUtils.writeStringToFile(artworkInfoFile, artworkInfo.toString() + "\n", "utf8", true);
    }

    /**
     * 作品信息：向作品信息文件追加一条新的作品信息
     */
    @Deprecated
    private synchronized void recordArtworkInfo(File artworkInfoFile, ArtworkInfo artworkInfo) throws IOException {
        FileUtils.writeStringToFile(artworkInfoFile, artworkInfo.toString() + "\n", "utf8", true);
    }

    /**
     * 作品信息：获取作者的作品数量
     */
    public int getArtworkNum(SourceType sourceType, String artistFileName) {
        System.out.println("从本地目录获取已收录作品数量");
        artistFileName = cleanArtistFileName(artistFileName);

        int num = 0;
        //PathList pathList = getPathList(sourceType);
        File pics = new File(getArtistPath(sourceType, "1.jpg", artistFileName));
        File vids = new File(getArtistPath(sourceType, "1.mp4", artistFileName));
        if (pics.exists() && pics.listFiles() != null)
            num += pics.listFiles().length;
        if (vids.exists() && vids.listFiles() != null)
            num += vids.listFiles().length;
        return num;
    }
    /**
     * 作者/作品信息： 该作者
     * */
    /**
     * 作者信息：获取作者超简单信息列表
     */
    public Map<String, Integer> getArtistListByJson(SourceType sourceType) {
        if (sourceType == SourceType.SANKAKU) {

//            String[] infos = new File(sankakuDefaultInfoPath).list(jsonFilenameFiler);
            String[] infos = new File(sankakuDefaultInfoPath).list(jsonlineFilenameFilter);
            System.out.println(infos.length);
            Map<String, Integer> artists = new HashMap<String, Integer>();
            for (int i = 0; i < infos.length; i++) {
                artists.put(infos[i].substring(0, infos[i].length() - ".jsonline".length()), 1);
            }
            return artists;
        }
        return null;
    }

    /**
     * 作者信息：如果 作者.json / .jsonline文件不存在 创建这个文件
     * ！important
     */
    public boolean guaranteeArtistInfoFileExists(SourceType sourceType, String artistFileName) throws IOException {
        // TODO: 2019-12-29
        System.out.println("新版本完全依赖于数据库，不再创建本地文件");
        return true;
        // artistFileName = cleanArtistFileName(artistFileName);
        // PathList pathList = getPathList(sourceType);
        // File artistInfoFile = new File(pathList.InfoPath + artistFileName + FILE_SUFFIX_ARTIST);
        // if (!artistInfoFile.exists()) {
        //     System.out.println("create:" + artistInfoFile.getPath() + " | " + artistInfoFile.getName());
        //     return artistInfoFile.createNewFile();
        // } else {
        //     return true;
        // }
    }

    /**
     * 作者信息：从作品信息提取作者信息
     */
    public static ArtistInfo getArtistInfoFromArtwork(List<ArtworkInfo> artworkInfos, String artistFileName) {
        artistFileName = cleanArtistFileName(artistFileName);
        return ArtistInfo.getArtistInfoFromArtwork(artworkInfos, artistFileName);
    }

    /**
     * 作者信息：作者信息落盘
     */
    public ArtistInfo writeArtistInfo(SourceType sourceType, ArtistInfo artistInfo) throws IOException {
        PathList pathList = getPathList(sourceType);
        File artistInfoFile = new File(pathList.InfoPath + artistInfo.getName() + FILE_SUFFIX_ARTIST);
        FileUtils.writeStringToFile(artistInfoFile, artistInfo.toString(), "utf8", false);
        return artistInfo;
    }

    public ArtistInfo writeArtistInfo(SourceType sourceType, List<ArtworkInfo> artworkInfos, String artistFileName) throws IOException {
        artistFileName = cleanArtistFileName(artistFileName);
        ArtistInfo artistInfo = getArtistInfoFromArtwork(artworkInfos, artistFileName);
        return writeArtistInfo(sourceType, artistInfo);
    }

    /**
     * 文件存储：用于讲下载成功的临时文件存入目标位置
     */
    public Boolean saveFile(SourceType sourceType, File tmpFile, String artistFileName, String artworkName) {
        artistFileName = cleanArtistFileName(artistFileName);
        String fullPath = getArtistPath(sourceType, artworkName, artistFileName);
        try {
            FileUtils.moveFile(tmpFile, new File(fullPath + artworkName));
            logger.debug("文件转存：" + fullPath + artworkName);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @Description 从作品找作者，使用的时候 传入个 .mp4  .jpg 这样的字符串，就能用于定位作者位置
     * @Updated 2020-01-11 支持多ROOT存储之后，对方法进行了适配
     */
    public String getArtistPath(SourceType sourceType, String artworkName, String artistFileName) {
        artistFileName = cleanArtistFileName(artistFileName); // 作者名字最后可能带着一个  .  但是路径里面没有
        String parentPath = "";
        PathList pathList = getPathList(sourceType);
        Iterator<Map.Entry<String, List<String>>> iterator = specialSelectedVidArtistPathMap.entrySet().iterator();
        if (isVideo(artworkName)) { //
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entry = iterator.next();
                if (entry.getValue().contains(artistFileName)) {
                    return buildPath(entry.getKey(), artistFileName);
                }
            }
            parentPath = pathList.VidPath;
        } else {
            iterator = specialSelectedPicArtistPathMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entry = iterator.next();
                if (entry.getValue().contains(artistFileName)) {
                    return buildPath(entry.getKey(), artistFileName);
                }
            }
            parentPath = pathList.PicPath;
        }
        return buildPath(parentPath, artistFileName);
    }

    /**
     * 文件存储：判断文件是否存在
     * <p>
     * 更新：文件也可能存在于其他作者（所以这里用数据库判断一下重名情况）
     *
     * @Update 2020-01-11 此方法以数据库存在为准，
     */
    public boolean existInDB(SourceType sourceType, String artistFileName, String artworkName) {
        return sankakuDBSourceManager.exists(artworkName);
    }

    public boolean existsInDisk(SourceType sourceType, String artistFileName, String artworkName) {
        artistFileName = cleanArtistFileName(artistFileName);
        String fullPath = getArtistPath(sourceType, artworkName, artistFileName);
        return new File(fullPath + artworkName).exists();
    }
//    /**
//     * 文件存储： 返回文件路径
//     * */
//    public String getPathOfArtwork(SourceType sourceType,String artistFileName,String artworkName){
//        artistFileName = cleanArtistFileName(artistFileName);
//        String fullPath = getArtistPath(sourceType, artworkName, artistFileName);
//        // TODO: 2019-07-25
//    }

    /**
     * 文件存储：返回临时路径文件
     */
    public File getFileInTmpPath(String filename) {
        return new File(tmpPath + filename);
    }

    /**
     * 更新控制：查询知否需要更新 1 表示已经更新
     *
     * @update 2020-01-11 已经完全交给数据库控制
     */

    int i = 0;

    public boolean isUpdated(SourceType sourceType, String artistName, int minPriority) throws IOException {
        int priority = getPriorityFromPathName(sourceType, artistName);
        logger.warn("作者：" + artistName + " 优先级：" + priority);
        if (priority > minPriority)
            return true;
        if (sourceType == SourceType.SANKAKU) {
            if (sankakuUpdateInfo == null) {
                initUpdateInfo(SourceType.SANKAKU);
            }
            if (sankakuUpdateInfo.containsKey(artistName)) {
                return System.currentTimeMillis() < sankakuUpdateInfo.get(artistName);
            } else return false;
        } else {
            throw new RuntimeException("目前仅支持更新 SANKAKU");
        }
    }

    /**
     * 更新控制：更新
     *
     * @Update 基于数据库字段控制更新，不再依赖本地文件
     */
    public void update(SourceType sourceType, String artistName, String artistFileName, int updatedNum) throws IOException {
        boolean doUpdate = true;
        int priority = getPriorityFromPathName(sourceType, artistFileName);
        int times = 30;
        if (priority == 5) {
            doUpdate = false;
//            times = 36000; // 5表示废弃 用不更新
        } else if (priority == 4) {
            times = 360;
        } else if (priority == 3) {
            times = 180;
        } else if (priority == 2) {
            times = 90;
        } else if (priority == 1 && updatedNum > 0) {
            times = 45;

        } else if (priority == 0) {
            if (updatedNum == 0)
                times = 30;
            else times = 15;
        }
        if (doUpdate && sourceType == SourceType.SANKAKU) {
            long aimUpdateTime = System.currentTimeMillis() + times * ONE_DAY_TIME_MILLIS;
            // 设定下次更新的时间
            // sankakuUpdateInfo.put(artistName, aimUpdateTime);
            // FileUtils.writeStringToFile(new File(sankakuDefaultInfoPath + UPDATE_INFO_FILE_NAME),
            //         JSON.toJSONString(sankakuUpdateInfo), "utf8", false);
            sankakuDBSourceManager.updateArtist(artistName, aimUpdateTime / 1000);
            logger.info("更新完毕：" + artistName + "\t下次更新：" + times + "天 不再更新文本记录，只更新数据库记录");
        } else {
            logger.warn("未做更近记录 ##########");
        }
    }

    static Pattern priorityPattern = Pattern.compile("-([0-9])-");

    /**
     * 数据库中，更新作者优先级是手动控制的，可能不及时，所以从文本文件还是可以的
     */
    public int getPriorityFromPathName(SourceType sourceType, String artistFileName) {
        int priority = 10;
        String parentPathPic = getArtistPath(sourceType, ".jpg", artistFileName);
        String parentPathVid = getArtistPath(sourceType, ".mp4", artistFileName);
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

    public Map<String, Integer> getPriority(SourceType sourceType, String artistFileName) {
        Map<String, Integer> priMap = new HashMap<>();
        String parentPathPic = getArtistPath(sourceType, ".jpg", artistFileName);
        String parentPathVid = getArtistPath(sourceType, ".mp4", artistFileName);
        Matcher matcherPic = priorityPattern.matcher(parentPathPic);
        Matcher matcherVid = priorityPattern.matcher(parentPathVid);
        priMap.put("pic_level", 10);
        priMap.put("vid_level", 10);
        if (matcherPic.find()) {
            priMap.put("pic_level", Integer.valueOf(matcherPic.group(1)));
        }
        if (matcherVid.find()) {
            priMap.put("vid_level", Integer.valueOf(matcherVid.group(1)));
        }
        return priMap;
    }

    private static final String UPDATE_INFO_FILE_NAME = "update_info.json";

    /**
     * 更新控制：初始化更新信息
     */
    private void initUpdateInfo(SourceType sourceType) throws IOException {

        logger.debug("获取[" + sourceType.desc + "]更新信息存档");
        if (sourceType == SourceType.SANKAKU) {
            // TODO: 2019-12-27 这里是从文本文件载入更新信息
//            File updateInfoFile = new File(sankakuDefaultInfoPath + UPDATE_INFO_FILE_NAME);
//            if (updateInfoFile.exists()) {
//                String jsonInfo = FileUtils.readFileToString(updateInfoFile, "utf8");
//                sankakuUpdateInfo = (Map<String, Long>) JSON.parse(jsonInfo);
//                if (sankakuUpdateInfo == null) {
//                    sankakuUpdateInfo = new HashMap<String, Long>();
//                }
//            } else {
//                sankakuUpdateInfo = new HashMap<String, Long>();
//            }
            // TODO: 2019-12-27 这里是从数据库载入更新信息
            logger.debug("获取数据库中的更新时间记录");
            Map<String, Long> infos = sankakuDBSourceManager.getUpdateInfo();
            if (infos != null && infos.size() > 0)
                sankakuUpdateInfo = infos;
            else
                sankakuUpdateInfo = new HashMap<>();
        } else {
            throw new RuntimeException("目前仅支持更新 SANKAKU");
        }
    }

    /**
     * 用于把旧版本的文件组织形式变为新版本的形式，当所有文件转移成功后，这份方法就不再有用
     */
    @Deprecated
    public static void convertOldSourceFileStructureToNewOne() throws IOException {
        if (true)
            throw new RuntimeException("必须看好新旧目录，目前仅支持 sankaku");
        SourceManager sourceManager = new SourceManager("D:\\ROOT");

        File oldSanRootFile = new File("D:\\sankaku");
        System.out.println(oldSanRootFile.getName());
        FileFilter dirFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        System.out.println(sourceManager.sankakuDefaultInfoPath);
        File[] files = oldSanRootFile.listFiles(dirFilter);
        File infoDir = new File(sourceManager.sankakuDefaultInfoPath);
        for (int i = 0; i < files.length; i++) {
            File[] cFiles = files[i].listFiles(dirFilter);
            String artistFileName = files[i].getName();
            System.out.println("artistFileName:" + artistFileName + " " + cFiles.length);
            File artistPicFile = new File(sourceManager.sankakuDefaultPicPath + artistFileName + "/");
            File artistVidFile = new File(sourceManager.sankakuDefaultVidPath + artistFileName + "/");
            System.out.println(artistVidFile.getPath());
            for (int j = 0; j < cFiles.length; j++) {

                // info vid pic 分别转移
                String name = cFiles[j].getName();
                System.out.println(name);
                if (name.contains("info")) {
                    File[] jsonFiles = cFiles[j].listFiles();
                    for (int k = 0; k < jsonFiles.length; k++) {
                        if (jsonFiles[k].getName().endsWith("json")) {
                            File renamed = new File(jsonFiles[k].getParent() + "/" + artistFileName + ".json");
                            if (jsonFiles[k].renameTo(renamed)) {
                                if (!new File(infoDir.getPath() + "/" + renamed.getName()).exists())

                                    FileUtils.moveFileToDirectory(renamed, infoDir, true);
                            }
                        } else if (jsonFiles[k].getName().endsWith("jsonline")) {
                            File renamed = new File(jsonFiles[k].getParent() + "/" + artistFileName + ".jsonline");
                            if (jsonFiles[k].renameTo(renamed))
                                if (!new File(infoDir.getPath() + "/" + renamed.getName()).exists())

                                    FileUtils.moveFileToDirectory(renamed, infoDir, true);
                        }
                    }
                } else if (name.contains("pic")) {
                    File[] pics = cFiles[j].listFiles();
                    System.out.println(pics.length);
                    for (int k = 0; k < pics.length; k++) {
                        if (!new File(artistPicFile.getPath() + "/" + pics[k].getName()).exists())
                            FileUtils.moveFileToDirectory(pics[k], artistPicFile, true);
                        // pics[k].renameTo(new File(sourceManager.sankakuDefaultPicPath+artistFileName+"/"+pics[k].getName()));
                    }
                } else if (name.contains("vid")) {
                    File[] vids = cFiles[j].listFiles();
                    System.out.println(vids.length);
                    for (int k = 0; k < vids.length; k++) {
                        if (!new File(artistVidFile.getPath() + "/" + vids[k].getName()).exists())

                            FileUtils.moveFileToDirectory(vids[k], artistVidFile, true);
//                        vids[k].renameTo(new File(sourceManager.sankakuDefaultVidPath+artistFileName+"/"+vids[k].getName()));
                    }
                }
            }
        }
    }

    @Deprecated
    public void cleanInfo() throws IOException {
        // TODO: 2019-12-29 早期版本遗留问题处理，使用过后已经没有价值了 
        ChromeBookmarksReader reader = new ChromeBookmarksReader(ChromeBookmarksReader.defaultBookmarkpath);
        List<Map> maps = reader.getBookMarkListByDirName("download1");
        List<Map> maps1 = reader.getBookMarkListByDirName("download2");
        List<String> namelist = new ArrayList<String>();
        System.out.println("download1: " + maps.size()); // 2653
        for (Map bookmark :
                maps) {
            String tagName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
            if (tagName.equals("ky.")) {
                System.out.println(tagName);
            }
            guaranteeArtistInfoFileExists(SourceType.SANKAKU, tagName);
            namelist.add(tagName);
        }
        Map<String, Integer> dirfile = getSankakuArtistsListByDir();
        System.out.println("dirfile: " + dirfile.size());
        Map<String, Integer> jsonfile = getSankakuArtistListByJson();
        System.out.println("jsonfile: " + jsonfile.size());
        Iterator iterator = jsonfile.keySet().iterator();
        //
        int i = 0;

        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            i++;
            if (!namelist.contains(name)) {
                System.out.println(name);
                System.out.println(i);
            }

        }

    }

    public void findDuplicationDir() {
        Map<String, String> tmpList = new HashMap<String, String>();
        Iterator<String> iterator = specialSelectedPicArtistPathMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            logger.info(key);
            List<String> namelist = specialSelectedPicArtistPathMap.get(key);
            for (String name :
                    namelist) {
                if (!tmpList.containsKey(name)) {
                    tmpList.put(name, key);
                } else {
                    logger.info("============== " + name + " | " + tmpList.get(name) + " | " + key);
                }
            }
        }


        Map<String, String> tmpList2 = new HashMap<String, String>();
        Iterator<String> iterator2 = specialSelectedVidArtistPathMap.keySet().iterator();
        while (iterator2.hasNext()) {
            String key = iterator2.next();
            logger.info(key);
            List<String> namelist2 = specialSelectedVidArtistPathMap.get(key);
            for (String name :
                    namelist2) {
                if (!tmpList2.containsKey(name)) {
                    tmpList2.put(name, key);
                } else {
                    logger.info("============== " + name + " | " + tmpList2.get(name) + " | " + key);
                }
            }
        }
    }

    public void dealWithVidArtistWithSamllNumberOfVid() throws IOException {
        File vid01234 = new File(sankakuRootPath + "vid-01234/");
        if (!vid01234.exists()) vid01234.mkdir();
        File[] defaultFiles = sankakuDefaultVids.listFiles();
        // 寻找 01234 目录里面作品数量超过4个的 把他们移动到默认vid
        List<String> nameLow = specialSelectedVidArtistPathMap.get(sankakuRootPath + "vid-01234/");
        if (nameLow != null) {
            for (String name :
                    nameLow) {
                File artist = new File(sankakuRootPath + "vid-01234/" + name);
                if (artist.list().length > 3) {
                    logger.info("放回默认目录：" + name);
                    FileUtils.moveDirectoryToDirectory(artist, sankakuDefaultVids, false);
                }

            }
        }

        // 遍历 默认vid把 少于4给的放到 01234目录
        for (int i = 0; i < defaultFiles.length; i++) {
            if (defaultFiles[i].list().length < 4) {
                logger.info("放入1234目录：" + defaultFiles[i].getName());
                FileUtils.moveDirectoryToDirectory(defaultFiles[i], vid01234, false);
            }
        }
    }

    /**
     * 删除目录中存在的临时文件
     *
     * @Deprecated 现在所有临时文件都在tmp目录中，这个方法用于处理早期版本造成的临时文件遗留问题
     **/
    @Deprecated
    public void cleanRootDir() {
        File root = new File(rootPath);
        doRecusiveClean(root);
    }
    @Deprecated
    private void doRecusiveClean(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    doRecusiveClean(files[i]);
                    files[i] = null;
                } else if (files[i].isFile()) {
                    if (files[i].getName().contains("-")
                            && files[i].getName().length() == 36
                            && !isVideo(files[i].getName())
                            && !isPicture(files[i].getName())
                            && !isMetaFile(files[i].getName())
                    ) {
                        logger.info("删除：" + files[i].getPath());
                        files[i].delete();
                        files[i] = null;
                    }
                }
            }
            dir = null;
        }


    }

    /**
     * 给定 artistFileName 如果有对应的文件夹，但是没有 jsonline 文件，那就创建一个
     * ！ artistFileName 是减少过最后的 . 的，所以 jsonline 文件的名称 要从 Chrome书签获得真正的名称
     */
    @Deprecated
    public void buildMissingJsonLineFileForArtist(SourceType sourceType, Set<String> artists) throws IOException {
        SpiderUtils spiderUtils = new SpiderUtils();
        if (sourceType == SourceType.SANKAKU) {
            // 获取所有 jsonline 文件名称
            String[] jsonlines = new File(sankakuDefaultInfoPath).list(jsonlineFilenameFilter);
            Set<String> jsonlineName = new HashSet<String>();
            // 获取 jsonline 文件对应的作者名称
            for (int i = 0; i < jsonlines.length; i++) {
                jsonlineName.add(jsonlines[i].substring(0, jsonlines[i].length() - 9));
            }
            // 循环给定的作者列表： 这个列表应该使用 ChromeReader 获得的收藏的作者列表
            for (String artistName : artists
            ) {
                // 如果有这个 作者的作品

                if (!jsonlineName.contains(artistName)) {
                    int num = getArtworkNum(SourceType.SANKAKU, spiderUtils.fileNameGenerator(artistName));
                    System.out.println("没有文件：" + artistName + "\t作品数量" + num);
                    if (num > 0) {
                        boolean created = new File(sankakuDefaultInfoPath + artistName + ".jsonline").createNewFile();
                        System.out.println(sankakuDefaultInfoPath + artistName + ".jsonline" + "\t创建" + created);
                    }
                }
            }
        }

    }

    public void initSankakuLevelPath() {
        // 图-0-重口
        String pic = "T";
        String vid = "V";
        Map<Integer, String> picLevelName = new HashMap<Integer, String>() {{
            put(0, "典藏");
            put(1, "核心");
            put(2, "优秀");
            put(3, "良好");
            put(4, "一般");
            put(5, "及格");
            put(6, "废弃");
        }};
        Map<Integer, String> vidLevelName = new HashMap<Integer, String>() {{
            put(0, "典藏");
            put(1, "优秀");
            put(2, "良好");
            put(3, "一般");
            put(4, "废弃");
        }};
        int picSize = picLevelName.size();
        int vidSize = vidLevelName.size();
        List<String> picSpec = Arrays.asList("可爱", "硬核", "诱惑", "3D", "安全", "重口");
        List<String> vidSpec = Arrays.asList("高分", "口味", "2D");
        for (int i = 0; i < picSize; i++) {
            StringBuffer nameBuffer = new StringBuffer();
            nameBuffer.append(pic).append("-").append(i).append("-").append(picLevelName.get(i)).append("-");
            String prefix = nameBuffer.toString();
            for (String sp :
                    picSpec) {
                if (!new File(sankakuRootPath + prefix + sp).exists())
                    new File(sankakuRootPath + prefix + sp).mkdir();
            }
        }
        for (int i = 0; i < vidSize; i++) {
            StringBuffer nameBuffer = new StringBuffer();
            nameBuffer.append(vid).append("-").append(i).append("-").append(vidLevelName.get(i)).append("-");
            String prefix = nameBuffer.toString();
            for (String sp : vidSpec) {
                if (!new File(sankakuRootPath + prefix + sp).exists()) {
                    new File(sankakuRootPath + prefix + sp).mkdir();
                }
            }
        }
    }

    public Map<String, Integer> getArtistListByDB() {
        return sankakuDBSourceManager.getArtists();
    }

    public void updateArtistPathAndLevel() {
        System.out.println("更新作者作品路径与级别");
        Map<String, Integer> artists = getArtistListByDB();
        Set<String> names = artists.keySet();
        int i = 0;
        for (String name :
                names) {
            if (true) {
                System.out.println(i++);
                String vid_path = getArtistPath(SourceType.SANKAKU, "vid.mp4", name);
                String pic_path = getArtistPath(SourceType.SANKAKU, "pic.jpg", name);
                Matcher matcherPic = priorityPattern.matcher(pic_path);
                Matcher matcherVid = priorityPattern.matcher(vid_path);
                int pic_level = 10;
                int vid_level = 10;
                if (matcherPic.find()) {
                    pic_level = Integer.valueOf(matcherPic.group(1));
                }
                if (matcherVid.find()) {
                    vid_level = Integer.valueOf(matcherVid.group(1));
                }

                System.out.println(name + " " + vid_path + " " + vid_level + " " + pic_path + " " + pic_level);
                sankakuDBSourceManager.updateArtistPathAndLevel(name, pic_level, pic_path.substring(16), vid_level, vid_path.substring(16));
            }

        }
    }

    public List<String> getArtworkAddresses(String name) {

        List<String> sanCodes = sankakuDBSourceManager.getArtworkSanCodes(name);
        List<String> address = new ArrayList<>();
        for (String code : sanCodes
        ) {
            address.add("https://chan.sankakucomplex.com/post/show/" + code);
        }
        return address;
    }

    public static void main(String[] args) throws IOException {
        SourceManager testSourceManager = new SourceManager("H:\\ROOT", "G:\\ROOT");
        String path = testSourceManager.getArtistPath(SourceType.SANKAKU, ".jpg", "cosine");
        System.out.println(path);
//        testSourceManager.updateArtistPathAndLevel();


    }
}
