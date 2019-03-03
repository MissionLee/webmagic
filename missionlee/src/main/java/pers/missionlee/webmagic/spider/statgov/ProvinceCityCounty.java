package pers.missionlee.webmagic.spider.statgov;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-01 16:38
 */
public class ProvinceCityCounty implements PageProcessor {
    Site site = Site.me()
            .setTimeOut(60000)
            .addCookie("AD_RS_COOKIE", "20081944")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive");
    String rootSite = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2018/";

    static Pattern htmlTextPattern = Pattern.compile(">(.+?)<");
    static Pattern htmlEndTagHtmlTextPattern = Pattern.compile("html\">(.+?)<");
    static Pattern htmlTDTextPattern = Pattern.compile("<td>(.+?)</td>");
    static Pattern htmlTitlePattern = Pattern.compile("title=\"(.+?)\"");
    static Pattern htmlHrefPattern = Pattern.compile("href=\"(.+?)\"");
    static Pattern resolutionPattern = Pattern.compile("bytes\">(.+?)<");

    @Override
    public void process(Page page) {
        String URL = page.getUrl().toString();
        int numOf = 0;
        char aim = "/".charAt(0);
        for (int i = 0; i < URL.length(); i++) {
            char tmp = URL.charAt(i);
            if (aim == tmp)
                numOf++;
        }

        Html html = page.getHtml();

        if (numOf == 7 && URL.contains("index.html")) {// index 页面 省份
            List<String> provinceList = html.$(".provincetr").$("a").all();
            Map<String, String> provinceMap = new HashMap<String, String>();
            for (String str : provinceList
            ) {
                Matcher hrefMatcher = htmlHrefPattern.matcher(str);
                hrefMatcher.find();
                String sonHref = hrefMatcher.group(1);

                Matcher textMatcher = htmlTextPattern.matcher(str);
                textMatcher.find();
                String province = textMatcher.group(1);
                provinceMap.put(sonHref, province);
                page.addTargetRequest(rootSite + sonHref);
            }
            System.out.println(provinceMap);
        } else if (numOf == 7 || numOf == 8 || numOf ==9 ) { // 市 县 区 接口一样

            Map<String, String> aimMap = new HashMap<String, String>();
            List<String> aimString = new ArrayList<String>();
            if (numOf == 7)
                aimString = html.$(".citytr").all();
            else if (numOf == 8)
                aimString = html.$(".countytr").all();
            else
                aimString = html.$(".towntr").all();

            for (String str : aimString) {

                Matcher hrefMatcher = htmlHrefPattern.matcher(str);
                String sonHref = "";
                if (hrefMatcher.find()) {
                    sonHref = hrefMatcher.group(1);
                }


                Matcher textMatcher;
                if (str.contains("href")) {
                    textMatcher = htmlEndTagHtmlTextPattern.matcher(str);
                } else {
                    textMatcher = htmlTDTextPattern.matcher(str);
                }
                textMatcher.find();
                String code = textMatcher.group(1);
                textMatcher.find();
                String name = textMatcher.group(1);
                aimMap.put(code, name);


                if (!StringUtils.isEmpty(sonHref)) {
                    if (numOf == 7) {
                        System.out.println("add:"+rootSite+sonHref);
                        page.addTargetRequest(rootSite + sonHref);
                    }
                    // TODO: 2019/3/2 把乡镇级别的统计注释掉了，太多了
//                    else if (numOf == 8) {
//                        System.out.println("add:"+rootSite
//                                + page.getUrl().toString()
//                                .split("/")[7]
//                                + "/" + sonHref);
//                        page.addTargetRequest(
//                                rootSite
//                                        + page.getUrl().toString()
//                                        .split("/")[7]
//                                        + "/" + sonHref);
//                    }

                }

            }
            String ppp ="";
            if(numOf==7){
                ppp="city#";
            }else if(numOf ==8){
                ppp="county#";
            }else
                ppp="town#";
            synchronized(this.getClass()){
                try {
                    FileUtils.writeStringToFile(new File("C:\\Users\\Administrator\\Desktop\\pcc.txt"),"\n"+ppp+aimMap.toString(),"UTF8",true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.print(ppp);
            System.out.println(aimMap);
        }

    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        // http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2018/41/4106.html
        //http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2018/index.html
        Spider.create(new ProvinceCityCounty()).addUrl("http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2018/index.html").thread(10).run();



    }
}
