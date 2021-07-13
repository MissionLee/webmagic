package pers.missionlee.chan.downloader;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import pers.missionlee.chan.starter.SpiderSetting;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-25 13:36
 */
public class HCaptchaConnectionFormat {
    //                                 Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36
    public static String User_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36";
//    public static String User_agent = "Chrome/90.0.4430.85";
    public static String HCAPTCHA_VERIFY = "https://accounts.hcaptcha.com/verify_email";
    public static String __cfduid_PREFIX = "__cfduid";
    //         connection.setRequestProperty("cookie","__cfduid=dbf7e7773e9cff987a17a97a7985956eb1619305796; cf_chl_2=d75f3598eff7b80; cf_chl_prog=a9; cf_clearance=38dc314abf4c8bb334a48a8e34a898fb19612a26-1619314705-0-250; _sankakucomplex_session=BAh7CDoMdXNlcl9pZGkD5lgGIgpmbGFzaElDOidBY3Rpb25Db250cm9sbGVyOjpGbGFzaDo6Rmxhc2hIYXNoewAGOgpAdXNlZHsAOg9zZXNzaW9uX2lkIiU4ODJhNmQxOThlZTUxN2I3MjI0Y2Y5N2VjYTdlYTZlNw==--9c7b73bee2ac4e9e47ca127737b669aa22cc34f5");
    public static String __cfduid;
    public static String cf_chl_2;
    public static String cf_chl_prog;
    public static String cf_clearance;
    public static String cookieString;
    public static String settingPathString;
    public static List<String> cookieStrings ;

    public static HttpURLConnection format(String urlStr, String referer, String method) throws IOException {
        String formatUrl = formatUrl(urlStr);
        URL url = new URL(formatUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestProperty(":authority",":s.sankakucomplex.com");
        connection.setRequestProperty("accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        connection.setRequestProperty("accept-encoding","gzip, deflate, br");
        connection.setRequestProperty("accept-language","en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
//        connection.setRequestProperty("cache-control","no-cache");
//        connection.setRequestProperty("pragma","no-cache");
        connection.setRequestProperty("sec-ch-ua","\" Not;A Brand\";v=\"99\", \"Google Chrome\";v=\"91\", \"Chromium\";v=\"91\"");
        connection.setRequestProperty("sec-ch-ua-mobile","?0");
        connection.setRequestProperty("sec-fetch-dest","image");
        connection.setRequestProperty("sec-fetch-mode","no-cors");
        connection.setRequestProperty("sec-fetch-site","same-site");
//        connection.setRequestProperty("sec-fetch-user","?1");
//        connection.setRequestProperty("upgrade-insecure-requests","1");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("referer", referer);
        connection.setRequestProperty("User-Agent", User_agent);
        connection.setRequestProperty("Cookie", cookieString);
        connection.setRequestMethod(method);
//        System.out.println(urlStr+"______"+referer+"____________"+cookieString);
        return connection;
    }

    public static boolean refreshCookieString() {
        for (int i = 0; i < cookieStrings.size()-1 ; i++) {
            if(cookieString.equals(cookieStrings.get(i))){
                cookieString = cookieStrings.get(i+1);
                return true;
            }
        }
        return false;
    }

    public static String formatUrl(String url) {
        return url;
    }

    public static void refresh() throws IOException {
        URL url = new URL(HCAPTCHA_VERIFY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.getResponseCode();
        String CFDUID = "";
        String key = null;
        String cookieVal = "";
        String sessionId = "";
        // 获取 __cfduid
        for (int i = 1; (key = conn.getHeaderFieldKey(i)) != null; i++) {
            if (key.equalsIgnoreCase("set-cookie")) {
                cookieVal = conn.getHeaderField(i);
//                    cookieVal = cookieVal.substring(0, cookieVal.indexOf(";"));
                sessionId = sessionId + cookieVal + ";";
                System.out.println(key);
                System.out.println(cookieVal);
                if (cookieVal.startsWith("session")) {
                    CFDUID = cookieVal.substring(cookieVal.indexOf("=") + 1, cookieVal.indexOf(";"));
                }
            }
        }
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        URL getCookieUrl = new URL(HCAPTCHA_VERIFY + "/" + CFDUID);
        HttpURLConnection connection = (HttpURLConnection) getCookieUrl.openConnection();
        for (int i = 1; (key = connection.getHeaderFieldKey(i)) != null; i++) {
            if (key.equalsIgnoreCase("set-cookie")) {
                cookieVal = conn.getHeaderField(i);
//                    cookieVal = cookieVal.substring(0, cookieVal.indexOf(";"));
                sessionId = sessionId + cookieVal + ";";
                System.out.println(key);
                System.out.println(cookieVal);
//                if(cookieVal.startsWith(__cfduid_PREFIX)){
//                    CFDUID = cookieVal.substring(cookieVal.indexOf("=")+1,cookieVal.indexOf(";"));
//                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        HCaptchaConnectionFormat.refresh();
    }
    // Set-Cookie
    //__cfduid=df71f1c86b7c38a2d8669a5d6b7f996841619331966; expires=Tue, 25-May-21 06:26:06 GMT; path=/; domain=.hcaptcha.com; HttpOnly; SameSite=Lax; Secure
    //Set-Cookie
    //INGRESSCOOKIE=1619331967.822.4369.41274; Path=/; HttpOnly
    //Set-Cookie
    //session=d102a45f-01e8-4384-a36f-a06d0a80edfc; Domain=.hcaptcha.com; Expires=Wed, 26-May-2021 06:26:06 GMT; Secure; HttpOnly; Path=/
}
