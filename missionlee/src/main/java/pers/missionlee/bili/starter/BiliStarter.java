package pers.missionlee.bili.starter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BiliStarter {
    public BiliSetting biliSetting ;
    public ChromeBookmarksReader reader;
    public BiliStarter(String settingPath) {
        File setting = new File(settingPath);
        String settingString = null;
        try {
            settingString = FileUtils.readFileToString(setting,"UTF8");
            this.reader = new ChromeBookmarksReader(biliSetting.chromePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.biliSetting = JSON.parseObject(settingString, BiliSetting.class);
    }
    public void downloadWithChromeFolder(String folderName){
        List<Map> urls = reader.getBookMarkListByDirName(folderName);
        List<String> bids = new ArrayList<>();
        for(Map bookMar:urls){
            String url = bookMar.get("url").toString();
            if(url.contains("space.bilibili.com")){
//                   https://space.bilibili.com/179308916?spm_id_from=333.337.0.0
                String bid = null;
                if(url.contains("?")){
                    bid = url.substring(url.lastIndexOf("/")+1,url.indexOf("?"));
                }else{
                    bid = url.substring(url.lastIndexOf("/")+1);
                }
                bids.add(bid);
            }
        }
        Iterator<String>  iterator = bids.listIterator();
        int index = 0;
        while (iterator.hasNext()){
            String bid = iterator.next();
            BiliArtistInfo info = new BiliArtistInfo();
            info.bid = bid;
        }

    }
    public void start(){

    }
    public static void main(String[] args) {
//        if(args.length == 0){
//            args = new String[1];
//            args[0] = "G:/bili-setting.json";
//        }
//        BiliStarter starter = new BiliStarter(args[0]);
//        starter.start();
        String url = "https://space.bilibili.com/179308916?spm_id_from=333.337.0.0";
        String bid = url.substring(url.lastIndexOf("/")+1,url.indexOf("?"));
//        String bid = url.substring(url.lastIndexOf("/"));
        System.out.println(bid);

    }
}
