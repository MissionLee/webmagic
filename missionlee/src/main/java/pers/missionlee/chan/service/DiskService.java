package pers.missionlee.chan.service;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.regexp.RE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.starter.SpiderSetting;
import pers.missionlee.webmagic.spider.newsankaku.dao.LevelInfo;
import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

        logger.info("初始化特殊作者名称完成");
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
            File[] addRootChildrenFiles = new File(addPoot).listFiles(PathUtils.aimFileFilter());
            extractPathInfo(addRootChildrenFiles, pics, vids);
        }
        // 提取 作者 book parent 路径下的 作者分布信息
        File[] artistBookParentFiles = new File(spiderSetting.getBookParentArtistBase()).listFiles(PathUtils.aimFileFilter());
        extractPathInfo(artistBookParentFiles, pics, vids);
        this.CHAN_ARTIST_PICS = pics;
        this.CHAN_ARTIST_VIDS = vids;
        logger.info("初始化作者信息完成");
    }

    Map<String, Integer> artistPicLevel; // key 为 path name
    Map<String, Integer> artistVidLevel;// key 为 path name
    Map<String, String> artistPicPath;// key 为 path name
    Map<String, String> artistVidPath;// key 为 path name
    static Pattern priorityPattern = Pattern.compile("-([0-9])-");

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
        System.out.println(this.artistPicLevel);
        System.out.println(this.artistPicPath);
    }
    public void cleanArtistBookParentBases(){

    }
    public void updateArtistLevel(DataBaseService dataBaseService) {
        initArtistPathLevel();
        List<LevelInfo> dbLevelInfos = dataBaseService.getLevelInfos();
        dbLevelInfos.forEach((LevelInfo info) -> {
            String artistName = info.name;
            String pathName = transformArtistNameToPath(artistName);
            if (!artistVidLevel.containsKey(pathName) && !artistPicLevel.containsKey(pathName)) {
                // 如果硬盘上没有这个作者了，标记为 is_target = 0
                logger.info("作者丢失（）："+artistName);
                dataBaseService.makeArtistLost(artistName);
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
            logger.info("文件成功保存到["+fileSaveName+"]： "+parentPath);
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
            parentPath = getCommonArtistParentPath(artworkInfo.aimName, artworkInfo.fileName);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.STUDIO.storePlace) {
            parentPath = getStudioParentPath(artworkInfo.aimName);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.SINGLE.storePlace) {
            parentPath = getSingleParentPath(artworkInfo);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.ARTIST_PARENT_BOOK.storePlace) {
            parentPath = getArtistParentBookPathParentPath(artworkInfo, PBPrefix);
        } else if (storePlace == ArtworkInfo.STORE_PLACE.SINGLE_PARENT_BOOK.storePlace) {
            parentPath = getSingleParentBookPathParentPath(artworkInfo, PBPrefix);
        } else {
            throw new RuntimeException("位置的Store Place");
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
        //base/levelpath/#{artistname}+ _BP/
        String commonArtistParentPath = getCommonArtistParentPath(artworkInfo.aimName, artworkInfo.fileName);
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
                .replaceAll("\\|", "~");

    }

    private String getSingleParentPath(ArtworkInfo artworkInfo) {
        // base/pic/copyright
        String basePath_1 = spiderSetting.getNormalSingleBase();
        String picvid_2 = PIC;
        if (PathUtils.isVideo(artworkInfo.fileName)) {
            picvid_2 = VID;
        }
        String copyright_3 = getCopyrightStringForPath(artworkInfo);
        String character_4 = getCharacterStringForPath(artworkInfo);
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

    private String getCommonArtistParentPath(String artistName, String artworkName) {
        String artistPathName = transformArtistNameToPath(artistName);
        if (PathUtils.isVideo(artworkName)) {
            Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_VIDS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entry = iterator.next();
                // 如果这个文件夹下有这个作者名字 ， 作者名字+"."  ,
                if (entry.getValue().contains(artistPathName) || entry.getValue().contains(artistPathName + "."))
                    return PathUtils.buildPath(entry.getKey(), artistPathName);
            }
            return PathUtils.buildPath(CHAN_ARTIST_DEFAULT_VID, artistPathName);
        } else {
            Iterator<Map.Entry<String, List<String>>> iterator = CHAN_ARTIST_PICS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<String>> entry = iterator.next();
                if (entry.getValue().contains(artistPathName) || entry.getValue().contains(artistPathName + "."))
                    return PathUtils.buildPath(entry.getKey(), artistPathName);
            }
            return PathUtils.buildPath(CHAN_ARTIST_DEFAULT_PIC, artistPathName);
        }
    }

    public String transformArtistNameToPath(String name) {
        if (null != namePairs) {

            name = namePairs.containsKey(name) ? namePairs.get(name) : name;
        }
        return name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
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
                if (new File(parentPath + artworkInfo.fileName).exists()) {
                    logger.info("文件已发现：" + parentPath + artworkInfo.fileName);
                    exists.set(true);
                }
            }
            return exists.get();
        } else {
            String parentPath = getParentPath(artworkInfo, PBPrefix, artworkInfo.storePlace);
            boolean exists = new File(parentPath + artworkInfo.fileName).exists();
            if (exists) {
                logger.info("文件已发现：" + parentPath + artworkInfo.fileName);
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

    public static void main(String[] args) {
        DiskService diskService = new DiskService(SpiderSetting.buildSetting());
//        diskService.moveAllFileInBookBackToPic();
//        diskService.initArtistPathLevel();
        diskService.updateArtistLevel(new DataBaseService());

    }

}
