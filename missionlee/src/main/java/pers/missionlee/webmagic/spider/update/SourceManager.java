package pers.missionlee.webmagic.spider.update;

import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.AutoDeduplicatedArrayList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * 本地文件管理器
 * 1. 自动初始化层级结构目录
 * 2.
 * @create: 2019-04-12 08:50
 */
public class SourceManager {
    public enum SourceType {
        SANKAKU,
        IDOL
    }

    private static final String DIRL1_SANKAKU = "sankaku";
    private static final String DIRL1_IDOL = "idol";
    private static final String DIRL1_TMP = "tmp";
    private static final String DIRL2_PIC = "pic";
    private static final String DIRL2_VID = "vid";
    private static final String DIRL2_INFO = "info";
    private static final String FILE_SUBFIX_ARTWORK = ".jsonline";
    private static final String FILE_SUBFIX_ARTIST = ".json";
    private static final String[] ENPTY_STRING_ARRAY = new String[0];
    private String rootPath;
    // sankaku
    private String sankakuRootPath;
    private String sankakuPicPath;
    private String sankakuVidPath;
    private String sankakuInfoPath;
    private PathList sankakuPathList;
    private File sankakuPics;
    private File sankakuVids;
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

    public SourceManager(String rootPath) {

        this.rootPath = formatPath(rootPath);
        this.tmpPath = buildPath(rootPath, DIRL1_TMP);
        initSankakuPathInfo();
        initIdolPathInfo();
        initSankakuArtistList();
    }

    private void initSankakuPathInfo() {
        this.sankakuRootPath = buildPath(rootPath, DIRL1_SANKAKU);
        this.sankakuInfoPath = buildPath(sankakuRootPath, DIRL2_INFO);
        this.sankakuPicPath = buildPath(sankakuRootPath + DIRL2_PIC);
        this.sankakuVidPath = buildPath(sankakuRootPath + DIRL2_VID);
        this.sankakuPics = new File(sankakuPicPath);
        this.sankakuVids = new File(sankakuVidPath);
        this.sankakuPathList = new PathList(sankakuPicPath, sankakuVidPath, sankakuInfoPath);
    }

    private void initIdolPathInfo() {
        this.idolRootPath = buildPath(rootPath + DIRL1_IDOL);
        this.idolInfoPath = buildPath(idolRootPath, DIRL2_INFO);
        this.idolPicPath = buildPath(idolRootPath, DIRL2_PIC);
        this.idolVidPath = buildPath(idolRootPath, DIRL2_VID);
        this.idolPathList = new PathList(idolPicPath, idolVidPath, idolInfoPath);
    }

