package pers.missionlee.webmagic.spider.sankaku.manager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.SpiderUtils;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-12 14:56
 * // TODO: 2019-04-13  [普通与official只需要调整参数即可]
 * 1. 借助ChromeBookmarksReader 指定某个目录，发现目录下存在的artist
 * 2. 指定更新某个目录
 */
public class SpiderManager extends SpiderUtils {
    static Logger logger = LoggerFactory.getLogger(SpiderManager.class);
    public enum UpdateType{
        SANKAKU,
        SANKAKUOFFICIAL,
        IDOL,
        IDOLOFFICIAL
    }
    public static void update(SourceManager sourceManager,UpdateType updateType){
        if(updateType == UpdateType.SANKAKU){
            Map<String,Integer> artists= sourceManager.getSankakuArtistList();
            Set<String> artistNames = artists.keySet();
            for (String name :
                    artistNames) {
                updateOne(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.UPDATE,name);
            }
        }
    }
    private static void updateOne(SourceManager sourceManager, SourceManager.SourceType sourceType, SpiderTask.TaskType taskType,String artistname){

    }
    public static void runWithChromeDir(SourceManager sourceManager, SourceManager.SourceType sourceType, SpiderTask.TaskType taskType,String chromeBookmarkDirName,boolean official) throws IOException {
        ChromeBookmarksReader reader = new ChromeBookmarksReader(ChromeBookmarksReader.defaultBookmarkpath);
        List<Map> artistList = reader.getBookMarkListByDirName(chromeBookmarkDirName);

        Set<String> downloaded = sourceManager.getSankakuArtistList().keySet();
        List<String> nameList = new ArrayList<String>();
        for (Map bookmark:artistList
             ) {
            String tmpName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
            if(!downloaded.contains(tmpName)){
                nameList.add(tmpName);
            }
        }
        logger.info("初始化目标列表："+nameList);
        for (String name :
                nameList) {
            autoRun(sourceManager,sourceType,taskType,name,official);
        }
    }
    public static void autoRun(SourceManager sourceManager, SourceManager.SourceType sourceType, SpiderTask.TaskType taskType, String artistName,boolean official) throws IOException {
        SpiderTaskFactory factory = new SpiderTaskFactory(sourceManager);
        SpiderTask task = factory.getSpiderTask(sourceType, artistName, official, taskType);
        SourceSpiderRunner runner = new SourceSpiderRunner();
        runner.runTask(task);
    }
    @Deprecated
    public static void runWithNameList(String filePath) throws IOException {
        SourceManager sourceManager = new SourceManager("D:\\ROOT");

        File nameListFile = new File(filePath);
        String nameListString = FileUtils.readFileToString(nameListFile, "UTF8");
        String[] nameListArray = nameListString.split("\n");
        int length = nameListArray.length;

        // 2. 获取排序后的 name list
        Map<String, Integer> nameListMap = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < length; i++) {
            String str = nameListArray[i].trim();
            if (!StringUtils.isEmpty(str)) {
                int lastIndex = str.lastIndexOf(" ");
                if (lastIndex != -1) {
                    String name = str.substring(0, str.lastIndexOf(" ")).trim();
                    String num = str.substring(str.lastIndexOf(" ")).trim().replaceAll("\\(", "").replaceAll("\\)", "").replaceAll(",", "");
                    nameListMap.put(name, Integer.valueOf(num));
                }
            }
        }
        Set<String> set = nameListMap.keySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            autoRun(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.NEW,key,false);
            nameListMap.remove(key);
            rewriteTodoList(nameListFile, nameListMap);
        }
    }

    public static void main(String[] args) throws IOException {

        SourceManager sourceManager = new SourceManager("D:\\ROOT");
        runWithChromeDir(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.NEW,"download2",false);
    }
}
