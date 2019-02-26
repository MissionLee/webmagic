package us.codecraft.webmagic.samples.missionlee;

import com.alibaba.fastjson.JSON;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-24 21:55
 */
public class HuaBanMeiNv implements PageProcessor {
    Site site = Site.me();
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

    @Override
    public void process(Page page) {
        List<String> scriptLists= page.getHtml().$("script").all();
        String target = scriptLists.get(21)
                .replace("&lt;","")
                .replace("&gt;","")
                .replace("&amp;","")
                .split(";")[6]
                .substring("app.page[\"pins\"] = ".length()+1);
        List<Object> lists = (List)JSON.parse(target);
        int i =0;
        for (Object th:lists
             ) {
            System.out.println("download"+ i++);
            Map map = (Map)JSON.parse(th.toString());
            Map fileMap = (Map)JSON.parse(map.get("file").toString());

            try {
                download("http://img.hb.aicdn.com/"+fileMap.get("key"),fileMap.get("key")+".jpg","C:\\Users\\MissionLee\\Desktop\\表格");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new HuaBanMeiNv()).addUrl("http://huaban.com/favorite/beauty/").thread(1).run();
    }
}
