package pers.missionlee.webmagic.spider.sankaku;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
import pers.missionlee.webmagic.spider.sankaku.info.SankakuFileUtils;
import pers.missionlee.webmagic.spider.sankaku.info.UpdateInfo;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuDownloadSpider;
import pers.missionlee.webmagic.spider.sankaku.pageprocessor.SankakuNumberSpider;
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

    private String[] getUrlStringArray(String rootPath,String artistName, boolean official, boolean update) {
        if (update) {
            String[] urls = new String[1];
            urls[0] = SITE_ORDER_PREFIX.DATE.getPrefix(artistName, official) + 1;
            return urls;
        } else {
            int artworkNum = 0;
            int retry = 0;
            while (artworkNum < 1 && retry < 3) {
                try {
                    artworkNum = getRealNumOfArtist(rootPath,artistName, official);
                } catch (Exception e) {
                    e.printStackTrace();
                    retry++;
                }
            }
            return urlGenertor(artistName, official, artworkNum);
        }
    }



    public int updateOne(String rootPath, String artistName, boolean official, int threadNum) throws IOException {
        int num = 0;
        if (updateInfo == null)
            updateInfo = new UpdateInfo();
        if (updateInfo.needUpdate(artistName)) {

            logger.info("NEED :  artist: " + artistName + "UPDATED:" + updateInfo.getUpdateDate(artistName));
            int numberNow = getRealNumOfArtist(rootPath,artistName, official);
            int numberStored = SankakuFileUtils.getArtworkNumber(new File(rootPath + artistName));
            logger.info("numberNow: " + numberNow + " numberStored: " + numberStored);
            if (numberNow > numberStored) { // 如果需要更新的超过1个 开启更新
                String startPage = getUrlStringArray(rootPath,artistName, false, true)[0];
                num = startDownloadSpider(rootPath, artistName, threadNum, startPage);
                logger.info("update num: " + num);
                if (numberNow < 2000 && (num < numberNow)) {
                    num = fullDownloadOne(rootPath, artistName, official, threadNum);
                }

            }
            updateInfo.update(artistName);
            updateInfo.writeUpdateInfo(updateInfoFile);
        } else {
            logger.info("作者: " + artistName + "\t更新时间:" + updateInfo.getUpdateDate(artistName));
        }
        return num;
    }

    public void updateAll(String rootPath, boolean official, int threadNum) throws IOException {
        if (root.exists()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    updateOne(rootPath, files[i].getName(), official, threadNum);
                }
            }
        }
    }

    public int fullDownloadOne(String rootPath, String artistName, boolean official, int threadNum) throws IOException {
        String[] urls = getUrlStringArray(rootPath,artistName, official, false);
        return startDownloadSpider(rootPath, artistName, threadNum, urls);
    }

    //    public static void fullDownloadAll(String rootPath, boolean official, int threadNum, String... artistname) {
//
//    }
    public void fullDownloadWithInnerList(String rootPath, boolean official, int threadNum) throws IOException {
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
            // 3 遍历下载
            fullDownloadOne(rootPath, key, official, threadNum);
            // 原本会检测下载数量是否不足，但是update机制完善之后，可以通过update方式来补充下载缺漏
            storageMap.remove(key);
            rewriteTodoList(nameListFile, storageMap);
        }
    }


    /**
     * @Return int 返回当前作者作品总量
     */
    private int startDownloadSpider(String rootPath, String artistName, int threadNum, String... urls) throws IOException {
        // 1. 创建一个下载用 页面处理器 SankakuDownloadSpider
        SankakuDownloadSpider sankakuSpider = new SankakuDownloadSpider(site, rootPath, artistName);
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
            processor.fullDownloadOne(rootPath, artistName, false, threadNum);
        } else if (type == RunType.RUN_WITH_ARTIST_NAMElIST) {
            processor.fullDownloadWithInnerList(rootPath, false, threadNum);
        } else if (type == RunType.UPDATE_ARTIST) {
            processor.updateAll(rootPath, false, threadNum);
        } else if (type == RunType.RUN_WITH_COPYRIGHT_NAME) {
            processor.fullDownloadOne(rootPath, artistName, true, threadNum);
        } else if (type == RunType.RUN_WITH_COPYRIGHT_NAMELIST) {
            processor.fullDownloadWithInnerList(rootPath, true, threadNum);
        } else if (type == RunType.UPDATE_COPYRIGHT) {
            processor.updateAll(rootPath, true, threadNum);
        }
    }
    public static void extractSamplePic(String rootPath) throws IOException {
        File rootFile = new File(rootPath);
        SankakuFileUtils.extractSamplePic(rootFile);
    }
    public static void main(String[] args) {
        try {
            runProcessor(RunType.RUN_WITH_ARTIST_NAMElIST, "D:\\santest", "", 5);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        runOffical("D:\\sankakuofficial", "league of legends", 941, 4);

//        String help = "| [TYPE] \n" +
//                "|- run \t[root path] [aim list] [thread num] [asc/desc]\n"+
//                "|- autoRun \t [rootPath = D:\\sankaku] [aim list = C:\\Users\\Administrator\\Desktop\\sankaku\\20190313.md] [thread num = 4] [asc]\n" +
//                "|- update\t [root path] [thread num]\n" +
//                "|- autoUpdate\t [rootPath = D:\\sankaku] [thread num = 4]";
//        if(args.length ==0 || args[0].equals("help")){
//            System.out.println(help);
//        }else{
//            if(args[0].equals("run") && args.length==6){
//                boolean desc = true;
//                if(args[4].equals("asc"))
//                    desc =false;
//
//                runWithNameList(args[1],args[2],Integer.valueOf(args[3]),desc,false);
//            }else if(args[0].equals("update")&&args.length==3){
//
//            }else
//            if(args[0].equals("autoRun")){
//                runWithNameList("D:\\sankaku","C:\\Users\\Administrator\\Desktop\\sankaku\\20190313.md",4,true,false);
//            }else if(args[0].equals("autoUpdate")){
//                try {
//                    updateFullPath("D:\\sankaku", 4);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }else{
//                System.out.println(help);
//            }
//        }
    }
}