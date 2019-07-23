package pers.missionlee.webmagic.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.manager.SpiderTask;

import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @description: 用于下载网络资源 ： 图片 视频 音频等
 * @author: Mission Lee
 * @create: 2019-02-27 10:05
 */

public class TimeLimitedHttpDownloader implements Thread.UncaughtExceptionHandler {
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static Logger logger = LoggerFactory.getLogger(TimeLimitedHttpDownloader.class);
    private static int downloadSpeedLimit = 5; // Unit: k/s
    private static DecimalFormat df = new DecimalFormat(".00");
    private static int mb = 1024 * 1024;
    private static long TEN_MINUTES = 10 * 60L;
    private static Map<Thread, Map<String, Object>> runInfo = new HashMap<Thread, Map<String, Object>>();

    @Override
    public void uncaughtException(Thread t, Throwable e) {

    }

    private static class CallableInputStreamDownloader implements Callable {
        InputStream in;
        OutputStream out;
        int size;
        String filename;

        public CallableInputStreamDownloader(InputStream in, OutputStream out, int size, String filename) {
            this.in = in;
            this.out = out;
            this.size = size;
            this.filename = filename;
        }

        public static int readBufferSize = 1024 * 16;
        public static int writeBufferSize = 1024 * 1024 * 16;
        public static int writePoint = 1024 * 1024 * 12;

        @Override
        public Object call() throws Exception {
            boolean returnStatus = false;
            try {
                Long start = System.currentTimeMillis();
                byte[] writeBuffer = new byte[writeBufferSize]; // 创建一个 16MB的 buffer
                int writeBufferPointer = 0;
                byte[] readBuffer = new byte[readBufferSize]; // buffer 改为 16k 大小,因为经过测试，每次read操作最大为16k内容
                int len;
                int i = 0;
                double totallen = 0.0;
                while ((len = in.read(readBuffer)) != -1) {
                    totallen += len;
                    i++;
                    //再从bytes中写入文件
                    if (i % 32 == 0) {
                        if (size > mb) {
                            logger.info("下载进程:[" + df.format(100 * totallen / size) + "%]-[" + (size / mb) + "M] | " + df.format(totallen * 1000 / 1024 / (System.currentTimeMillis() - start)) + "K/S " + filename);

                        } else {
                            logger.info("下载进程:[" + df.format(100 * totallen / size) + "%]-[" + (size / 1024) + "K] | " + df.format(totallen * 1000 / 1024 / (System.currentTimeMillis() - start)) + "K/S " + filename);
                        }
                    }
                    // 把read buffer 的0 ~ len 位置 写到 write buffer 的 writeBufferPointer 位置
                    System.arraycopy(readBuffer, 0, writeBuffer, writeBufferPointer, len);
                    writeBufferPointer += len;
                    if (writeBufferPointer > writePoint) {
                        out.write(writeBuffer, 0, writeBufferPointer);
                        writeBufferPointer = 0;
                    }
                    //
                }
                if (writeBufferPointer > 0)
                    out.write(writeBuffer, 0, writeBufferPointer);
                logger.info("下载进程:[100.0%]-[" + (size / 1024) + "K] | " + df.format((totallen * 1000 / 1024) / (System.currentTimeMillis() - start)) + "K/S " + filename);
                returnStatus = true;
            } catch (Exception e) {
                // TODO: 2019/4/10 call方法在下载线程中报错，在 downloadWithAutoRetry方法中无法捕捉，
                //
                logger.error("下载进程:失败 " + e.getMessage());

            }

            return returnStatus;
        }
    }

