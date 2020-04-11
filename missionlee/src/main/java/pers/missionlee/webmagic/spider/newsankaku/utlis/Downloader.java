package pers.missionlee.webmagic.spider.newsankaku.utlis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.concurrent.*;

public class Downloader {
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static Logger logger = LoggerFactory.getLogger(Downloader.class);
    private static int downloadSpeedLimit = 5;
    private static DecimalFormat df = new DecimalFormat(".00");
    private static int mb = 1024 * 1024;
    private static long MAX_DOWNLOAD_LIMIT_SECONDS = 100 * 60L;

    private static class CallableStreamDownloader implements Callable {
        InputStream in;
        OutputStream out;
        int size;
        String filename;

        public CallableStreamDownloader(InputStream in, OutputStream out, int size, String filename) {
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
                    if (i % 128 == 0) {
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

    public static boolean download(String urlStr, String filename, String referer, TaskController task, ArtworkInfo artworkInfo) throws IOException {
        System.out.println("download");
        boolean success = false; // 下载成功
        boolean stored = false; // 保存成功
        String tmpPath = task.getTempPath();
        int retry = task.getRetryLimit();
        while (!(success && stored) && retry-- > 0) { // 如果下载和保存不成功，并且还没超过重试限制，级重试
            if (task.existOnDisk(filename)) {
                // 已经下载了，但是没有对应的数据记录
                System.out.println("已经存在该文件 " + filename);
                task.storeFile(new File(""), filename, artworkInfo, true);
                success = true;
                stored = true;
            } else {
                System.out.println("尝试下载：" + (task.getRetryLimit() - retry));
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
                        CallableStreamDownloader downloader = new CallableStreamDownloader(in, out, fileSize, filename);
                        Future<Object> future = executorService.submit(downloader);
                        long timeout = fileSize / (downloadSpeedLimit * 1024);
                        if (timeout > MAX_DOWNLOAD_LIMIT_SECONDS)
                            timeout = MAX_DOWNLOAD_LIMIT_SECONDS;
                        boolean downloaded = (Boolean) future.get(timeout, TimeUnit.SECONDS);
                        if (downloaded)
                            success = true;
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

                    if (success) {// 如果下载成功 临时名称，改为真正名称
                        File tmpFile = new File(tmpPath + randomName);
                        stored = task.storeFile(tmpFile, filename, artworkInfo, false);
                        if (stored)
                            logger.info("临时文件转存成功 " + filename);
                        else {
                            logger.error("临时文件转存失败 " + filename);

                        }
                    } else {//如果下载失败 （超时等其他错误）  注意 stream 必须close之后，文件才能delete
                        new File(tmpPath + randomName).delete();
                    }
                    if (in != null)
                        in.close();
                    if (connection != null)
                        connection.disconnect();

                }
            }


        }
        return success && stored;

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
