package pers.missionlee.webmagic.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-08 22:52
 */
public class ChromeBookmarksReader {
    static Logger logger = LoggerFactory.getLogger(ChromeBookmarksReader.class);
    public static class BookmarkNode{
        public String date_added;
        public String id;
        public Map<String,String> meta_info;
        public String name;
        public String type;
        public String url;
    }
    public static class BookmarkDir{
        public Map[] children; // children 为一个map数组，里面的map，
        public String date_added;
        public String date_modified;
        public String id;
        public String name;
        public String type;
    }
    public static class BookmarkRoot{
        public String checksum;
        public Map<String,Map> roots;
        public String version;
    }

    private String path;
   // private Map root;
    //private Map<String,BookmarkDir> bookmarkDirMap;
    private Map<String,Map> roots;
    private Map<String, List<Map>> dirs;
    /**
     * 简单约定：使用chrome标准 json形式的书签源数据
     *          根书签目录下面不直接放具体书签（因为解析的时候没有考虑）
     *          没有使用上面的几个内部类，因为写的时候发现 （JSONObject/JSONArray）更加灵活好用一些
     * */
    public ChromeBookmarksReader(String path) throws IOException {
        this.path = path;
        //this.bookmarkDirMap = new HashMap<String, BookmarkDir>();
        this.roots = new HashMap<String, Map>();
        this.dirs = new HashMap<String,List< Map>>();
        init();
    }
    public ChromeBookmarksReader() throws IOException {
        this(defaultBookmarkpath);
    }
    public List<Map> getBookMarkListByDirName(String dirName){
        return dirs.get(dirName);
    }
    private void init() throws IOException {
        String jsonString = FileUtils.readFileToString(new File(path),"utf8");
        JSONObject jsonRoots = (JSONObject) (((Map)JSON.parse(jsonString)).get("roots"));
        Set<String> rootKeys = jsonRoots.keySet();
        for (String key :
                rootKeys) {
            Map tmpMap = (Map)JSON.parse(jsonRoots.get(key).toString());
            this.roots.put(key,tmpMap);
            extractFromTmpMap(key,tmpMap);
        }
    }
    private void extractFromTmpMap(String key,Map tmp){
        String theKey = key;
        if(tmp.containsKey("name")){
            theKey = tmp.get("name").toString();
        }
        if(tmp.containsKey("type")){
            if(tmp.get("type").equals("folder")){
                // 如果当前 tmp map 的类型为 folder

                List<Map> thisNode = new ArrayList<Map>();
                // 1.遍历 children
                if(tmp.containsKey("children")){
                    JSONArray children = (JSONArray)JSON.parse(tmp.get("children").toString());
                    for (Object child :
                            children) {
                        Map childMap = (Map)JSON.parse(child.toString());
                        if(childMap.containsKey("type")){
                            if(childMap.get("type").equals("folder")){
                                extractFromTmpMap("",childMap);
                            }else if(childMap.get("type").equals("url")){
                                thisNode.add(childMap);
                            }
                        }
                    }
                }
                System.out.println("key:"+theKey);
                dirs.put(theKey,thisNode);
            }
        }
    }
    public Set<String> getArtistNameFromBookNarkDir(String bookmarkDirName) throws IOException {
        ChromeBookmarksReader reader = new ChromeBookmarksReader(defaultBookmarkpath);
        Set<String> names = new HashSet<String>();
        List<Map> aimDir = reader.getBookMarkListByDirName(bookmarkDirName);
        for(Map bookmark:aimDir){
            if(bookmark.get("url").toString().contains("tags=")) {
                String codedName = bookmark.get("url").toString().split("tags=")[1];
                String realName = SpiderUtils.urlDeFormater(codedName);
                names.add(realName);
            }
        }
            return names;
    }
    public static String defaultBookmarkpath = "C:\\Documents and Settings\\Administrator\\Local Settings\\Application Data\\Google\\Chrome\\User Data\\Default\\Bookmarks";
    public static void extractNonDownloadedSankakuNameList(String rootPath, String bookmarkDirName, String aimFilePath) throws IOException {
        SourceManager sourceManager = new SourceManager(rootPath);
        Set<String> downloaded = sourceManager.getSankakuArtistListByJson().keySet();
        ChromeBookmarksReader reader = new ChromeBookmarksReader(defaultBookmarkpath);
        List<Map> aimDir = reader.getBookMarkListByDirName(bookmarkDirName);
        StringBuffer buffer = new StringBuffer();
        for(Map bookmark:aimDir){
            if(bookmark.get("url").toString().contains("tags=")){
                String urlName = bookmark.get("url").toString().split("tags=")[1];
                String tmpName = SpiderUtils.urlDeFormater(urlName);
                logger.info("URL: "+urlName+"\tDEFORMATER: "+tmpName);
                if(!downloaded.contains(tmpName)){
                    buffer.append(tmpName+" 1\n");
                }
            }else{
                System.out.println(bookmark.get("url").toString());
            }

        }
          FileUtils.writeStringToFile(new File(aimFilePath),buffer.toString(),"utf8",false);
    }

    public static void main(String[] args) throws IOException {
        extractNonDownloadedSankakuNameList("E:\\ROOT","san6","C:\\Users\\Administrator\\Desktop\\san6.md");
//        extractNonDownloadedSankakuNameList("D:\\ROOT","download2","D:\\ROOT\\bookmarks\\sankaku-download2.md");
    }
}