    private void initSankakuArtistList() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        if (sankakuPics == null || sankakuVids == null) {
            initSankakuPathInfo();
        }
        File[] picArtists = this.sankakuPics.listFiles();
        countArtists(map, picArtists);
        File[] vidArtists = this.sankakuVids.listFiles();
        countArtists(map, vidArtists);
        this.sankakuArtists = map;
    }

    private void countArtists(Map<String, Integer> map, File[] artists) {
        for (int i = 0; i < artists.length; i++) {
            if (map.containsKey(artists[i].getName())) {
                map.put(artists[i].getName(),
                        map.get(artists[i].getName()) + artists[i].list().length);
            } else {
                map.put(artists[i].getName(), artists[i].list().length);
            }
        }
    }

    public static String getDirl1Sankaku() {
        return DIRL1_SANKAKU;
    }

    public Map<String, Integer> getSankakuArtists() {
        return sankakuArtists;
    }

    public Map<String, Integer> getIdolArtists() {
        return idolArtists;
    }

    public Map<String, Integer> getSankakuOfficaialArtists() {
        return sankakuOfficaialArtists;
    }

    public Map<String, Integer> getIdolOfficialArtists() {
        return idolOfficialArtists;
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
            return sankakuPathList;
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
        return aimPath;
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
     */
    public <T extends List<ArtworkInfo>> T getArtworkOfArtist(SourceType sourceType, String artistName) throws IOException {
        PathList pathList = getPathList(sourceType);
        T artworkInfoList = (T) new AutoDeduplicatedArrayList();
        File artworkInfoFile = new File(pathList.InfoPath + artistName + FILE_SUBFIX_ARTWORK);
        if (artworkInfoFile.exists()) {
            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "utf8");
            String[] artworkInfoStringArray = artworkInfos.split("\n");
            int jsonLength = artworkInfoStringArray.length;
            ArtworkInfo.convertArtworkStringToList(artworkInfoList, artworkInfoStringArray);
            // 处理 json记录与实际文件不符合的内容
            File pics = new File(pathList.PicPath + artistName);
            File vids = new File(pathList.VidPath + artistName);
            String[] picNames = pics.list();
            String[] vidNames = vids.list();
            List<String> artworks = new ArrayList<String>();
            int removed = 0;
            if (picNames != null)
                for (int i = 0; i < picNames.length; i++) {
                    artworks.add(picNames[i]);
                }
            if (vidNames != null)
                for (int i = 0; i < vidNames.length; i++) {
                    artworks.add(vidNames[i]);
                }
            Iterator iterator = artworkInfoList.iterator();
            while (iterator.hasNext()) {
                if (!artworks.contains(((ArtworkInfo) iterator.next()).getName())) {
                    iterator.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                rebuildArtworkInfoFile(sourceType, artistName, artworkInfoList);
            }
        }
        return artworkInfoList;
    }

    /**
     * 作品信息：清空原本的作品信息文件，写入新的内容
     */
    private void rebuildArtworkInfoFile(SourceType sourceType, String artistName, List<ArtworkInfo> artworkInfos) throws IOException {
        PathList pathList = getPathList(sourceType);
        File artworkInfoFile = new File(pathList.InfoPath + artistName + FILE_SUBFIX_ARTWORK);
        FileUtils.writeStringToFile(artworkInfoFile, "", "utf8", false);
        Iterator iterator = artworkInfos.iterator();
        while (iterator.hasNext()) {
            appendArtworkInfoToFile(artworkInfoFile, (ArtworkInfo) iterator.next());
        }
    }

    /**
     * 作品信息：向作品信息文件追加一条新的作品信息
     */
    public synchronized void appendArtworkInfoToFile(SourceType sourceType, String artistName, ArtworkInfo artworkInfo) throws IOException {
        PathList pathList = getPathList(sourceType);
        File artworkInfoFile = new File(pathList.InfoPath + artistName + FILE_SUBFIX_ARTWORK);
        FileUtils.writeStringToFile(artworkInfoFile, artworkInfo.toString() + "\n", "utf8", true);
    }

    /**
     * 作品信息：向作品信息文件追加一条新的作品信息
     */
    private synchronized void appendArtworkInfoToFile(File artworkInfoFile, ArtworkInfo artworkInfo) throws IOException {
        FileUtils.writeStringToFile(artworkInfoFile, artworkInfo.toString() + "\n", "utf8", true);
    }

    /**
     * 作品信息：获取作者的作品数量
     */
    public int getArtworkNum(SourceType sourceType, String artistName) {
        int num = 0;
        PathList pathList = getPathList(sourceType);
        File pics = new File(pathList.PicPath + artistName);
        File vids = new File(pathList.VidPath + artistName);
        if (pics.exists() && pics.listFiles() != null)
            num += pics.listFiles().length;
        if (vids.exists() && vids.listFiles() != null)
            num += vids.listFiles().length;
        return num;
    }

    /**
     * 作者信息：从作品信息提取作者信息
     */
    public static ArtistInfo getArtistInfoFromArtwork(List<ArtworkInfo> artworkInfos, String artistName) {
        return ArtistInfo.getArtistInfoFromArtwork(artworkInfos, artistName);
    }

    /**
     * 作者信息：作者信息落盘
     */
    public ArtistInfo writeArtistInfo(SourceType sourceType, ArtistInfo artistInfo) throws IOException {
        PathList pathList = getPathList(sourceType);
        File artistInfoFile = new File(pathList.InfoPath + artistInfo.getName() + FILE_SUBFIX_ARTIST);
        FileUtils.writeStringToFile(artistInfoFile, artistInfo.toString(), "utf8", false);
        return artistInfo;
    }

    public ArtistInfo writeArtistInfo(SourceType sourceType, List<ArtworkInfo> artworkInfos, String artistName) throws IOException {
        ArtistInfo artistInfo = getArtistInfoFromArtwork(artworkInfos, artistName);
        return writeArtistInfo(sourceType, artistInfo);
    }

    /**
     * 文件存储：用于讲下载成功的临时文件存入目标位置
     */
    public Boolean saveFile(SourceType sourceType, File tmpFile, String artistName, String artworkName) {
        String parentPath = getParentPath(sourceType, artworkName);
        //return tmpFile.renameTo(new File(parentPath + artistName + "/" + artworkName));
        try {
            FileUtils.moveFile(tmpFile, new File(parentPath + artistName + "/" + artworkName));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getParentPath(SourceType sourceType, String artworkName) {
        String parentPath = "";
        PathList pathList = getPathList(sourceType);
        if (isVideo(artworkName)) {
            parentPath = pathList.VidPath;
        } else {
            parentPath = pathList.PicPath;
        }
        System.out.println(parentPath);
        return parentPath;
    }

    /**
     * 文件存储：判断文件是否存在
     */
    public boolean exists(SourceType sourceType, String artistName, String artworkName) {
        String parentPath = getParentPath(sourceType, artworkName);
        return new File(parentPath + artistName + "/" + artworkName).exists();
    }

    /**
     * 文件存储：返回临时路径文件
     */
    public File getFileInTmpPath(String filename) {
        return new File(tmpPath + filename);
    }

    /**
     * 用于把旧版本的文件组织形式变为新版本的形式，当所有文件转移成功后，这份方法就不再有用
     */
    public static void convertOldSourceFileStructureToNewOne() throws IOException {
//        if (true)
//            throw new RuntimeException("必须看好新旧目录，目前仅支持 sankaku");
        SourceManager sourceManager = new SourceManager("D:\\ROOT");

        File oldSanRootFile = new File("D:\\sankaku");
        System.out.println(oldSanRootFile.getName());
        FileFilter dirFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        System.out.println(sourceManager.sankakuInfoPath);
        File[] files = oldSanRootFile.listFiles(dirFilter);
        File infoDir = new File(sourceManager.sankakuInfoPath);
        for (int i = 0; i < files.length; i++) {
            File[] cFiles = files[i].listFiles(dirFilter);
            String artistName = files[i].getName();
            System.out.println("artistName:" + artistName + " " + cFiles.length);
            File artistPicFile = new File(sourceManager.sankakuPicPath + artistName + "/");
            File artistVidFile = new File(sourceManager.sankakuVidPath + artistName + "/");
            System.out.println(artistVidFile.getPath());
            for (int j = 0; j < cFiles.length; j++) {

                // info vid pic 分别转移
                String name = cFiles[j].getName();
                System.out.println(name);
                if (name.contains("info")) {
                    File[] jsonFiles = cFiles[j].listFiles();
                    for (int k = 0; k < jsonFiles.length; k++) {
                        if (jsonFiles[k].getName().endsWith("json")) {
                            File renamed = new File(jsonFiles[k].getParent() + "/" + artistName + ".json");
                            if (jsonFiles[k].renameTo(renamed)) {
                                if(!new File(infoDir.getPath()+"/"+renamed.getName()).exists())

                                    FileUtils.moveFileToDirectory(renamed, infoDir, true);
                            }
                        } else if (jsonFiles[k].getName().endsWith("jsonline")) {
                            File renamed = new File(jsonFiles[k].getParent() + "/" + artistName + ".jsonline");
                            if (jsonFiles[k].renameTo(renamed))
                                if(!new File(infoDir.getPath()+"/"+renamed.getName()).exists())

                                    FileUtils.moveFileToDirectory(renamed, infoDir, true);
                        }
                    }
                } else if (name.contains("pic")) {
                    File[] pics = cFiles[j].listFiles();
                    System.out.println(pics.length);
                    for (int k = 0; k < pics.length; k++) {
                        if (!new File(artistPicFile.getPath() + "/" + pics[k].getName()).exists())
                            FileUtils.moveFileToDirectory(pics[k], artistPicFile, true);
                        // pics[k].renameTo(new File(sourceManager.sankakuPicPath+artistName+"/"+pics[k].getName()));
                    }
                } else if (name.contains("vid")) {
                    File[] vids = cFiles[j].listFiles();
                    System.out.println(vids.length);
                    for (int k = 0; k < vids.length; k++) {
                        if (!new File(artistVidFile.getPath() + "/" + vids[k].getName()).exists())

                            FileUtils.moveFileToDirectory(vids[k], artistVidFile, true);
//                        vids[k].renameTo(new File(sourceManager.sankakuVidPath+artistName+"/"+vids[k].getName()));
                    }
                }
            }
        }

    }

    public static void main(String[] args) throws IOException {
        //new File("C:\\Users\\Administrator\\Desktop\\ttt\\a.txt").renameTo(new File("C:\\Users\\Administrator\\Desktop\\ttt\\abc\\d.txt"));
        convertOldSourceFileStructureToNewOne();
    }
}
