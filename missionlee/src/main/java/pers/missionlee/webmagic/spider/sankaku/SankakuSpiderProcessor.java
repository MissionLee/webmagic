package pers.missionlee.webmagic.spider.sankaku;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.SankakuFileUtils;
import pers.missionlee.webmagic.spider.sankaku.info.UpdateInfo;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuDownloadSpider;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuNumberSpider;
import pers.missionlee.webmagic.spider.sankaku.task.SankakuSpiderTask;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:10
 */
public class SankakuSpiderProcessor extends SankakuBasicUtils {

    private Logger logger = LoggerFactory.getLogger(SankakuSpiderProcessor.class);

    public File root;
    public File updateInfoFile;
    public UpdateInfo updateInfo;
    // 构造参数


    private SankakuSpiderProcessor(String rootPath) throws IOException {
        root = new File(rootPath);
        updateInfoFile = new File(rootPath + "update.json");
        if(!updateInfoFile.exists())
            updateInfoFile.createNewFile();
        updateInfo = UpdateInfo.getUpdateInfo(updateInfoFile);

    }

    /**
     * 获取 artwork number
     */
    private Integer getRealNumOfArtist(String rootPath,String artistName, boolean official) {
        String url = BASE_SITE + urlFormater(artistName, official);
        SankakuNumberSpider spider = new SankakuNumberSpider(site,rootPath,artistName, official);
        Spider.create(spider).addUrl(url).thread(1).run();
        return spider.getNum();
    }

    private String[] getUrlStringArray(String rootPath,String artistName, boolean official, boolean update,SankakuSpiderTask task) {
        if (update) {
            task.currentDownloadTask.total=9999;
            String[] urls = new String[1];
            urls[0] = SITE_ORDER_PREFIX.DATE.getPrefix(artistName, official) + 1;
            return urls;
        } else {
            int artworkNum = 0;
            int retry = 0;
            while (artworkNum < 1 && retry < 3) {
                try {
                    artworkNum = getRealNumOfArtist(rootPath,artistName, official);
                    task.currentDownloadTask.total=artworkNum;
                } catch (Exception e) {
                    e.printStackTrace();
                    retry++;
                }
            }
            return urlGenertor(artistName, official, artworkNum);
        }
    }



    public int updateOne(String rootPath, String artistName, boolean official, int threadNum,SankakuSpiderTask task) throws IOException {
        if(task ==null){
            task = new SankakuSpiderTask(rootPath,threadNum,official,"update");

        }
        task.resetDownloadTask(artistName);
        int num = 0;
        if (updateInfo == null)
            updateInfo = new UpdateInfo();
        if (updateInfo.needUpdate(artistName)) {

            logger.info("NEED :  artist: " + artistName + "UPDATED:" + updateInfo.getUpdateDate(artistName));
            int numberNow = getRealNumOfArtist(rootPath,artistName, official);
            task.currentDownloadTask.total=numberNow;
            int numberStored = SankakuFileUtils.getArtworkNumber(new File(rootPath + artistName));
            task.currentDownloadTask.stored=numberStored;
            logger.info("numberNow: " + numberNow + " numberStored: " + numberStored);
            if (numberNow > numberStored) { // 如果需要更新的超过1个 开启更新
                String startPage = getUrlStringArray(rootPath,artistName, false, true,task)[0];
                num = startDownloadSpider(rootPath, artistName, threadNum,task, startPage);
                logger.info("update num: " + num);
                if (numberNow < 2000 && (num < numberNow)) {
                    num = fullDownloadOne(rootPath, artistName, official, threadNum,task);
                }

            }
            updateInfo.update(artistName);
            updateInfo.writeUpdateInfo(updateInfoFile);
        } else {
            logger.info("作者: " + artistName + "\t更新时间:" + updateInfo.getUpdateDate(artistName));
        }
        return num;
    }
    public void updateAll(SankakuSpiderTask task) throws IOException {
        updateAll(task.rootPath,task.offical,task.threadNum,task);
    }
    public void updateAll(String rootPath, boolean official, int threadNum,SankakuSpiderTask task) throws IOException {
        if (root.exists()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {

                    updateOne(rootPath, files[i].getName(), official, threadNum,task);
                }
            }
        }
    }

    public int fullDownloadOne(String rootPath, String artistName, boolean official, int threadNum,SankakuSpiderTask task) throws IOException {
        if(task==null){
            task = new SankakuSpiderTask(rootPath,threadNum,official,"new");

        }
        task.resetDownloadTask(artistName);
        String[] urls = getUrlStringArray(rootPath,artistName, official, false,task);
        task.currentDownloadTask.stored = SankakuFileUtils.getArtworkNumber(new File(task.rootPath+task.currentDownloadTask.artistName));
        return startDownloadSpider(rootPath, artistName, threadNum,task, urls);
    }

    //    public static void fullDownloadAll(String rootPath, boolean official, int threadNum, String... artistname) {
