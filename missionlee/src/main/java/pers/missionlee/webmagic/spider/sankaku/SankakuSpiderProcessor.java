package pers.missionlee.webmagic.spider.sankaku;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtistInfo;
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

    private  Logger logger = LoggerFactory.getLogger(SankakuSpiderProcessor.class);


    private SankakuDownloadSpider diarySankakuSpider;
    public SankakuInfoUtils sankakuInfoUtils;
    public File root;
    public  File updateInfoFile;
    public  UpdateInfo updateInfo;
    // 构造参数


    private SankakuSpiderProcessor(String rootPath) throws IOException {
        root = new File(rootPath);
        updateInfoFile = new File(rootPath + "update.json");
        updateInfo = UpdateInfo.getUpdateInfo(updateInfoFile);

    }

    /**
     * 获取 artwork number
     */
    private  Integer getRealNumOfArtist(String artistName, boolean offical) {
        String url = BASE_SITE + urlFormater(artistName, offical);
        SankakuNumberSpider spider = new SankakuNumberSpider(site, offical);
        Spider.create(spider).addUrl(url).thread(1).run();
        return spider.getNum();
    }

    /**
     * 给定 作者名称 与 是否 offical 是否 为更新 来获取 目标url
     * 其中 整体下载的情况下（非update） 可以通过爬虫自动获取 目标数量
     */
    private  String[] urlGenertor(String artist, boolean offical, boolean update) {
        String[] urls;
        if (update) {
            urls = new String[1];
            urls[0] = SITE_ORDER_PREFIX.DATE.getPrefix(artist, offical) + 1;
        } else {
            int artworkNum = 0;
            int retry = 0;
            String BaseTagAsc = SITE_ORDER_PREFIX.TAG_COUNT_ASC.getPrefix(artist, offical);
            String BaseTagDec = SITE_ORDER_PREFIX.TAG_COUNT_DEC.getPrefix(artist, offical);
            String BaseDate = SITE_ORDER_PREFIX.DATE.getPrefix(artist, offical);
            String BasePopular = SITE_ORDER_PREFIX.POPULAR.getPrefix(artist, offical);
            String BaseQurlity = SITE_ORDER_PREFIX.QUALITY.getPrefix(artist, offical);
//            String
            while (artworkNum < 1 && retry < 3) {
                try {
                    artworkNum = getRealNumOfArtist(artist, offical);
                } catch (Exception e) {
                    e.printStackTrace();
                    retry++;
                }
            }

            if (artworkNum > 2000) { // 2000+ 情况遍历 tag升降序 + date最新 + popular最高 + quality 最高
                urls = new String[250];
                for (int i = 0; i < 50; i++) {
                    urls[i] = BaseTagAsc + (i + 1);
                    urls[i + 50] = BaseTagDec + (i + 1);
                    urls[i + 100] = BaseDate + (i + 1);
                    urls[i + 150] = BaseQurlity + (i + 1);
                    urls[i + 200] = BasePopular + (i + 1);
                }
            } else {
                int pageNum = ((Double) (Math.ceil((new Double(artworkNum)) / 20))).intValue();
                urls = new String[pageNum];
                if (pageNum > 50) {// 1001~2000 tag升降序

                    for (int i = 0; i < pageNum; i++) {
                        if ((i + 1) <= 50) {
                            urls[i] = BaseTagDec + (i + 1);
                        } else {
                            urls[i] = BaseTagAsc + (i - 49);
                        }
                    }

                } else {// 1~999 date遍历

                    for (int i = 0; i < pageNum; i++) {
                        urls[i] = BasePopular + (i + 1);
                    }
                }
            }
        }
        return urls;
    }

    public  int updateOne(String rootPath, String artistName, boolean offical, int threadNum) throws IOException {
        int num = 0;
        if (updateInfo == null)
            updateInfo = new UpdateInfo();
        if (updateInfo.needUpdate(artistName)) {

            logger.info("NEED :  artist: " + artistName + "UPDATED:" + updateInfo.getUpdateDate(artistName));
            int numberNow = getRealNumOfArtist(artistName, offical);
            int numberStored = SankakuInfoUtils.getArtworkNumber(new File(rootPath+artistName));
            logger.info("numberNow: " + numberNow + " numberStored: " + numberStored);
            if (numberNow > numberStored) { // 如果需要更新的超过1个 开启更新
                String startPage = urlGenertor(artistName, false, true)[0];
                num = startDownloadSpider(rootPath, artistName, threadNum, startPage);
                logger.info("update num: " + num);
                if (numberNow < 2000 && (num < numberNow)) {
                    num = fullDownloadOne(rootPath,artistName,offical,threadNum);
                }

            }
            updateInfo.update(artistName);
            System.out.println("XXXXXXXXXXX");
            updateInfo.writeUpdateInfo(updateInfoFile);

        } else {
            logger.info("already updated artist: " + artistName + "UPDATED:" + updateInfo.getUpdateDate(artistName));
        }
        return num;
    }

    public  void updateAll(String rootPath, boolean offical, int threadNum) throws IOException {

        if (root.exists()) {
            File[] files = root.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                     updateOne(rootPath, files[i].getName(), offical, threadNum);
                }

            }
        }
    }

    public  int fullDownloadOne(String rootPath, String artistName, boolean offical, int threadNum) throws IOException {
        String[] urls = urlGenertor(artistName, offical, false);
        return startDownloadSpider(rootPath, artistName, threadNum, urls);
    }

