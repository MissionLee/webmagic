package pers.missionlee.bili.starter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.bili.spider.BidPageProcessor;
import pers.missionlee.chan.pagedownloader.MixDownloader;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BiliStarter {
    /**
     * 下面几个方法是路径操作工具类
     * */
    public List<String> getBidsFromDisk(){
        File root = new File(biliSetting.ROOT);
        String[] fileNames = root.list();
        List<String> bids = new ArrayList<>();
        for (int i = 0; i < fileNames.length; i++) {
            if("tmp".equals(fileNames[i])){

            }else{
                bids.add(fileNames[i]);
            }
        }
        return bids;
    }



    /**
     *
     *
     *
     * */
    Logger logger = LoggerFactory.getLogger(BiliStarter.class);
    public BiliSetting biliSetting;
    public ChromeBookmarksReader reader;

    public BiliStarter(String settingPath) throws IOException {
        File setting = new File(settingPath);
        String settingString = null;
        settingString = FileUtils.readFileToString(setting, "UTF8");
        this.biliSetting = JSON.parseObject(settingString, BiliSetting.class);
        MixDownloader.restartLimit = biliSetting.CHROME_RESTART_LIMIT;
        this.reader = new ChromeBookmarksReader(biliSetting.CHROME_BOOKMARK_PATH);
    }

    public void downloadWithChromeFolder(String folderName) {
        List<Map> urls = reader.getBookMarkListByDirName(folderName);
        List<String> bids = new ArrayList<>();
        for (Map bookMar : urls) {
            String url = bookMar.get("url").toString();
            if (url.contains("space.bilibili.com")) {
//                   https://space.bilibili.com/179308916?spm_id_from=333.337.0.0
                String bid = null;
                if (url.contains("?")) {
                    bid = url.substring(url.lastIndexOf("/") + 1, url.indexOf("?"));
                } else {
                    bid = url.substring(url.lastIndexOf("/") + 1);
                }
                bids.add(bid);
            }
        }
        Iterator<String> iterator = bids.listIterator();
        int index = 0;
        while (iterator.hasNext()) {
            String bid = iterator.next();
            BiliArtistInfo info = new BiliArtistInfo(bid);
        }

    }


    public void downloadBid(String bid) throws IOException {
        String url = PathUtils.buildPath(biliSetting.BIL_BASE, bid,"article");
        BiliArtistInfo info = new BiliArtistInfo(bid);
        BidPageProcessor pageProcessor = new BidPageProcessor(info,biliSetting);
        MixDownloader downloader = new MixDownloader(biliSetting.CHROME_PATH,biliSetting.CHROME_DRIVER_PATH, biliSetting.WEB_DRIVER_DEBUGGING_PORT);
        MixDownloader.calledTime = biliSetting.ARTICLE_PAGE_START_SCROLL_TIME;
        int stored = pageProcessor.getArtworkNum();
        logger.warn("XXXX 作者初始文件数："+stored);
        int limit = biliSetting.BREAK_LIMIT;
        while (limit>0){

            Spider.create(pageProcessor)
                    .setDownloader(downloader)
                    .addUrl(url)
                    .thread(1)
                    .run();
            int newStored = pageProcessor.getArtworkNum();
            if(newStored>stored){
                logger.warn("文件增加了：原"+stored+" / 新"+newStored);
                stored = newStored;
                limit = biliSetting.BREAK_LIMIT;
            }else{
                logger.warn("本次启动爬虫，存储文件数为增加，LIMIT倒计时 "+limit+"/"+biliSetting.BREAK_LIMIT);
                limit--;
            }
        }

    }
    public void updateBid(String bid) throws IOException {
        String url = PathUtils.buildPath(biliSetting.BIL_BASE, bid,"article");
        BiliArtistInfo info = new BiliArtistInfo(bid);
        BidPageProcessor pageProcessor = new BidPageProcessor(info,biliSetting);
        MixDownloader downloader = new MixDownloader(biliSetting.CHROME_PATH,biliSetting.CHROME_DRIVER_PATH, biliSetting.WEB_DRIVER_DEBUGGING_PORT);
        MixDownloader.calledTime = 0;
        int keepGoing = biliSetting.BREAK_LIMIT;
        while (keepGoing>0){
            logger.info("keepGoing: "+keepGoing);
            Spider.create(pageProcessor)
                    .setDownloader(downloader)
                    .addUrl(url)
                    .thread(1)
                    .run();
            int newAdded = pageProcessor.newPageAdded;
            if(newAdded>0){
                keepGoing = biliSetting.BREAK_LIMIT;
            }else{
                keepGoing--;
            }
        }
    }
    public void start() throws IOException {
        if("UPATE".equals(biliSetting.TASK)){
            List<String> bids = getBidsFromDisk();
            for (int i = 0; i < bids.size(); i++) {
                updateBid(bids.get(i));
            }
        }else if("NEW".equals(biliSetting.TASK)){
            String[] bids = biliSetting.NEW_TASK_BID;
            for (int i = 0; i < bids.length; i++) {
                downloadBid(bids[i]);
            }
        }else{

        }

    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[1];
            args[0] = "G:/bili-setting.json";
        }
        BiliStarter starter = new BiliStarter(args[0]);
        starter.start();


    }
}
