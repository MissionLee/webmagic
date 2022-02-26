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
import pers.missionlee.chan.starter.SpiderSetting;
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
import java.text.SimpleDateFormat;
import java.util.Date;
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
    public static boolean smartShutDown = true;
    public static Logger logger = LoggerFactory.getLogger(FileDownloader.class);
    public static  int retryLimit = 3;
    public static DecimalFormat df = new DecimalFormat("0.00");
    public static int blockSize = 8 * 1024 * 1024; // 每个连接做多下载 256mb大小内容
    public static CloseableHttpClient closeableHttpClient = HttpClients.custom().build();
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static int error403 = 0;

    public static File download(String aimUrl, String referer, String tempPath) {
        logger.info("注意，下载器提供了 403 连续错误 50次，如果时间在1：00 ~ 9：30 之间 自动关机的功能");
        logger.info("下载：" + aimUrl);
        logger.info("retryLimit" + retryLimit);
        boolean downloadSuccess = false; // 下载成功
        String fileName = aimUrl.substring(aimUrl.lastIndexOf("/") + 1, aimUrl.indexOf("?"));
        int retry = retryLimit;


        while ((!downloadSuccess) && (retry-- > 0)) {
            long startTime = System.currentTimeMillis();
            try {
                logger.info("开始下载[" + (retryLimit - retry) + "]：" + aimUrl);
//                    URL url = new URL(aimUrl);
//                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                HttpURLConnection connection = HCaptchaConnectionFormat.format(aimUrl, referer, "GET");
                int responseCode = connection.getResponseCode();
                if(responseCode >= 500){
                    logger.error("遇到大于500的错误，下载器直接跳出下载循环");
                    break;
                }
                if (responseCode >= 400) {
                    logger.warn("文件大小[" + fileName + "]:请求失败 RESPONSE_STATUS " + responseCode);
                    if (responseCode == 403) {
                        error403++;
                        logger.info("遇到403错误，检查cookieString刷新情况，此处Sleep 3分钟");
                        boolean updated = HCaptchaConnectionFormat.refreshCookieString();
                        if (updated) {
                            error403 = 0;
                        }
//                        Thread.sleep(180000);
                    }
                    if (error403 > 5) {
                        synchronized ("abc"){
                            logger.info("系统403 权限问题出现 50次了，智能关机");
                            if (smartShutDown && FileDownloader.shutDownTime()) {
                                Runtime rt = Runtime.getRuntime();
                                rt.exec("shutdown.exe -s -t 40");
                            }
                            Thread.sleep(20000);
                            System.exit(0);
                        }
                    }
                    Thread.sleep(100000);
                } else {
                    int fileSize = connection.getContentLength();
                    // A : 根据文件大小 Sleep 从而降低触发 429 的概率
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
                        CallableHttpRangeDownloader downloader = new CallableHttpRangeDownloader(aimUrl, referer, tempFileAccess, range, latch, rangeDownload, start);
//                            Downloader.CallableHttpRangeDownloader downloader = new Downloader.CallableHttpRangeDownloader(aimUrl, referer, tempFileAccess, start, range, latch, rangeDownload);
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
                        logger.info("所有段落下载成功，返回临时文件供后续程序使用");
                        tempFileAccess.close(); // 关闭access\
                        downloadSuccess = true;
                        long endTime = System.currentTimeMillis();
                        logger.info("文件大小："+fsize+"MB 耗时"+df.format((startTime-endTime)*1.0/1000)+"秒");
                        if((startTime-endTime)*1.0/1000 < 5.1){
                            logger.info("因为下载时间低于5秒，此处Sleep5秒");
                            Thread.sleep(5000);
                        }else if((startTime-endTime)*1.0/1000 < 10.1 ){
                            logger.info("因为下载时间低于5秒，此处Sleep5秒");
                            Thread.sleep(3000);
                        }
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

        return null;
    }
    public static boolean shutDownTime(){
        SimpleDateFormat dateFormater = new SimpleDateFormat("HHmm");
        String date = dateFormater.format(new Date());
        int time = Integer.parseInt(date);
        if (time > 200 && time < 930) {
            return true;
        } else {
            return false;
        }
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
        logger.info("根据文件大小 Sleep："+sleepTime/1000+"秒");
        Thread.sleep(sleepTime);
    }

    private static void formatConnection(String referer, HttpURLConnection connection, String method) throws ProtocolException {
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

//        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//        connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
//        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
//        connection.setRequestProperty("cache-control", "no-cache");
//        connection.setRequestProperty("pragma", "no-cache");
        System.out.println(referer);
        connection.setRequestProperty("referer", referer);
//        connection.setRequestProperty("upgrade-insecure-requests", "1");
        connection.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36");
        connection.setRequestProperty("cookie", "__cfduid=dbf7e7773e9cff987a17a97a7985956eb1619305796; cf_chl_2=d75f3598eff7b80; cf_chl_prog=a9; cf_clearance=38dc314abf4c8bb334a48a8e34a898fb19612a26-1619314705-0-250; _sankakucomplex_session=BAh7CDoMdXNlcl9pZGkD5lgGIgpmbGFzaElDOidBY3Rpb25Db250cm9sbGVyOjpGbGFzaDo6Rmxhc2hIYXNoewAGOgpAdXNlZHsAOg9zZXNzaW9uX2lkIiU4ODJhNmQxOThlZTUxN2I3MjI0Y2Y5N2VjYTdlYTZlNw==--9c7b73bee2ac4e9e47ca127737b669aa22cc34f5");
        connection.setRequestMethod(method);

    }


}
