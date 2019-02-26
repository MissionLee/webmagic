package us.codecraft.webmagic.samples.missionlee;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-26 21:51
 */
public class SankakuDemo1 implements PageProcessor {
    int flag = 0;
    Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("auto_page","0")
            .addCookie("locale","en")
            .addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding","gzip, deflate, br")
            .addHeader("Accept-Language","zh-CN,zh;q=0.9")
            .addHeader("Cache-Control","no-cache")
            .addHeader("Connection","keep-alive")
            .addHeader("Host","chan.sankakucomplex.com")
            .addHeader("Pragma","no-cach")
            .addHeader("Upgrade-Insecure-Requests","1")
            .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36")

            ;
    @Override
    public void process(Page page) {

        try {
            int sleep = new Random().nextInt(15000);
            System.out.println("SLEEP"+Thread.currentThread().getName() +" TIME: "+sleep);
            Thread.sleep(sleep);
            System.out.println("AWAKE"+Thread.currentThread().getName() +" TIME: "+sleep);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(page.getUrl().toString().contains("https://chan.sankakucomplex.com/post/show/")){
            // 详情页面
            Html html = page.getHtml();
            String target ;
            List<String> maybe = html.$("#image-link","href").all();
            if(!maybe.get(0).equals("")){ // 有非缩略地址
                System.out.println("target from #image-link - href");
                target= "https:"+maybe.get(0);
            }else{ // 自身就是
                System.out.println("target from img - src");
                target="https:"+html.$("#post-content").$("img","src").all().get(0).replace("&amp;","&");
            }
            String[] split = target.split("/");
            String name = split[split.length-1].split("\\?")[0];
            try {
                SankakuItemDownloader.download(target,name,"C:\\Users\\Administrator\\Desktop\\123");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            // 列表页面
            List<String> urlList =page.getHtml().$(".thumb").$("a","href").all();
            if(urlList!=null && urlList.size()>0 ){
                for (String url:urlList
                     ) {
                    System.out.println("⭐ add TargetRequest "+"https://chan.sankakucomplex.com"+url);
                    page.addTargetRequest("https://chan.sankakucomplex.com"+url);
                }

            }
        }


    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new SankakuDemo1()).addUrl("https://chan.sankakucomplex.com/post/show/5315951").thread(2).run();
    }
}
