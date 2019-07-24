package pers.missionlee.webmagic.spider.pixiv.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.utils.TimeLimitedHttpDownloader;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.utils.HttpConstant;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-23 14:57
 */
public class PixivSpider  implements PageProcessor {
    boolean login = false;
    static {

    }
    private static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("OX_plg", "pm")
            .addCookie("PHPSESSID", "8640851_a30a677fffc85a6b35b962c9120cbbf2")
            .addCookie("__utma", "235335808.1904108407.1563863313.1563863313.1563863313.1")
            .addCookie("__utmb", "235335808.17.9.1563864053348")
            .addCookie("__utmc", "235335808")
            .addCookie("__utmv", "235335808.|2=login%20ever=yes=1^3=plan=normal=1^5=gender=male=1^6=user_id=8640851=1^9=p_ab_id=1=1^10=p_ab_id_2=4=1^11=lang=zh=1")
            .addCookie("__utmz", "235335808.1563863313.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)")
            .addCookie("_ga", "GA1.2.1904108407.1563863313")
            .addCookie("_gid", "GA1.2.679768405.1563863340")
            .addCookie("a_type", "0")
            .addCookie("b_type", "1")
            .addCookie("c_type", "26")
            .addCookie("device_token", "115aaef7325d9d0b3a82cf47eb881179")
            .addCookie("first_visit_datetime_pc", "2019-07-23+15%3A28%3A15")
            .addCookie("id", "22643c43a2ae00fc||t=1548722154|et=730|cs=002213fd482c349876f0ef72c5")
            .addCookie("is_sensei_service_user", "1")
            .addCookie("ki_r", "")
            .addCookie("ki_s", "198890%3A0.0.0.0.0")
            .addCookie("ki_t", "1563864053327%3B1563864053327%3B1563864053327%3B1%3B1")
            .addCookie("login_bc", "1")
            .addCookie("login_ever", "yes")
            .addCookie("module_orders_mypage", "%5B%7B%22name%22%3A%22sketch_live%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22tag_follow%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22recommended_illusts%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22everyone_new_illusts%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22following_new_illusts%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22mypixiv_new_illusts%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22spotlight%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22fanbox%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22featured_tags%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22contests%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22user_events%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22sensei_courses%22%2C%22visible%22%3Atrue%7D%2C%7B%22name%22%3A%22booth_follow_items%22%2C%22visible%22%3Atrue%7D%5D")
            .addCookie("p_ab_d_id", "1266853578")
            .addCookie("p_ab_id", "1")
            .addCookie("p_ab_id_2", "4")
            .addCookie("privacy_policy_agreement", "1")
            .addCookie("yuid_b", "FVIkN3A")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "zh-CN")
            .addHeader("Cache-Control", "max-age=0")
            .addHeader("Host", "www.pixiv.net")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");

//    https://2heng.xin/2017/09/19/pixiv/
    String illPrefix = "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=";
    String imgUrlStartStr = "original\":";
    int theLength = imgUrlStartStr.length();
    String imgUrlEndStr = "\"},\"tags\":";
    @Override
    public void process(Page page) {
        if(page.getUrl().toString().startsWith("https://www.pixiv.net/ajax/user/")){
            String res = page.getRawText().replaceAll("null","\"\"");
            JSONObject resJ = JSONObject.parseObject(res);
            JSONObject resBody = (JSONObject) resJ.get("body");
            // ill
            JSONObject illusts = resBody.getJSONObject("illusts");
            JSONObject manga = resBody.getJSONObject("manga");
            JSONObject mangaSeries = resBody.getJSONObject("mangaSeries");
            int i = 0;
            for (String key :
                    illusts.keySet()) {
                page.addTargetRequest(illPrefix+key);
                i++;
                if(i>1)
                break;
            }
            System.out.println(illusts.keySet());
            System.out.println(manga.keySet());
            System.out.println(mangaSeries.keySet());
        }else if(page.getUrl().toString().startsWith(illPrefix)){
            Html html = page.getHtml();
            String jsWithHref =html.$("script").all().get(5);

            int start = jsWithHref.indexOf(imgUrlStartStr);
            int end = jsWithHref.indexOf(imgUrlEndStr);
            System.out.println("start: "+start+"  | end "+end);
            String imgUrl = jsWithHref.substring(start+theLength+1,end).replaceAll("\\\\","");
            String fileName = System.currentTimeMillis()+".jpg";
            page.addTargetRequest(imgUrl);
//            try {
//                TimeLimitedHttpDownloader.downloadWithAutoRetry(imgUrl,fileName,page.getUrl().toString(),null);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        }else{
            System.out.println("eeelesese");
            System.out.println(page.getBytes());
            try {
                FileUtils.copyToFile(new ByteInputStream(page.getBytes(),page.getBytes().length),new File("C:\\Users\\MissionLee\\Desktop\\img\\"+System.currentTimeMillis()+".jpg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        PixivSpider spider = new PixivSpider();
//        Spider.create(spider).addUrl("https://www.pixiv.net/member_illust.php?id=5155946").thread(1).run();
        Spider ajaxspider = Spider.create(spider);
        Request request = new Request("https://www.pixiv.net/ajax/user/264716/profile/all");
        request.setMethod(HttpConstant.Method.GET);
        ajaxspider.addRequest(request).thread(1).run();


//        String str = "{\"name\":\"limingshun\",\"class\":{\"name\":null,\"type\":null},\"ok\":true}";
//        Object pd = JSON.parse(str);
//        System.out.println(pd);
//        String pdStr = JSON.toJSONString(pd,SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty);
//        System.out.println(pdStr);
//        Object pd2 = JSON.parse(pdStr);
//        System.out.println(pd2);

    }
}