//    public static void fullDownloadAll(String rootPath, boolean offical, int threadNum, String... artistname) {
//
//    }
    public  void fullDownloadWithInnerList(String rootPath,boolean offical,int threadNum) throws IOException {
        File nameListFile = new File(rootPath+"name.md");
        String nameListString = FileUtils.readFileToString(nameListFile, "UTF8");
        String[] nameListArray = nameListString.split("\n");
        int length = nameListArray.length;
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
        Map<String, Integer> sortedMap = sortNameList(nameListMap, true);
        rewriteTodoList(nameListFile, sortedMap);
        Map<String, Integer> storageMap = new LinkedHashMap<String, Integer>(sortedMap);
        Set<String> set = sortedMap.keySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()){
            String key = (String) iterator.next();
            fullDownloadOne(rootPath,key,offical,threadNum);
            // 原本会检测下载数量是否不足，但是update机制完善之后，可以通过update方式来补充下载缺漏
            storageMap.remove(key);
            rewriteTodoList(nameListFile, storageMap);
//            updateInfo.update(key);
//            updateInfo.writeUpdateInfo(updateInfoFile);
        }

    }


    /**
     * @Return int 返回当前作者作品总量
     */
    private int startDownloadSpider(String rootPath, String artistName, int threadNum, String... urls) throws IOException {
        SankakuDownloadSpider sankakuSpider = new SankakuDownloadSpider(site, rootPath, artistName);
        Spider spider = Spider.create(sankakuSpider);
        spider.addUrl(urls).thread(threadNum).run();
        // TODO: 2019/3/4  以上内容运行结束之后，重构对应作者的artistinfo
        ArtistInfo artistInfo = SankakuInfoUtils.freshArtistInfo(sankakuSpider.artworkInfos, rootPath + artistName, artistName);
        return artistInfo.getArtworkNum();
    }


    public static void runProcessor(RunType type, String rootPath,String artistName,  int threadNum) throws IOException {
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        SankakuSpiderProcessor processor = new SankakuSpiderProcessor(rootPath);
        if (type == RunType.RUN_WITH_ARTIST_NAME) {
            processor.fullDownloadOne(rootPath,artistName,false,threadNum);
        } else if (type == RunType.RUN_WITH_ARTIST_NAMElIST) {
            processor.fullDownloadWithInnerList(rootPath,false,threadNum);
        } else if (type == RunType.UPDATE_ARTIST) {
            processor.updateAll(rootPath, false, threadNum);
        }else if (type == RunType.RUN_WITH_COPYRIGHT_NAME) {
            processor.fullDownloadOne(rootPath,artistName,true,threadNum);
        } else if (type == RunType.RUN_WITH_COPYRIGHT_NAMELIST) {
            processor.fullDownloadWithInnerList(rootPath, true,threadNum);
        }  else if (type == RunType.UPDATE_COPYRIGHT) {
            processor.updateAll(rootPath, true, threadNum);
        }
    }

    public static void main(String[] args) {
        try {
            runProcessor(RunType.RUN_WITH_ARTIST_NAMElIST,"D:\\sankaku","",4);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        runOffical("D:\\sankakuoffical", "league of legends", 941, 4);

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