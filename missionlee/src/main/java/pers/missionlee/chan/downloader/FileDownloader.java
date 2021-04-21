package pers.missionlee.chan.downloader;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.newsankaku.utlis.Downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-11 10:23
 */
public class FileDownloader {
    public static Logger logger = LoggerFactory.getLogger(FileDownloader.class);
    public static final int retryLimit = 3;
    public static DecimalFormat df = new DecimalFormat("0.00");
    private static int blockSize = 32 * 1024 * 1024; // 每个连接做多下载 256mb大小内容
    public static CloseableHttpClient closeableHttpClient = HttpClients.custom().build();
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static File download(String aimUrl, String referer,String tempPath) {
        logger.info("下载："+aimUrl);
        boolean downloadSuccess = false; // 下载成功
        String fileName = aimUrl.substring(aimUrl.lastIndexOf("/")+1,aimUrl.indexOf("?"));
        int retry = retryLimit;


        while ((!downloadSuccess) && (retry-- > 0)) {
            try {
                    logger.info("开始下载[" + (retryLimit - retry) + "]：" + aimUrl);
                    URL url = new URL(aimUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    formatConnection(referer, connection, "HEAD");
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 400) {
                        logger.warn("文件大小[" + fileName + "]:请求失败 RESPONSE_STATUS " + responseCode);
                    } else {
                        int fileSize = connection.getContentLength();
                        // A : 根据文件大小 Sleep 从而降低触发 429 的概率
                        logger.info("根据文件大小 Sleep");
                        String fsize = df.format(fileSize * 1.0 / (1024 * 1024));
                        logger.info("文件大小[" + fileName + "]:" + fsize + "MB[" + fileSize + "bytes]");
                        sleep(fileSize);
                        String tempName = java.util.UUID.randomUUID().toString();
                        File tempFile = new File(tempPath + tempName);

                        RandomAccessFile tempFileAccess = new RandomAccessFile(tempFile, "rw");
                        tempFileAccess.setLength(fileSize);
                        int countDownNumber = new Double(Math.ceil(fileSize * 1.0 / blockSize)).intValue();
                        CountDownLatch latch = new CountDownLatch(countDownNumber);
                        Map<String, Boolean> rangeDownload = new HashMap<>();
                        for (int i = 0; i < countDownNumber; i++) {
                            int start = blockSize * i;
                            int end = blockSize * (i + 1) - 1;
                            if (end >= fileSize) end = fileSize - 1;
                            String range = "bytes=" + start + "-" + end;
                            rangeDownload.put(range, false);
                            Downloader.CallableHttpRangeDownloader downloader = new Downloader.CallableHttpRangeDownloader(aimUrl, referer, tempFileAccess, start, range, latch, rangeDownload);
                            executorService.submit(downloader);
                        }

                        latch.await();

                        System.out.println("CountDownLatch 全部完成");
                        // 如果分块下载成功了，会给对应 range 改成 true
                        AtomicBoolean countDownSuccess = new AtomicBoolean(true);
                        rangeDownload.forEach((String range, Boolean success) -> {
                            if (success) {
                                System.out.println("段落下载成功：" + range);
                            } else {
                                System.out.println("段落下载失败：" + range);
                                countDownSuccess.set(false);

                            }
                        });
                        if (countDownSuccess.get()) { // 每一部分都下载成功
                            tempFileAccess.close(); // 关闭access\
                            downloadSuccess = true;
                            return tempFile;

                    }
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {

            }

        }

        return  null;
    }

    public static void sleep(int fileSize) throws InterruptedException {
        long sleepTime = 1000;
        if (fileSize > 48 * 1024 * 1024) {
            sleepTime *= 0;
        } else if (fileSize > 24 * 1024 * 1024) {
            sleepTime *= 0;
        } else if (fileSize > 12 * 1024 * 1024) {
            sleepTime *= 0;
        } else if (fileSize > 6 * 1024 * 1024) {
            sleepTime *= 1;
        } else if (fileSize > 3 * 1024 * 1024) {
            sleepTime *= 2;
        } else if (fileSize > 1024 * 1024) {
            sleepTime *= 3;
        } else if (fileSize > 512 * 1024) {
            sleepTime *= 4;
        } else {
            sleepTime *= 5;
        }
        Thread.sleep(sleepTime);
    }

    private static void formatConnection(String referer, HttpURLConnection connection, String method) throws ProtocolException {
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod(method);

//        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//        connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
//        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
//        connection.setRequestProperty("cache-control", "no-cache");
//        connection.setRequestProperty("pragma", "no-cache");
        System.out.println(referer);
        connection.setRequestProperty("Referer", referer);
//        connection.setRequestProperty("upgrade-insecure-requests", "1");
        connection.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");

    }

    public static void main(String[] args) {
        String str = "https://s.sankakucomplex.com/data/c3/d0/c3d03e6a9625b813f5650c7a395eba56.jpg?e=1618408898&m=82yta2lBjcUXVTQhv7jBmw";
        String name = str.substring(str.lastIndexOf("/")+1,str.indexOf("?"));
        System.out.println(name);
    }
}
