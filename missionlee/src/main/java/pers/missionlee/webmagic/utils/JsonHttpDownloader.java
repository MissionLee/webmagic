package pers.missionlee.webmagic.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-05-06 13:43
 */
public class JsonHttpDownloader {
    public static void main(String[] args) throws IOException {
        URL url = new URL("https://www.qianque.net/Diagnosis/GetDisease");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection
        formatConnection("https://www.qianque.net/Home/Index", connection);
        int responseCode = connection.getResponseCode();
        if ( responseCode>0) {
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            in.close();
            System.out.println(sb.toString());

        }else{
            System.out.println(responseCode);
        }
    }

    private static void formatConnection(String referer, HttpURLConnection connection) throws ProtocolException {
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("accept", "application/json, text/plain, */*");
        connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
        connection.setRequestProperty("cache-control", "no-cache");
        connection.setRequestProperty("Cookie","ASP.NET_SessionId=2zn1oe4om22isgw2q3btvmu4");
        connection.setRequestProperty("host","www.qianque.net");
        connection.setRequestProperty("origin","https://www.qianque.net");
        connection.setRequestProperty("pragma", "no-cache");
        connection.setRequestProperty("referer", referer);
        connection.setRequestProperty("upgrade-insecure-requests", "1");
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
    }
}