    public static boolean downloadWithAutoRetry(String urlStr, String filename, String referer, SpiderTask spiderTask) throws IOException {
        boolean downloadStatus = false;
        String tmpPath;
        if(spiderTask!=null)
        tmpPath = spiderTask.getTmpPath();
        else tmpPath = "C:\\Users\\MissionLee\\Desktop\\img";
//        int retry = spiderTask.getDownloadRetryTimes();
        int retry = 2;
        if (true||!spiderTask.existsInTmpPath(filename)) {
            while (!downloadStatus && retry > 0) {
                logger.info("尝试下载[" + (4 - retry) + "]: " + filename);
                retry--;
                InputStream in = null;
                OutputStream out = null;
                HttpURLConnection connection = null;
                String randomName = "random";
                try {
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    formatConnection(referer, connection);
                    long startTime = System.currentTimeMillis();
                    int fileSize = connection.getContentLength();
                    int responseCode = connection.getResponseCode();
                    if (200 == responseCode) {
                        in = connection.getInputStream();
                        long getInputStreamTime = System.currentTimeMillis();
                        randomName = java.util.UUID.randomUUID().toString();
                        out = new FileOutputStream(tmpPath + randomName);
                        CallableInputStreamDownloader downloader = new CallableInputStreamDownloader(in, out, fileSize, filename);
                        Future<Object> future = executorService.submit(downloader);
                        long timeout = fileSize / (downloadSpeedLimit * 1024);
                        if (timeout > TEN_MINUTES)
                            timeout = TEN_MINUTES;
                        boolean downloaded = (Boolean) future.get(timeout, TimeUnit.SECONDS);
                        if (downloaded)
                            downloadStatus = true;
                        long endTime = System.currentTimeMillis();
                        logger.info("下载成功[" + (3 - retry) + "]:[大小:" + fileSize + " | 总耗时:" + (endTime - startTime) / 1000 + " | 速度[K/S]:" + ((fileSize * 1000 / 1024) / (endTime - getInputStreamTime)) + " | " + filename + "]");

                    } else {
                        logger.error("下载失败[" + (3 - retry) + "]:服务器响应- " + responseCode + " " + filename);
                    }

                } catch (MalformedURLException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:错误的URL " + filename + urlStr + e.getMessage());
                } catch (ProtocolException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:Connection配置错误" + filename + e.getMessage());
                } catch (IOException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:建立输入流/输出流 出错或超时" + filename + e.getMessage());
                } catch (InterruptedException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:下载任务意外中断" + filename + e.getMessage());
                } catch (ExecutionException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:执行失败" + filename + e.getMessage());
                } catch (TimeoutException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:下载超时" + filename);
                } finally {
                    // 下面三个 if的顺序是有要设计的，只要 out.close执行成功，就可以进一步对文件进行操作
                    // in.close 可能会报错，并且因为 InputStream是从网络资源中获取的，所以报错概率也很大
                    // 但是此时实际上已经成功下载了文件，所以我们用下面的顺序来处理 finally
                    if (out != null)
                        out.close();

                    if (downloadStatus) {// 如果下载成功 临时名称，改为真正名称
                        File tmpFile = new File(tmpPath + randomName);
                        boolean success = spiderTask.saveFile(tmpFile, filename);
                        if (success)
                            logger.info("临时文件转存成功 " + filename);
                        else
                            logger.error("临时文件转存失败 " + filename);
                    } else {//如果下载失败 （超时等其他错误）  注意 stream 必须close之后，文件才能delete
                        new File(tmpPath + randomName).delete();
                    }
                    if (in != null)
                        in.close();
                    if (connection != null)
                        connection.disconnect();
                }
            }
        } else {

            downloadStatus = true;
        }
        return downloadStatus;
    }

