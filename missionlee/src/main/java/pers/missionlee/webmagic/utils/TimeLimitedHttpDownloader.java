package pers.missionlee.webmagic.utils;


import com.sun.xml.internal.bind.v2.TODO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.concurrent.*;

/**
 * @description: 用于下载网络资源 ： 图片 视频 音频等
 * @author: Mission Lee
 * @create: 2019-02-27 10:05
 */

public class TimeLimitedHttpDownloader {
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static Logger logger = LoggerFactory.getLogger(TimeLimitedHttpDownloader.class);
    private static int downloadSpeedLimit = 5; // Unit: k/s
    private static DecimalFormat df = new DecimalFormat(".00");
    private static int mb = 1024 * 1024;
    private static class CallableInputStreamDownloader implements Callable {
        InputStream in;
        OutputStream out;
        int size;
        String filename;

        public CallableInputStreamDownloader(InputStream in, OutputStream out, int size,String filename) {
            this.in = in;
            this.out = out;
            this.size = size;
            this.filename=filename;
        }

        public static int readBufferSize = 1024 * 16;
        public static int writeBufferSize = 1024 * 1024 * 16;
        public static int writePoint = 1024 * 1024 * 12;

        @Override
        public Object call() throws Exception {
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
                        logger.info("[" + df.format(100 * totallen / size) + "%]-[" + (size / mb) + "M] | " + df.format(totallen * 1000 / 1024 / (System.currentTimeMillis() - start)) + "K/S "+filename);

                    } else {
                        logger.info("[" + df.format(100 * totallen / size) + "%]-[" + (size / 1024) + "K] | " + df.format(totallen * 1000 / 1024 / (System.currentTimeMillis() - start)) + "K/S "+filename);
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
            logger.info(" -[100.0%]-[" + (size / 1024) + "K] | " + df.format((totallen * 1000 / 1024) / (System.currentTimeMillis() - start)) + "K/S "+filename);
            return null;
        }
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
     */
    public static boolean download(String urlStr, String filename, String savePath, String referer) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        boolean status = false;

        // FileUtils 有父路径不存在自动船舰的功能，但是 FileOutputStream没有这个能力，所以要自己判断一下
        File saveDir = new File(savePath);
        if(!saveDir.exists())
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
        InputStream in =  connection.getInputStream();
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
        CallableInputStreamDownloader downloader = new CallableInputStreamDownloader(in, out, fileSize,filename);
        Future<Object> future = executorService.submit(downloader);
        try {


            if (in != null && out != null){
                future.get(fileSize / (downloadSpeedLimit * 1024), TimeUnit.SECONDS);
                status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if(!status){//如果下载失败 （超时等其他错误）  注意 stream 必须close之后，文件才能delete
                new File(savePath+"\\"+randomName).delete();
            }
        }
        connection.disconnect();
        long endReadBytes = System.currentTimeMillis();
        if ((endReadBytes - startReadBytes) / 1000 > 0)
            logger.info("Speed:" + ((fileSize / 1024) / ((endReadBytes - startReadBytes) / 1000)) + "K/s");
        else
            logger.info("Speed: download time less than 1 second");
        // MissionLee ： 为什么要用随机命名，然后重命名 ？ 因为如果程序被中断，可能留下错误文件，
        //                而程序的验证机制是验证名称。
        // 这里不用判断状态也可以，因为如果报错了，根本走不到这里
        if(status){
            File tmpFile = new File(savePath + "\\" + randomName);
            File aimFile = new File(savePath + "\\" + filename);
            tmpFile.renameTo(aimFile);
        }
        return status;
    }

    private static void formatConnection(String referer, HttpURLConnection connection) throws ProtocolException {
        connection.setConnectTimeout(50000);
        connection.setReadTimeout(50000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
        connection.setRequestProperty("cache-control", "no-cache");
        connection.setRequestProperty("pragma", "no-cache");
        connection.setRequestProperty("referer", referer);
        connection.setRequestProperty("upgrade-insecure-requests", "1");
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
    }
}

