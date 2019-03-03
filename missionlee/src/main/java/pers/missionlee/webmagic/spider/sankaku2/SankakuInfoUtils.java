package pers.missionlee.webmagic.spider.sankaku2;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:24
 */
public class SankakuInfoUtils {
    private SankakuSpiderProcessor processor;
    private static String ARTWORK_INFO_FILE_NAME = "artworkInfo.jsonline";
    private String fullParentPath;
    private File artworkInfoFile;

    public SankakuInfoUtils(SankakuSpiderProcessor processor) {
        this.processor = processor;
        preCheck();
        init();
    }

    private void preCheck() { //判断 标签对应的目录是否存在，如果不存在初始一个
        File artistFile = new File(processor.ROOT_PATH + processor.TAG);
        if (!artistFile.exists())
            initFilesForTag();

    }

    private void initFilesForTag() {
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

    private void init() {
        this.fullParentPath = processor.ROOT_PATH + processor.TAG;
        this.artworkInfoFile = new File(fullParentPath + "/" + ARTWORK_INFO_FILE_NAME);
    }
    /**
     * @Description:
     *      解析文件获取文件中记录的所有 ArtworkInfo
     *      1. 如果 json文件中有重复记录（name字段相同），则会去重并重置 json文件
     *          重写了 ArtworkInfo 的 equals 类， 使用 name字段判断 equals，并使用 List.contains()方法去重
     *      2. 如果解析发现 文本记录的信息与实际本地文件不符合，删除文本记录中多余的内容（但是如果文件多出来则不会处理），重置json文件
     * @Param: []
     * @return: java.util.List<pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo>
     * @Author: Mission Lee
     * @date: 2019/3/2
     */
    public List<ArtworkInfo> getArtworkInfoMap() throws IOException {

        List<ArtworkInfo> list = new ArrayList<ArtworkInfo>();
        if (artworkInfoFile.exists()) {
            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "UTF8");
            String[] artworkInfoLines = artworkInfos.split("\n");
            for (int i = 0; i < artworkInfoLines.length; i++) {
                if (!StringUtils.isEmpty(artworkInfoLines[i])){
                    ArtworkInfo artworkInfo = JSON.parseObject(artworkInfoLines[i], ArtworkInfo.class);
                    if(!list.contains(artworkInfo))
                        list.add(artworkInfo);
                }
            }
            if(list.size()!=artworkInfoLines.length){
                rebuildArtworkInfoFile(list);
            }
            File tagFilePic = new File(fullParentPath + "/pic");
            File tagFileVid = new File(fullParentPath + "/vid");
            String[] pics = tagFilePic.list();
            String[] vids = tagFileVid.list();
            List<String> allWeHave = new ArrayList<String>();
            System.out.println("JSON: "+list.size()+" PIC/VID: "+pics.length+"/"+vids.length);
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
                    if (!allWeHave.contains(((ArtworkInfo)iterator.next()).getName())) {
                        iterator.remove();
                        remove++;
                    }
                }
                System.out.println("REMOVE: "+remove);
                if(remove>0){
                    rebuildArtworkInfoFile(list);
                }

            }


        }

        return list;
    }

    private void rebuildArtworkInfoFile(List<ArtworkInfo> list) throws IOException {
        Iterator iterator = list.iterator();
        if (iterator.hasNext())
            FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(iterator.next()) + "\n", "UTF8", false);
        while (iterator.hasNext()) {
            FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(iterator.next()) + "\n", "UTF8", true);

        }
    }
    /**
     * @Description: 用于把原本旧的一体json变成 jsonline
     * @Param: [rootPath]
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/3/3
     */
    // TODO: 2019/3/3 还未检测是否好用 
    public static void fileConvertor(String rootPath){
        if(!rootPath.endsWith("/"))
            rootPath=rootPath+"/";
        File root = new File(rootPath);
        if(root.exists()&&root.isDirectory()){ // 简单验证根目录
            String[] files = root.list();
            for (int i = 0; i < files.length; i++) {
                File json = new File(rootPath+files[i]+"/artwork.json");
                File jsonline = new File(rootPath+files[i]+"/artwork.jsonline");
                if((!jsonline.exists()) && (json.exists()) && (json.isFile())){ // jsonline 不存在，json存在的情况下进行操作
                    jsonline.mkdir();
                    List<ArtworkInfo> artworkInfos = readOldArtworkInfo(json);
                    if(artworkInfos.size()>0){
                        for (ArtworkInfo info :
                                artworkInfos) {

                            try {
                                FileUtils.writeStringToFile(jsonline,JSON.toJSONString(info)+"\n","UTF8",true);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

    }
    public static List<ArtworkInfo> readOldArtworkInfo(File oldJson){
        String jsonStr = null;
        try {
            jsonStr = FileUtils.readFileToString(oldJson, "UTF8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<JSONObject> lists = JSON.parseObject(jsonStr, List.class);
        List<ArtworkInfo> artworkInfos = new ArrayList<ArtworkInfo>();
        if(artworkInfos!=null)
            for (JSONObject object : lists
            ) {
                artworkInfos.add(object.toJavaObject(ArtworkInfo.class));
            }
        return artworkInfos;
    }
    public synchronized void appendInfo(ArtworkInfo info) throws IOException {
        FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(info) + "\n", "UTF8", true);
    }
}
