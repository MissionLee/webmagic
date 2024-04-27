package pers.missionlee.bili.spider;

import org.apache.commons.io.FileUtils;
import pers.missionlee.bili.starter.BiliArtistInfo;
import pers.missionlee.bili.starter.BiliSetting;
import pers.missionlee.chan.filedownloader.FileDownloader;
import pers.missionlee.chan.pagedownloader.MixDownloader;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BidPageProcessor implements PageProcessor {
    BiliArtistInfo artistInfo;
    public BidPageProcessor(BiliArtistInfo info) {
        this.artistInfo = info;
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        // 1.
        if(url.contains("article")){
            processArticle(page);
        }else if(url.contains("opus")){
            processOpus(page);
        }else{

        }


    }
    public void processArticle(Page page){
//        System.out.println(page.getHtml());
        List<String> urls = page.getHtml()
                .$(".waterfall-content .container")
                .$(".item .article-card  a","href")
                .all();

        for (int i = 0; i < urls.size(); i++) {
            System.out.println(urls.get(i).substring(2));
            String newurl = "https://"+urls.get(i).substring(2);
            String ser = newurl.substring(newurl.lastIndexOf("/")+1);
            if(new File("G:\\C-B-ALL\\179308916\\"+ser+"\\").exists() && new File("G:\\C-B-ALL\\179308916\\"+ser+"\\").isDirectory()){
                System.out.println("目标已经存在，跳过啦啦啦啦啦啦"+ser);
            }else{
                System.out.println("发现新的ser，添加到列表"+ser);
                page.addTargetRequest(newurl);
            }

        }
    }
    public void processOpus(Page page){
        String url = page.getUrl().toString();
        String ser = url.substring(url.lastIndexOf("/")+1);
        System.out.println("页面序列号"+ser);
        if(new File("G:\\C-B-ALL\\179308916\\"+ser+"\\").exists() && new File("G:\\C-B-ALL\\179308916\\"+ser+"\\").isDirectory()){
            System.out.println("目标已经存在，跳过啦啦啦啦啦啦");
            return;
        }
        System.out.println("XXXXX");
        String pageString = page.getRawText();
        if(pageString.contains("6元充电")){

        }else{
            List<String> urls = page.getHtml()
                    .$(".bili-opus-view .opus-para-pic")
                    .$(".bili-album__preview__picture")
                    .$("img","src")
                    .all();
            List<String> originList = new ArrayList<>();
            for (int i = 0; i < urls.size(); i++) {
                String smallurl = urls.get(i);
                System.out.println(smallurl);
                String origurl ="https:"+smallurl.substring(0,smallurl.indexOf("@"));
                String fileName = origurl.substring(origurl.lastIndexOf("/")+1);
                System.out.println("文件名"+fileName);
                System.out.println(smallurl);
                System.out.println(origurl);
                System.out.println("==============");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                File tempFile = FileDownloader.download(origurl,page.getUrl().toString(),"G:\\C-B-ALL\\tmp\\",getSite());
                if (null != tempFile && tempFile.exists() && tempFile.isFile() && tempFile.length() > 10) {
                    try {
                        FileUtils.moveFile(tempFile,new File("G:\\C-B-ALL\\179308916\\"+ser+"\\"+fileName));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    onDownloadFail(page);

                }
            }
        }
    }
    public void onDownloadSuccess(Page page){

    }
    public void onDownloadFail(Page page){

    }


    @Override
    public Site getSite() {
        return SpiderUtils.site_bili;
    }

    public static void main(String[] args) {
        String url = PathUtils.buildPath(BiliSetting.BIL_BASE,"179308916/article");
        BiliArtistInfo info = new BiliArtistInfo("179308916");
        BidPageProcessor pageProcessor = new BidPageProcessor(info);

        for (int i = 0; i < 100; i++) {

            try {
                Spider.create(pageProcessor)
                        .setDownloader(new MixDownloader("", "C:\\chromedriver-win64\\chromedriver.exe", "9292"))
                        .addUrl(url)
                        .thread(1)
                        .run();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
