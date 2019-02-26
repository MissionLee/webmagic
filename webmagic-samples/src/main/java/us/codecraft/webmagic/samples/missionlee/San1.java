package us.codecraft.webmagic.samples.missionlee;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-24 21:07
 */
public class San1 implements PageProcessor {
    public static void download(String urlStr,String filename,String savePath) throws IOException {

        URL url = new URL(urlStr);
        //打开url连接  可以用普通的URLConnection,但是根据后台的不同，有些后台回对普通的URLConnection返回500错误
        //            更保险的形式，我们把Connection换成HttpURLConnection，因为浏览器使用这种方式来创建链接
        //            “GET/POST” 的设置是否恰当会从 405错误看出来
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        //请求超时时间
        connection.setConnectTimeout(50000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        connection.setRequestProperty("accept-encoding","gzip, deflate, br");
        connection.setRequestProperty("accept-language","zh-CN,zh;q=0.9");
        connection.setRequestProperty("cache-control","no-cache");
        connection.setRequestProperty("pragma","no-cache");
        connection.setRequestProperty("referer","https://chan.sankakucomplex.com/post/show/7556922");
        connection.setRequestProperty("upgrade-insecure-requests","1");
        connection.setRequestProperty("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");

        System.out.println(connection.getRequestMethod());

        //输入流
        InputStream in = connection.getInputStream();
        //缓冲数据
        byte [] bytes = new byte[1024];
        //数据长度
        int len;
        //文件
        File file = new File(savePath);
        if(!file.exists())
            file.mkdirs();

        OutputStream out  = new FileOutputStream(file.getPath()+"\\"+filename);
        //先读到bytes中
        while ((len=in.read(bytes))!=-1){
            //再从bytes中写入文件
            out.write(bytes,0,len);
        }
        //关闭IO
        out.close();
        in.close();

    }

    private Site site = Site.me()
            .setRetrySleepTime(3)
            .addCookie("PHPSESSID","h7v8rmd66b5kqspm31i8f5d12d")
            .addCookie("__atuvc","1%7C9")
            .addCookie("__atuvs","5c7297b75f5545fb000")
            .addCookie("_pk_id.1.eee1","b3b2c6efae9b58bc.1550191762.1.1550191795.1550191762.")
            .addCookie("_pk_id.2.42fa","582a6e4eb725b6ad.1544878729.7.1551013815.1551013808.")
            .addCookie("_pk_ref.1.eee1","%5B%22%22%2C%22%22%2C1550191762%2C%22https%3A%2F%2Fwww.baidu.com%2Flink%3Furl%3DzUbpTpsJYYop-gY0cLU2PTD7aurT9salfNbCD0zMOIBwRZj6HmDtn8IKmlZqHfe2Gu-ysvCALGPIIj-X1wT6Iz6rnB2QQT9ufoywHu_vb5m%26wd%3D%26eqid%3Ddbaf5c840003505e000000055c660c86%22%5D")
            .addCookie("_pk_ses.2.42fa","*")
            .addCookie("auto_page","1")
            .addCookie("locale","en")
            .setSleepTime(0);


    @Override
    public void process(Page page) {
        System.out.println("11111");

        List<String> list=page.getHtml()
                        .$("#post-content")
                        .$("#image-link")
                        .$("img","src")
                        .all();
        if(list.size()>0){
            String theUrl = list.get(0);
            try {
                download(theUrl,"hello","C:\\Users\\Administrator\\Desktop\\123");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("222222");
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
//        Spider.create(new San1()).addUrl("https://chan.sankakucomplex.com/post/show/7034962").thread(1).run();

        try {
            System.out.println("START DOWNLOAD");
            download("https://cs.sankakucomplex.com/data/85/c2/85c21f6e108c68903660b3a6e552b4e5.mp4?e=1551099883&m=YXy5serKwXrX8t5_syYhEQ","85c21f6e108c68903660b3a6e552b4e5.mp4","C:\\Users\\MissionLee\\Desktop\\表格");
            System.out.println("END DOWNLOAD");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
