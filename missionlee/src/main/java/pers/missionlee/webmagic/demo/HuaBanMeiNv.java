package pers.missionlee.webmagic.demo;

import com.alibaba.fastjson.JSON;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-24 21:55
 */
public class HuaBanMeiNv implements PageProcessor {
    Site site = Site.me();


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
//            System.out.println("utils"+ i++);
            Map map = (Map)JSON.parse(th.toString());
            Map fileMap = (Map)JSON.parse(map.get("file").toString());

//            try {
////                download("http://img.hb.aicdn.com/"+fileMap.get("key"),fileMap.get("key")+".jpg","C:\\Users\\MissionLee\\Desktop\\表格");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
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