    /**
     * @Description: 带自动重试
     * @Param: [urlStr 目标URL字符串形式, filename要保存为的文件名, savePath保存路径, referer Connection的referer字段, retry 尝试下载次数]
     * @return: boolean
     * @Throw:
     * @Author: Mission Lee
     * @date: 2019/4/10
     */
    @Deprecated
    public static boolean downloadWithAutoRetry(String urlStr, String filename, String savePath, String referer, int retry) throws IOException {

        boolean downloadStatus = false;
        File saveDir = new File(savePath);
        if (!saveDir.exists()) saveDir.mkdir();
        File aimFile = new File(savePath + filename);
        if (!aimFile.exists()) { // ！important 在SankakuDownloadSpider中有本地文件检测机制，
            // 但是如果出现文件已经正常下载，并重命名，但是最后 in.close的时候报错
            // 此错误由本方法抛出，倒是外部[SankakuDownloadSpider]使用方法判定下载失败，
            // 触发外部重新下载机制,这是程序会自动返回下载成功，而不进行重新下载
            while (!downloadStatus && retry > 0) { // 如果没有下载成功，并且重试次数没有用尽，就进行下载尝试
                logger.info("尝试下载[" + (4 - retry) + "]: " + filename);
                retry--;
                InputStream in = null;
                OutputStream out = null;
                HttpURLConnection connection = null;
                String randomName = "random";
                try {
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    formatConnection(referer, connection);
                    long startTime = System.currentTimeMillis();
                    int fileSize = connection.getContentLength();
                    int responseCode = connection.getResponseCode();
                    if (200 == responseCode) {
                        in = connection.getInputStream();
                        long getInputStreamTime = System.currentTimeMillis();
                        randomName = java.util.UUID.randomUUID().toString();
                        out = new FileOutputStream(savePath + randomName);
                        CallableInputStreamDownloader downloader = new CallableInputStreamDownloader(in, out, fileSize, filename);
                        Future<Object> future = executorService.submit(downloader);
                        long timeout = fileSize / (downloadSpeedLimit * 1024);
                        if (timeout > TEN_MINUTES)
                            timeout = TEN_MINUTES;
                        boolean downloaded = (Boolean) future.get(timeout, TimeUnit.SECONDS);
                        if (downloaded)
                            downloadStatus = true;
                        long endTime = System.currentTimeMillis();
                        logger.info("下载成功[" + (3 - retry) + "]:[大小:" + fileSize + " | 总耗时:" + (endTime - startTime) / 1000 + " | 速度[K/S]:" + ((fileSize * 1000 / 1024) / (endTime - getInputStreamTime)) + " | " + filename + "]");

                    } else {
                        logger.error("下载失败[" + (3 - retry) + "]:服务器响应- " + responseCode + " " + filename);
                    }

                } catch (MalformedURLException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:错误的URL " + filename + urlStr + e.getMessage());
                } catch (ProtocolException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:Connection配置错误" + filename + e.getMessage());
                } catch (IOException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:建立输入流/输出流 出错或超时" + filename + e.getMessage());
                } catch (InterruptedException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:下载任务意外中断" + filename + e.getMessage());
                } catch (ExecutionException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:执行失败" + filename + e.getMessage());
                } catch (TimeoutException e) {
                    logger.error("下载失败[" + (3 - retry) + "]:下载超时" + filename);
                } finally {
                    // 下面三个 if的顺序是有要设计的，只要 out.close执行成功，就可以进一步对文件进行操作
                    // in.close 可能会报错，并且因为 InputStream是从网络资源中获取的，所以报错概率也很大
                    // 但是此时实际上已经成功下载了文件，所以我们用下面的顺序来处理 finally
                    if (out != null)
                        out.close();

                    if (downloadStatus) {// 如果下载成功 临时名称，改为真正名称
                        File tmpFile = new File(savePath + randomName);

                        tmpFile.renameTo(aimFile);
                    } else {//如果下载失败 （超时等其他错误）  注意 stream 必须close之后，文件才能delete
                        new File(savePath + randomName).delete();
                    }
                    if (in != null)
                        in.close();
                    if (connection != null)
                        connection.disconnect();
                }
            }
        } else {
            downloadStatus = true;
        }

        return downloadStatus;
    }

