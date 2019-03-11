package pers.missionlee.webmagic.spider.sankaku;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
    private SankakuSpiderProcessor processor;
    private static String ARTWORK_INFO_FILE_NAME = "artworkInfo.jsonline";
    private static String ARTIST_INFO_FILE_NAME = "artistInfo.json";
    private String fullParentPath;
    private File artworkInfoFile;
    private static final List<String> picFormats = new ArrayList<String>() {{
        add("jpg");
        add("png");
        add("jpeg");
        add("webp");
        add("gif");
    }};
    private static final String[] emptyStringArray = new String[0];
    @Deprecated
    public SankakuInfoUtils(SankakuSpiderProcessor processor) {
        this.processor = processor;
        preCheck();
        this.fullParentPath = processor.ROOT_PATH + processor.TAG;
        this.artworkInfoFile = new File(fullParentPath + "/" + ARTWORK_INFO_FILE_NAME);
    }
    @Deprecated
    private void preCheck() { //判断 标签对应的目录是否存在，如果不存在初始一个
        File artistFile = new File(processor.ROOT_PATH + processor.TAG);
        if (!artistFile.exists())
            initFilesForTag();

    }

    private static String pathEndsFormater(String path) {
        if (!path.endsWith("/"))
            path = path + "/";
        return path;
    }
    /**
     * 创建文件的时候 过程目录会被创建，所以没必要创建文件夹
     * */
    @Deprecated
    private void initFilesForTag() { // 创建初始文件 不包含作者统计文件
        System.out.println("INIT FAILE FOR TAG");
        File rootPath = new File(processor.ROOT_PATH);
        // 根目录存在
        if (!rootPath.exists() || !rootPath.isDirectory())
            throw new RuntimeException("WRONG ROOT PATH:" + processor.ROOT_PATH);
        // 创建各级目录/文件
        // TAG目录
        String tagPath = processor.ROOT_PATH + processor.TAG;
        new File(tagPath).mkdir();
        new File(tagPath + "/pic").mkdir();
        new File(tagPath + "/vid").mkdir();
        try {
            new File(tagPath + "/" + ARTWORK_INFO_FILE_NAME).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * @Description: 解析文件获取文件中记录的所有 ArtworkInfo
     * 1. 如果 json文件中有重复记录（name字段相同），则会去重并重置 json文件
     * 重写了 ArtworkInfo 的 equals 类， 使用 name字段判断 equals，并使用 List.contains()方法去重
     * 2. 如果解析发现 文本记录的信息与实际本地文件不符合，删除文本记录中多余的内容（但是如果文件多出来则不会处理），重置json文件
     * @Param: []
     * @return: java.util.List<pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo>
     * @Author: Mission Lee
     * @date: 2019/3/2
     */
//    @Deprecated
//    public List<ArtworkInfo> getArtworkInfoMap() throws IOException {
//
//        List<ArtworkInfo> list = new ArrayList<ArtworkInfo>();
//        if (artworkInfoFile.exists()) {
//            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "UTF8");
//            String[] artworkInfoLines = artworkInfos.split("\n");
//            convertArtworkStringToList(list, artworkInfoLines);
//            if (list.size() != artworkInfoLines.length) {
//                rebuildArtworkInfoFile(list);
//            }
//            File tagFilePic = new File(fullParentPath + "/pic");
//            File tagFileVid = new File(fullParentPath + "/vid");
//            String[] pics = tagFilePic.list();
//            String[] vids = tagFileVid.list();
//            List<String> allWeHave = new ArrayList<String>();
//            System.out.println("JSON: " + list.size() + " PIC/VID: " + pics.length + "/" + vids.length);
//            if (list.size() != (pics.length + vids.length)) {
//                int remove = 0;
//                for (int i = 0; i < pics.length; i++) {
//                    allWeHave.add(pics[i]);
//                }
//                for (int i = 0; i < vids.length; i++) {
//                    allWeHave.add(vids[i]);
//                }
//                Iterator iterator = list.iterator();
//                while (iterator.hasNext()) {
//                    if (!allWeHave.contains(((ArtworkInfo) iterator.next()).getName())) {
//                        iterator.remove();
//                        remove++;
//                    }
//                }
//                System.out.println("REMOVE: " + remove);
//                if (remove > 0) {
//                    rebuildArtworkInfoFile(list);
//                }
//
//            }
//
//
//        }
//
//        return list;
//    }

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
            System.out.println("CLEAN " + cleaned + " BAD FILES");
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
            System.out.println("JSON: " + list.size() + " PIC/VID: " + pics.length + "/" + vids.length);
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
                System.out.println("REMOVE: " + remove);
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

    private void rebuildArtworkInfoFile(List<ArtworkInfo> list) throws IOException {
        Iterator iterator = list.iterator();
        if (iterator.hasNext())
            FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(iterator.next()) + "\n", "UTF8", false);
        while (iterator.hasNext()) {
            FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(iterator.next()) + "\n", "UTF8", true);

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
    private static int cleanFiles(File tag) {

        int deleted = 0;
        File[] files = tag.listFiles();
        for (int j = 0; j < files.length; j++) { // pic 与 vid
            if (files[j].isDirectory()) {
                // 清空小文件
                File[] pcvi = files[j].listFiles();
                for (int k = 0; k < pcvi.length; k++) { // 具体文件
                    if (pcvi[k].length() < 10) {
                        System.out.println("CLEAN: " + pcvi[k].getName());
                        pcvi[k].delete();
                        deleted++;
                    }
                }
                // 清空临时文件：名字中含有 "-" 的文件
                String[] pcviNames = files[j].list();
                for (int i = 0; i < pcviNames.length; i++) {
                    if (pcviNames[i].contains("-")) {
                        System.out.println("CLEAN: " + pcviNames[i]);
                        new File(tag.getPath() + pcviNames[i]).delete();
                        deleted++;
                    }
                }
            }
        }
        return deleted;
    }


//    /**
//     * @Description: 用于把原本旧的一体json变成 jsonline
//     * @Param: [rootPath]
//     * @return: void
//     * @Author: Mission Lee
//     * @date: 2019/3/3
//     */
//    @Deprecated
//    public static void fileConvertor(String rootPath) {
//        rootPath = formatRootPath(rootPath);
//        File root = new File(rootPath);
//        if (root.exists() && root.isDirectory()) { // 简单验证根目录
//            String[] files = root.list();
//
//            for (int i = 0; i < files.length; i++) {
//                File json = new File(rootPath + files[i] + "/artwork.json");
//                File jsonline = new File(rootPath + files[i] + "/" + ARTWORK_INFO_FILE_NAME);
//                if ((!jsonline.exists()) && (json.exists()) && (json.isFile())) { // jsonline 不存在，json存在的情况下进行操作
//                    System.out.println("convert:" + json.getName());
//                    try {
//                        jsonline.createNewFile();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    List<ArtworkInfo> artworkInfos = readOldArtworkInfo(json);
//                    if (artworkInfos.size() > 0) {
//                        for (ArtworkInfo info :
//                                artworkInfos) {
//
//                            try {
//                                FileUtils.writeStringToFile(jsonline, JSON.toJSONString(info) + "\n", "UTF8", true);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//    }
//
//    @Deprecated
//    public static List<ArtworkInfo> readOldArtworkInfo(File oldJson) {
//        String jsonStr = null;
//        try {
//            jsonStr = FileUtils.readFileToString(oldJson, "UTF8");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        List<JSONObject> lists = JSON.parseObject(jsonStr, List.class);
//        List<ArtworkInfo> artworkInfos = new ArrayList<ArtworkInfo>();
//        if (artworkInfos != null)
//            for (JSONObject object : lists
//            ) {
//                artworkInfos.add(object.toJavaObject(ArtworkInfo.class));
//            }
//        return artworkInfos;
//    }
//    @Deprecated
//    public synchronized void appendInfo(ArtworkInfo info) throws IOException {
//        FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(info) + "\n", "UTF8", true);
//    }
//    /**
//     * @Description: 用于把当前processor存储在内存中的信息计算并写入 artistinfo.json文件中
//     * @Param: []
//     * @return: void
//     * @Author: Mission Lee
//     * @date: 2019/3/4
//     */
//    @Deprecated
//    public void freshArtistInfo() {
//
//        List<ArtworkInfo> artworkInfos = processor.diarySankakuSpider.getArtworkInfos();
//        ArtistInfo artistInfo = getArtistInfo(artworkInfos, processor.TAG);
//        try {
//            FileUtils.writeStringToFile(new File(processor.ROOT_PATH + processor.TAG + "/" + ARTIST_INFO_FILE_NAME), JSON.toJSONString(artistInfo), "UTF8", false);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
