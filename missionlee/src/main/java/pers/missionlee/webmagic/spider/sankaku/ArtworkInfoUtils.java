package pers.missionlee.webmagic.spider.sankaku;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 14:38
 */
public class ArtworkInfoUtils {
    public static final String ARTIST_INFO_FILENAME = SankakuSpiderProcessor.ARTIST_INFO_FILENAME;
    public static final String ARTWORK_INFO_FILENAME = SankakuSpiderProcessor.ARTWORK_INFO_FILENAME;
    private synchronized  void writeDiary(ArtworkInfo artworkInfo){

        try {
            File artworkInfoFile = new File(SankakuSpiderProcessor.PARENT_PATH+SankakuSpiderProcessor.TAG+"/"+"artwork-lined.jsonline");
            FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(artworkInfo)+"\n","UTF8",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * @Description: 判断这个tag是否又对应的文件夹，如果文件夹不存在，调用方法创建
     * @Param: [tag]
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/3/1
     */
    public static void filePathPreHandleByTag(String tag) {

            File son = new File(SankakuSpiderProcessor.PARENT_PATH+tag);
            if (!son.exists())
                pathPreCheck(SankakuSpiderProcessor.PARENT_PATH,tag);

    }

    private static String readFromFile(String fullPath) throws IOException {
        File file = new File(fullPath);
        return FileUtils.readFileToString(file, "UTF8");
    }

    private static void writeToFile(String tag, String artworkInfoFilename, String s) {
        String fullName = SankakuSpiderProcessor.PARENT_PATH+tag +"/"+ artworkInfoFilename;
        String jsonStr = s;
        File file = new File(fullName);
        try {
            FileUtils.writeStringToFile(file, jsonStr, "UTF8", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 作者信息
    public static ArtistInfo getArtistInfo(String tag) {
        String fileName = SankakuSpiderProcessor.PARENT_PATH+ tag + "/" + ARTIST_INFO_FILENAME;
        try {
            filePathPreHandleByTag(tag);
            if (!FileUtils.directoryContains(new File(SankakuSpiderProcessor.PARENT_PATH+tag), new File(fileName))) {
                return new ArtistInfo();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String jsonStr = readFromFile(fileName);
            return JSON.parseObject(jsonStr, ArtistInfo.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArtistInfo();
    }

    public static void setArtistInfo(String tag, ArtistInfo info) {
        writeToFile(tag, ARTIST_INFO_FILENAME, JSON.toJSONString(info));
    }
    /**
     * @Description: 获取指定目录下的 {@link ArtworkInfo} Map
     * @Param: [path]
     * @return: java.util.Map<java.lang.String , pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo>
     * @Author: Mission Lee
     * @date: 2019/2/27
     */
    // 作品信息
    @Deprecated
    public static Map<String, ArtworkInfo> getArtworkInfoMap(String path) {

        List<ArtworkInfo> artworkInfos = getArtworkInfoList(path);
        Map<String, ArtworkInfo> infos = new HashMap<String, ArtworkInfo>();
        int unknown = 0;
        for (ArtworkInfo info : artworkInfos
        ) {
            infos.put(info.getName() != null ? info.getName() : ("undefined-" + ++unknown), info);
        }
        return infos;
    }
    /**
     * @Description: 核心方法，获取作品信息列表
     * @Param: [tag]
     * @return: java.util.List<pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo>
     * @Author: Mission Lee
     * @date: 2019/2/27
     */
    public static List<ArtworkInfo> getArtworkInfoList(String tag) {

        String fullPath = SankakuSpiderProcessor.PARENT_PATH+ tag + "/" + ARTWORK_INFO_FILENAME;
        try {
            filePathPreHandleByTag(tag);
            if (!FileUtils.directoryContains(new File(SankakuSpiderProcessor.PARENT_PATH+tag), new File(fullPath))) {
                return new ArrayList<ArtworkInfo>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String jsonStr = readFromFile(fullPath);
            List<JSONObject> lists = JSON.parseObject(jsonStr, List.class);
            List<ArtworkInfo> artworkInfos = new ArrayList<ArtworkInfo>();
            if(artworkInfos!=null)
            for (JSONObject object : lists
            ) {
                artworkInfos.add(object.toJavaObject(ArtworkInfo.class));
            }
            System.out.println("JSON FILE: "+artworkInfos.size());

            // TODO: 2019/2/28  检测 pic vid 文件夹中的具体文件 和 List 是否相同，删除不存在文件的文本部分
            File tagFilePic = new File(SankakuSpiderProcessor.PARENT_PATH+tag+"/pic");
            File tagFileVid = new File(SankakuSpiderProcessor.PARENT_PATH+tag+"/vid");
            String[] pics = tagFilePic.list();
            String[] vids = tagFileVid.list();
            System.out.println("REAL NUM: pic "+pics.length+" / vid "+vids.length);
            if(artworkInfos.size() != (pics.length+vids.length)){
                List<String> allWeHave = new ArrayList<String>();
                for (int i = 0; i < pics.length; i++) {
                    allWeHave.add(pics[i]);
                }
                for(int i=0;i<vids.length;i++){
                    allWeHave.add(vids[i]);
                }
                // TODO: 2019/2/28 下方代码会出现  java.util.ConcurrentModificationException 错误
//                for (ArtworkInfo art:artworkInfos
//                ) {
//                    if(!allWeHave.contains(art.getName())){
//                        artworkInfos.remove(art);
//                        System.out.println("====  remove "+art+" from list ===");
//                    }
//                }
                // // TODO: 2019/2/28 由于上面的错误，改为下面的内容

                Iterator iterator = artworkInfos.iterator();
                while(iterator.hasNext()){
                    if(!allWeHave.contains(((ArtworkInfo)iterator.next()).getName())){
                        iterator.remove();
                    }
                }
                System.out.println(artworkInfos.size());
            }else{
                System.out.println("==== everything is OK ====");
            }

            return artworkInfos;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Deprecated
    public static void setArtworkInfo(String path, Map<String, ArtworkInfo> info) {
        List<ArtworkInfo> artworkInfos = new ArrayList<ArtworkInfo>();
        Set<String> keys = info.keySet();
        for (String key : keys
        ) {
            artworkInfos.add(info.get(key));
        }
        setArtworkInfo(path, artworkInfos);

    }

    public static void setArtworkInfo(String tag, List<ArtworkInfo> info) {
        writeToFile(tag, ARTWORK_INFO_FILENAME, JSON.toJSONString(info));
    }

    // 获取现有作品名称列表
    @Deprecated
    public static List<String> getArtworkList(String path) {
        List<ArtworkInfo> artworkInfos = getArtworkInfoList(path);
        List<String> nameList = new ArrayList<String>();
        for (ArtworkInfo info : artworkInfos
        ) {
            nameList.add(info.getName());
        }
        return nameList;
    }
    @Deprecated
    public static List<String> getChildPageList(String path) {
        List<ArtworkInfo> artworkInfos = getArtworkInfoList(path);
        List<String> pageList = new ArrayList<String>();
        for (ArtworkInfo info : artworkInfos) {
            pageList.add(info.getAddress());
        }
        return pageList;
    }

    /**
     * @Description: 验证给定的 父目录是否存在，检测tag是否有对应的目录，如果没有，初始化tag相关内容
     * @Param: [parentPath, tag]
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/2/27
     */
    private static void pathPreCheck(String parentPath, String tag) {

        if (!parentPath.endsWith("/"))
            SankakuSpiderProcessor.PARENT_PATH = parentPath + "/";
        else
            SankakuSpiderProcessor.PARENT_PATH = parentPath;

        if (!new File(SankakuSpiderProcessor.PARENT_PATH).exists()) {
            throw new RuntimeException("PARENT_PATH :" + SankakuSpiderProcessor.PARENT_PATH + " DO NOT EXISTS");
        }
        File son = new File(SankakuSpiderProcessor.PARENT_PATH + tag);
        if (!son.exists()) {
            son.mkdir();
            new File(son.getPath() + "/pic").mkdir();
            new File(son.getPath() + "/vid").mkdir();
            try {
                new File(son.getPath() + "/"+ARTIST_INFO_FILENAME).createNewFile();
                File artwork = new File(son.getPath() + "/"+ARTWORK_INFO_FILENAME);
                        artwork.createNewFile();
                FileUtils.writeStringToFile(artwork,"[]","UTF8",false);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    public static void main(String[] args) {
    getArtworkInfoList("21yc (september breeze)");


    }
}