    /**
     * @Description: 使用HttpURLConnection 下载资源
     * 下载时间如果过长会强制返回，速度使用downloadSpeedLimit 作为下限
     * @Param: [urlStr  网络资源全限定名字符串
     * , filename  文件名
     * , savePath  要放的目录
     * , referer
     * ]
     * @return:int 0: success 1: socketTimeout/downloadTimeout
     * @Author: Mission Lee
     * @date: 2019/3/2
     * @Deprecated: 原本下载与重试方案为：在
     */
    @Deprecated
    public static boolean download(String urlStr, String filename, String savePath, String referer) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        boolean status = false;

        // FileUtils 有父路径不存在自动船舰的功能，但是 FileOutputStream没有这个能力，所以要自己判断一下
        File saveDir = new File(savePath);
        if (!saveDir.exists())
            saveDir.mkdir();

        URL url = new URL(urlStr);

        /**
         //打开url连接  可以用普通的URLConnection,但是根据后台的不同，有些后台回对普通的URLConnection返回500错误
         //            更保险的形式，我们把Connection换成HttpURLConnection，因为浏览器使用这种方式来创建链接
         //            “GET/POST” 的设置是否恰当会从 405错误看出来
         */
        // 此处抛出IOException
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //请求超时时间
        formatConnection(referer, connection);
        long start = System.currentTimeMillis();

        // 获取输入流 此处抛出 IOException
        InputStream in = connection.getInputStream();
        logger.info("web downloader getInputStream - time" + (System.currentTimeMillis() - start));
        int fileSize = connection.getContentLength();


        String randomName = java.util.UUID.randomUUID().toString();
        /**  关于FileNotFoundException
         * 1. 如果给定name指向一个目录
         * 2. 给定文件不存在 同时 不能被创建  【暗示会创建文件】
         * 3. 没法打开
         * ！！！ 这里因为我本机的环境，基本不会出现报错的
         * */
        // 输出流 此处抛出 FileNotFoundException
        OutputStream out = new FileOutputStream(savePath + "\\" + randomName);

        long startReadBytes = System.currentTimeMillis();
        CallableInputStreamDownloader downloader = new CallableInputStreamDownloader(in, out, fileSize, filename);
        Future<Object> future = executorService.submit(downloader);
        long timeout = 10;
        try {


            if (in != null && out != null) {
                timeout = fileSize / (downloadSpeedLimit * 1024);
                if (timeout > TEN_MINUTES)// 如果超时时间大于十分钟，那么就设置为10分钟
                    // 简单计算 如果限速 5k/s 20MB的超时时间超过一小时
                    timeout = TEN_MINUTES;
                future.get(timeout, TimeUnit.SECONDS);
                status = true;
            }
        } catch (TimeoutException e) {
            long timeoutReadBytes = System.currentTimeMillis();
            logger.error(" - 下载超时: [" + timeout + "|" + ((timeoutReadBytes - startReadBytes) / 1000) + "] " + filename);
        } finally {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (!status) {//如果下载失败 （超时等其他错误）  注意 stream 必须close之后，文件才能delete
                new File(savePath + "\\" + randomName).delete();
            }
        }

        long endReadBytes = System.currentTimeMillis();
        if ((endReadBytes - startReadBytes) / 1000 > 0)
            logger.info("Speed:" + ((fileSize / 1024) / ((endReadBytes - startReadBytes) / 1000)) + "K/s");
        else
            logger.info("Speed: downloadWithAutoRetry time less than 1 second");
        // MissionLee ： 为什么要用随机命名，然后重命名 ？ 因为如果程序被中断，可能留下错误文件，
        //                而程序的验证机制是验证名称。
        // 这里不用判断状态也可以，因为如果报错了，根本走不到这里
        if (status) {
            File tmpFile = new File(savePath + "\\" + randomName);
            File aimFile = new File(savePath + "\\" + filename);
            tmpFile.renameTo(aimFile);
        }
        return status;
    }

    private static void formatConnection(String referer, HttpURLConnection connection) throws ProtocolException {
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("GET");
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
}