//
//    }
    public void fullDownloadWithInnerList(SankakuSpiderTask task) throws IOException {
        fullDownloadWithInnerList(task.rootPath,task.offical,task.threadNum,task);
    }
    public void fullDownloadWithInnerList(String rootPath, boolean official, int threadNum,SankakuSpiderTask task) throws IOException {
        if(task ==null){
            task = new SankakuSpiderTask(rootPath,threadNum,official,"new");
        }
        // 1. 获取约定的 name.md 文件
        File nameListFile = new File(rootPath + "name.md");
        String nameListString = FileUtils.readFileToString(nameListFile, "UTF8");
        String[] nameListArray = nameListString.split("\n");
        int length = nameListArray.length;
        // 2. 获取排序后的 name list
        Map<String, Integer> nameListMap = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < length; i++) {
            String str = nameListArray[i].trim();
            if (!StringUtils.isEmpty(str))
                if (!str.contains("run")) {
                    while (str.startsWith("//"))
                        str = str.substring(2).trim();
                    int lastIndex = str.lastIndexOf(" ");
                    if (lastIndex != -1) {
                        String name = str.substring(0, str.lastIndexOf(" ")).trim();
                        String num = str.substring(str.lastIndexOf(" ")).trim().replaceAll("\\(", "").replaceAll("\\)", "").replaceAll(",", "");
                        if (Pattern.matches("\\d+", num)) {
                            nameListMap.put(name, Integer.valueOf(num));
                        }
                    }
                }
        }
        Map<String, Integer> sortedMap = sortNameList(nameListMap, false);
        // 2+ 排序后的列表写回目录
        rewriteTodoList(nameListFile, sortedMap);
        Map<String, Integer> storageMap = new LinkedHashMap<String, Integer>(sortedMap);
        Set<String> set = sortedMap.keySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            //task.resetDownloadTask(key);
            // 3 遍历下载
            fullDownloadOne(rootPath, key, official, threadNum,task);
            // 原本会检测下载数量是否不足，但是update机制完善之后，可以通过update方式来补充下载缺漏
            storageMap.remove(key);
            rewriteTodoList(nameListFile, storageMap);
        }
    }


    /**
     * @Return int 返回当前作者作品总量
     */
    private int startDownloadSpider(String rootPath, String artistName, int threadNum,SankakuSpiderTask task, String... urls) throws IOException {
        // 1. 创建一个下载用 页面处理器 SankakuDownloadSpider
        SankakuDownloadSpider sankakuSpider = new SankakuDownloadSpider(site, rootPath, artistName,task);
        Spider spider = Spider.create(sankakuSpider);
        spider.addUrl(urls).thread(threadNum).run();
        // 2. 更新作者信息
        ArtistInfo artistInfo = SankakuFileUtils.freshArtistInfo(sankakuSpider.artworkInfos, rootPath, artistName);
        // 运行结束后 清理一下文件夹，删除错误文件
        SankakuFileUtils.cleanErrorFilesForArtist(rootPath,artistName);
        return artistInfo.getArtworkNum();
    }


    public static void runProcessor(RunType type, String rootPath, String artistName, int threadNum) throws IOException {
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        SankakuSpiderProcessor processor = new SankakuSpiderProcessor(rootPath);
        if (type == RunType.RUN_WITH_ARTIST_NAME) {
            processor.fullDownloadOne(rootPath, artistName, false, threadNum,null);
        } else if (type == RunType.RUN_WITH_ARTIST_NAMElIST) {
            processor.fullDownloadWithInnerList(rootPath, false, threadNum,null);
        } else if (type == RunType.UPDATE_ARTIST) {
            processor.updateAll(rootPath, false, threadNum,null);
        } else if (type == RunType.RUN_WITH_COPYRIGHT_NAME) {
            processor.fullDownloadOne(rootPath, artistName, true, threadNum,null);
        } else if (type == RunType.RUN_WITH_COPYRIGHT_NAMELIST) {
            processor.fullDownloadWithInnerList(rootPath, true, threadNum,null);
        } else if (type == RunType.UPDATE_COPYRIGHT) {
            processor.updateAll(rootPath, true, threadNum,null);
        }
    }
    public static void extractSamplePic(String rootPath) throws IOException {
        File rootFile = new File(rootPath);
        SankakuFileUtils.extractSamplePic(rootFile);
    }
    public static void autoRun(String configPath) throws IOException {
        SankakuSpiderTask task =SankakuSpiderTask.buildTask(configPath);
        SankakuSpiderProcessor processor = new SankakuSpiderProcessor(task.rootPath);
        if(task.taskType.equals("new")){
            processor.fullDownloadWithInnerList(task);
        }else{
            processor.updateAll(task);
        }

    }
    public static void main(String[] args) throws IOException {
        autoRun("F:\\sankaku\\taskConfig.json");
//        try {
//            runProcessor(RunType.RUN_WITH_ARTIST_NAMElIST, "F:\\sankaku", "", 4);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}