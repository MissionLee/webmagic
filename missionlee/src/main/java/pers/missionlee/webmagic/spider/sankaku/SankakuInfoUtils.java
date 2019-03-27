package pers.missionlee.webmagic.spider.sankaku;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:24
 */
public class SankakuInfoUtils {
    private static Logger logger = LoggerFactory.getLogger(SankakuInfoUtils.class);
    private static String ARTWORK_INFO_FILE_NAME = "artworkInfo.jsonline";
    private static String ARTIST_INFO_FILE_NAME = "artistInfo.json";

    private File artworkInfoFile;
    private static final List<String> picFormats = new ArrayList<String>() {{
        add("jpg");
        add("png");
        add("jpeg");
        add("webp");
        add("gif");
    }};
    private static final String[] emptyStringArray = new String[0];


    private static String pathEndsFormater(String path) {
        if (!path.endsWith("/"))
            path = path + "/";
        return path;
    }

    /**
     * @param artistPath 表示当前作者的总目录
     */
    public static List<ArtworkInfo> getArtworkInfoMap(String artistPath) throws IOException {
        artistPath = formatRootPath(artistPath);
        List<ArtworkInfo> list = new ArrayList<ArtworkInfo>();
        File artworkInfoFile = new File(artistPath + ARTWORK_INFO_FILE_NAME);
        if (artworkInfoFile.exists()) {
            // 1. 清理垃圾文件 （小文件 与 临时文件）
            File artistFile = new File(artistPath);
            int cleaned = cleanFiles(artistFile);
            logger.info("CLEAN " + cleaned + " BAD FILES");
            // 2. 从文档中获取文件列表
            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "UTF8");
            String[] artworkInfoLines = artworkInfos.split("\n");
            convertArtworkStringToList(list, artworkInfoLines);
            // 3.处理文档与实际文件不符合的清空
            File tagFilePic = new File(artistPath + "/pic");
            File tagFileVid = new File(artistPath + "/vid");
            String[] pics = emptyStringArray;
            if (tagFilePic.exists())
                pics = tagFilePic.list();
            String[] vids = emptyStringArray;
            if (tagFileVid.exists())
                vids = tagFileVid.list();
            List<String> allWeHave = new ArrayList<String>();
            logger.info("JSON: " + list.size() + " PIC/VID: " + pics.length + "/" + vids.length);
            if (list.size() != (pics.length + vids.length)) {
                int remove = 0;
                for (int i = 0; i < pics.length; i++) {
                    allWeHave.add(pics[i]);
                }
                for (int i = 0; i < vids.length; i++) {
                    allWeHave.add(vids[i]);
                }
                Iterator iterator = list.iterator();
                while (iterator.hasNext()) {
                    if (!allWeHave.contains(((ArtworkInfo) iterator.next()).getName())) {
                        iterator.remove();
                        remove++;
                    }
                }
                logger.info("REMOVE: " + remove);
                if (remove > 0) {
                    rebuildArtworkInfoFile(artworkInfoFile, list);
                }

            }
        }
        return list;
    }

    private static List<ArtworkInfo> getArtworkInfoStatic(File artworkInfoFile) throws IOException {
        List<ArtworkInfo> list = new ArrayList<ArtworkInfo>();
        if (artworkInfoFile.exists()) {
            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "UTF8");
            String[] artworkInfoLines = artworkInfos.split("\n");
            convertArtworkStringToList(list, artworkInfoLines);
        }

        return list;
    }

    private static void convertArtworkStringToList(List<ArtworkInfo> list, String[] artworkInfoLines) {
        for (int i = 0; i < artworkInfoLines.length; i++) {
            if (!StringUtils.isEmpty(artworkInfoLines[i])) {
                ArtworkInfo artworkInfo = JSON.parseObject(artworkInfoLines[i], ArtworkInfo.class);
                if (!list.contains(artworkInfo))
                    list.add(artworkInfo);
            }
        }
    }

    private static void rebuildArtworkInfoFile(File file, List<ArtworkInfo> list) throws IOException {
        Iterator iterator = list.iterator();
        FileUtils.writeStringToFile(file, "", "UTF8", false);
        while (iterator.hasNext()) {
            FileUtils.writeStringToFile(file, JSON.toJSONString(iterator.next()) + "\n", "UTF8", true);
        }
    }


    public static synchronized void appendArtworkInfo(ArtworkInfo info, String artistPath) throws IOException {
        artistPath = formatRootPath(artistPath);
        FileUtils.writeStringToFile(new File(artistPath + ARTWORK_INFO_FILE_NAME), JSON.toJSONString(info) + "\n", "UTF8", true);
    }


    public static ArtistInfo freshArtistInfo(List<ArtworkInfo> artworkInfos, String artistPath, String artistName) {
        ArtistInfo artistInfo = getArtistInfo(artworkInfos, artistName);
        artistPath = formatRootPath(artistPath);
        try {
            FileUtils.writeStringToFile(new File(artistPath + ARTIST_INFO_FILE_NAME), JSON.toJSONString(artistInfo), "UTF8", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return artistInfo;
    }

    /**
     * @Description: 用于重构所有的作者信息
     * @Param: [rootPath]
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/3/4
     */
    public static void freshAllArtistInfo(String rootPath) throws IOException {

        rootPath = formatRootPath(rootPath);
        File root = new File(rootPath);
        if (root.exists() && root.isDirectory()) {
            String[] files = root.list();

            for (int i = 0; i < files.length; i++) {
                File jsonline = new File(rootPath + files[i] + "/" + ARTWORK_INFO_FILE_NAME);
                List<ArtworkInfo> artworkInfos = getArtworkInfoStatic(jsonline);
                String name = files[i];

                ArtistInfo artistInfo = getArtistInfo(artworkInfos, name);
                /**
                 * tag统计会造成内容非常多，所以暂时没有放到信息中
                 * */
//                TreeMap map = new TreeMap();
//                Set<String> keys = tagCounts.keySet();
//                for (String key :
//                        keys) {
//                    map.put(tagCounts.get(key),key);
//                }
//                System.out.println(map);
                //artistInfo.setHotTags(tagCounts);
                try {

                    FileUtils.writeStringToFile(new File(rootPath + files[i] + "/" + ARTIST_INFO_FILE_NAME), JSON.toJSONString(artistInfo), "UTF8", false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private static String formatRootPath(String rootPath) {
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        return rootPath;
    }

    private static ArtistInfo getArtistInfo(List<ArtworkInfo> artworkInfos, String name) {
        ArtistInfo artistInfo = new ArtistInfo();
        artistInfo.setArtworkNum(artworkInfos.size());
        artistInfo.setName(name);
        artistInfo.setUpdateTime(System.currentTimeMillis());
        Map<String, Integer> tagCounts = new HashMap<String, Integer>();
        for (ArtworkInfo info : artworkInfos
        ) {
            List<String> generalTags = info.getTagGeneral();
            for (String tag :
                    generalTags) {
                if (tagCounts.containsKey(tag)) {
                    tagCounts.put(tag, tagCounts.get(tag) + 1);
                } else {
                    tagCounts.put(tag, 1);
                }
            }
            if (picFormats.contains(info.getFormat())) {

                if (info.getRating().equals("Questionable")) {
                    artistInfo.getQuestionablePics().add(info.getName());
                } else if (info.getRating().equals("Explicit")) {
                    artistInfo.getExplicitPics().add(info.getName());
                } else {
                    artistInfo.getSafePics().add(info.getName());
                }
            } else {
                if (info.getRating().equals("Questionable")) {
                    artistInfo.getQuestionableVids().add(info.getName());
                } else if (info.getRating().equals("Explicit")) {
                    artistInfo.getExplicitVids().add(info.getName());
                } else {
                    artistInfo.getSafeVids().add(info.getName());
                }
            }
        }
        return artistInfo;
    }

    public static int deleteEmptyFilesTag(String rootPath, String tag) {
        rootPath = formatRootPath(rootPath);
        File tagFile = new File(rootPath + tag);
        return cleanFiles(tagFile);
    }

    /**
     * @Description: 下载出错的内容可能是个空文件，这里可以把空文件删除
     * @Param: [rootPath]
     * @return: int
     * @Author: Mission Lee
     * @date: 2019/3/4
     */
    public static int deleteEmptyFilesRoot(String rootPath) {
        int deleted = 0;
        File root = new File(rootPath);
        File[] tags = root.listFiles();
        for (int i = 0; i < tags.length; i++) {// tag 级别
            deleted += cleanFiles(tags[i]);
        }
        return deleted;
    }

    /**
     * @Description:
     * @Param: [tag]
     * @return: int 被删除的文件数量
     * @Author: Mission Lee
     * @date: 2019/3/4
     */
    private static int cleanFiles(File artist) {

        int deleted = 0;
        File[] files = artist.listFiles();
        for (int j = 0; j < files.length; j++) { // pic 与 vid
            if (files[j].isDirectory()) {
                if(files[j].listFiles().length==0){
                    // 处理空文件夹（早期代码遗留问题）
                    files[j].delete();
                }else{
                    // 清空小文件
                    File[] pcvi = files[j].listFiles();
                    for (int k = 0; k < pcvi.length; k++) { // 具体文件
                        if (pcvi[k].length() < 10 || pcvi[k].getName().contains("-")) {
                            logger.info("CLEAN: " + pcvi[k].getName());
                            pcvi[k].delete();
                            deleted++;
                        }
                    }
                }

            }
        }
        return deleted;
    }

    public static int getArtworkNumber(File artist){
        int num = 0;
        cleanFiles(artist);
        File[] files = artist.listFiles();
        for (int i = 0; i < files.length; i++) {
            if(files[i].isDirectory()){
                num+=files[i].listFiles().length;
            }
        }
        return num;
    }
}
