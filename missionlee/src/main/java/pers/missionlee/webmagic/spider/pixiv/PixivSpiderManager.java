package pers.missionlee.webmagic.spider.pixiv;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pers.missionlee.webmagic.spider.pixiv.common.Constants;
import us.codecraft.webmagic.Site;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-24 13:53
 */
public class PixivSpiderManager {
    private static Site pixivSite;
    private static HttpGet get;
    private static HttpPost post;
    private static CookieStore cookieStore;
    private static CloseableHttpClient httpClient;
    private static CloseableHttpResponse response;
    private static SSLContext sslContext;
    private static String POST_KEY;
    private Boolean login = false;

    static {
        // 初始化 cookieSotre
        cookieStore = new BasicCookieStore();
        // 初始化 SSL
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream inputStream = new FileInputStream(new File(Constants.KEY_SOTRE));
            keyStore.load(inputStream, Constants.KEY_STORE_KEY.toCharArray());
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        pixivSite = new Site();
        httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    /**
     * 访问 pixiv会获取一个 post key 之后的所有请求都要带上这个postkey
     */
    private static void preLogin() throws IOException {
        get = new HttpGet(Constants.PIXIV_BASE_URL);
        response = httpClient.execute(get);

        String res = EntityUtils.toString(response.getEntity());
        System.out.println(res);
        Document doc = Jsoup.parse(res);
        Element post_key = doc.select("input[name=post_key]").get(0);
        POST_KEY = post_key.attr("value");
        System.out.println(POST_KEY);
        System.out.println(cookieStore.toString());
        if (response != null)
            response.close();
    }

    /**
     * 登录
     */
    public static void login() throws IOException {
        if (StringUtils.isEmpty(POST_KEY))
            preLogin();
        System.out.println(POST_KEY);
        // 登录参数
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("pixiv_id", Constants.USERNAME));
        params.add(new BasicNameValuePair("password", Constants.PASSWORD));
        params.add(new BasicNameValuePair("post_key", POST_KEY));
        post = new HttpPost(Constants.PIXIV_LOGIN_URL);
        post.setEntity(new UrlEncodedFormEntity(params, Charset.forName("UTF-8")));
        response = httpClient.execute(post);
        String responseStr = EntityUtils.toString(response.getEntity());
        // 返回json
        JSONObject resJson = JSON.parseObject(responseStr);
        System.out.println(resJson);
        JSONObject responseJsonBody = JSON.parseObject(resJson.get("body").toString());
        try {
            if (responseJsonBody.containsKey("success")) {
                System.out.println("登陆成功");
            } else {
                System.out.println(responseJsonBody.get("validation_errors"));
                throw new Exception("登陆失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(response!=null)
                response.close();
        }

    }
    public Map<String,Set<String>> getArtistIllList(String artistId) throws IOException {
        get = new HttpGet(Constants.PIXIV_ARITST_ALL_AJAX.replace("USER-ID",artistId));
        response = httpClient.execute(get);
        String res = EntityUtils.toString(response.getEntity());
        System.out.println("阿贾克斯访问拿到的返回内容"+res);
        res = res.replaceAll("null","\"\"");
        JSONObject resJ = JSONObject.parseObject(res);
        JSONObject resBody = (JSONObject) resJ.get("body");
        // ill
        JSONObject illusts = resBody.getJSONObject("illusts");
        JSONObject manga = resBody.getJSONObject("manga");
        JSONObject mangaSeries = resBody.getJSONObject("mangaSeries");
        Map retMap = new HashMap();
        retMap.put("illusts",illusts.keySet());
        retMap.put("manga",manga.keySet());
        retMap.put("mangaSeries",mangaSeries.keySet());
        System.out.println(illusts.keySet());
        System.out.println(manga.keySet());
        System.out.println(mangaSeries.keySet());
        return retMap;
    }
    public static void downloadArtist(Map<String, Set<String>> artworks) throws IOException {
        if(artworks.containsKey("illusts")){
            for (String art:artworks.get("illusts")
                 ) {
                downLoad(art);
                break;
            }
        }
    }
    static String imgUrlStartStr = "original\":";
    static int theLength = imgUrlStartStr.length();
    static String imgUrlEndStr = "\"},\"tags\":";
    public static void downLoad(String artworkId) throws IOException {
        get = new HttpGet(Constants.PIXIV_ILLUST_MEDIUM_URL+artworkId);
        response = httpClient.execute(get);
        String res = EntityUtils.toString(response.getEntity());Document doc = Jsoup.parse(res);
        int start = res.indexOf(imgUrlStartStr)+theLength+1;
        int end = res.indexOf(imgUrlEndStr);
        String ingUrl = res.substring(start,end).replaceAll("\\\\","");
        if(response!=null)response.close();
        get = new HttpGet(ingUrl);
        get.setHeader("referer",ingUrl);
        response = httpClient.execute(get);
        File aim = new File(Constants.IMG_DOWNLOAD_BASE_PATH+"/"+ingUrl.substring(ingUrl.lastIndexOf(".")));
        FileOutputStream outputStream = new FileOutputStream(aim);
        InputStream inputStream = response.getEntity().getContent();
        byte data[] = new byte[1024];
        int len;
        while ((len = inputStream.read(data))!=-1){
            outputStream.write(data,0,len);
        }
        outputStream.flush();
        outputStream.close();
        if(response!=null)response.close();
    }
    public static void main(String[] args) throws IOException {
        PixivSpiderManager.login();
        System.out.println(cookieStore);
        PixivSpiderManager pixivSpiderManager = new PixivSpiderManager();
        Map<String,Set<String>> listList =pixivSpiderManager.getArtistIllList("264716");
        PixivSpiderManager.downloadArtist(listList);
        System.out.println(cookieStore);
    }
}
