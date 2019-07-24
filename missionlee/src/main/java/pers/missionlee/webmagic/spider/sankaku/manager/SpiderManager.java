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

//    public enum UpdateType {
//        SANKAKU,
//        SANKAKUOFFICIAL,
//        IDOL,
//        IDOLOFFICIAL
//    }

    public  void update(SourceManager sourceManager, SourceManager.SourceType updateSourceType, boolean getAll,int minPriority) throws IOException {
        if (updateSourceType == SourceManager.SourceType.SANKAKU) {
            Map<String, Integer> artists = sourceManager.getSankakuArtistListByJson();
            Set<String> artistNames = artists.keySet();
            for (String name :
                    artistNames) {

                if (!sourceManager.isUpdated(SourceManager.SourceType.SANKAKU, name,minPriority)) {
                    SpiderTask task = startSpider(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.UPDATE, name, false,getAll);
                    int downloaded = task.downloaded;
                    logger.info("更新清空：作者[" + name + "]本次更新 " + downloaded);
                    sourceManager.update(SourceManager.SourceType.SANKAKU, name,fileNameGenerator(name), downloaded);
                }
            }
        }
    }

    public  void runWithChromeDir(SourceManager sourceManager, SourceManager.SourceType sourceType, SpiderTask.TaskType taskType, String chromeBookmarkDirName, boolean official) throws IOException {
        ChromeBookmarksReader reader = new ChromeBookmarksReader(ChromeBookmarksReader.defaultBookmarkpath);
        List<Map> artistListInChrome = reader.getBookMarkListByDirName(chromeBookmarkDirName);

//        Set<String> downloadedDir = sourceManager.getSankakuArtistsListByDir().keySet();
//        Set<String> downloadedJson = sourceManager.getSankakuArtistListByJson().keySet();
        List<String> nameList = new ArrayList<String>();
        for (Map bookmark : artistListInChrome
        ) {
            System.out.println(bookmark.get("url"));
            String tmpName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
//            String artistFileName = fileNameGenerator(tmpName);
            // 如果 tmpName 是以 . 结尾的 实际文件是没有
//            if (!downloadedDir.contains(artistFileName)) {
//                nameList.add(tmpName);
//            }
            // TODO: 2019-04-26 有些作者名称以 . 结尾 创建目录的时候 最后一个 . 会被忽略，不如直接查看作品数量
            if(sourceManager.getArtworkNum(SourceManager.SourceType.SANKAKU,fileNameGenerator(tmpName))==0){
                nameList.add(tmpName);
            }
        }
        logger.info("初始化目标列表["+nameList.size()+"]：" + nameList);
        for (String name :
                nameList) {
            startSpider(sourceManager, sourceType, taskType, name, official,true);
        }
    }

    public  SpiderTask startSpider(SourceManager sourceManager, SourceManager.SourceType sourceType, SpiderTask.TaskType taskType, String artistName, boolean official,boolean getAll) throws IOException {
        SpiderTaskFactory factory = new SpiderTaskFactory(sourceManager);
        SpiderTask task = factory.getSpiderTask(sourceType, artistName,fileNameGenerator(artistName), official, taskType,getAll,sourceManager);
        SourceSpiderRunner runner = new SourceSpiderRunner();
        try {
              runner.runTask(task);
        } catch (Exception e) {
            logger.info("获取作者作品数量失败，爬虫无法执行");
        }
        return task;
    }

    public  void runWithNameList(String filePath,SourceManager sourceManager) throws IOException {

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
        Set<String> setForLoop = new HashSet<String>(set);
        Iterator iterator = setForLoop.iterator();
        while (iterator.hasNext()) {
            String key =  iterator.next().toString();
            startSpider(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.NEW, key, false,true);
            nameListMap.remove(key);
            rewriteTodoList(nameListFile, nameListMap);
        }
    }

    public static void main(String[] args) throws IOException {

        SourceManager sourceManager = new SourceManager("E:\\ROOT");
        SpiderManager spiderManager = new SpiderManager();
//        spiderManager.startSpider(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.UPDATE,"kamadeva",false,true);
//        spiderManager.runWithChromeDir(sourceManager, SourceManager.SourceType.SANKAKU, SpiderTask.TaskType.NEW, "san6", false);
         spiderManager.update(sourceManager, SourceManager.SourceType.SANKAKU,true,1);
//         spiderManager.runWithNameList("D:\\sankaku\\name.md",sourceManager);
    }
}
