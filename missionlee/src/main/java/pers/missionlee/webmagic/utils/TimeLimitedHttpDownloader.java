package pers.missionlee.webmagic.utils;


import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * @description: 用于下载网络资源 ： 图片 视频 音频等
 * @author: Mission Lee
 * @create: 2019-02-27 10:05
 */

public class TimeLimitedHttpDownloader {
    private static int downloadSpeedLimit = 20; // Unit: k/s

    private static class CallableInputStreamDownloader implements Callable {
        InputStream in;
        OutputStream out;
        int size;

        public CallableInputStreamDownloader(InputStream in, OutputStream out, int size) {
            this.in = in;
            this.out = out;
            this.size = size;
        }

        @Override
        public Object call() throws Exception {
            byte[] bytes = new byte[1024 * 8];
            int len;
            int i = 0;
            while ((len = in.read(bytes)) != -1) {
                i++;
                //再从bytes中写入文件
                if (i % 128 == 0) { // buffer size 64k 每 1MB打印一次正在下载
                    System.out.println(Thread.currentThread() + "downloading-[" + i * 8 * 1024 + "/" + size + "]");
                }
                out.write(bytes, 0, len);
            }
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
    public static int download(String urlStr, String filename, String savePath, String referer) throws IOException, ExecutionException, InterruptedException {

        URL url = new URL(urlStr);

        /**
         //打开url连接  可以用普通的URLConnection,但是根据后台的不同，有些后台回对普通的URLConnection返回500错误
         //            更保险的形式，我们把Connection换成HttpURLConnection，因为浏览器使用这种方式来创建链接
         //            “GET/POST” 的设置是否恰当会从 405错误看出来
         */
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //请求超时时间
        formatConnection(referer, connection);
        //输入流
        long start = System.currentTimeMillis();
        InputStream in = null;
        // TODO: 2019/3/9 原本用try-catch的方式返回1，现在如果报错，外层会进入处理机制
//        try {
            in = connection.getInputStream();
//        } catch (SocketTimeoutException e) {
//            e.printStackTrace();
//            return 1;
//        }
        System.out.println("web downloader getInputStream - time" + (System.currentTimeMillis() - start));
        int size = connection.getContentLength();

        // 父目录不存在的时候，创建这个文件夹
        File file = new File(savePath);
        if (!file.exists())
            file.mkdirs();

        String UUID = java.util.UUID.randomUUID().toString();
        /**  关于FileNotFoundException
         * 1. 如果给定name指向一个目录
         * 2. 给定文件不存在 同时 不能被创建  【暗示会创建文件】
         * 3. 没法打开
         * ！！！ 这里因为我本机的环境，基本不会出现报错的
         * */
        OutputStream out = new FileOutputStream(file.getPath() + "\\" + UUID);

        long startReadBytes = System.currentTimeMillis();

        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        CallableInputStreamDownloader downloader = new CallableInputStreamDownloader(in, out, size);
        Future<Object> future = executorService.submit(downloader);
        try {
            if (in != null && out != null)
                future.get(size / (downloadSpeedLimit * 1024), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
            return 1;
        } catch (Exception e){
            e.printStackTrace();

        }finally {
            executorService.shutdown();
        }

        long endReadBytes = System.currentTimeMillis();
        if ((endReadBytes - startReadBytes) / 1000 > 0)
            System.out.println("Speed:" + ((size / 1024) / ((endReadBytes - startReadBytes) / 1000)) + "K/s");
        else
            System.out.println("Speed: download time less than 1 second");
        //关闭IO
        if (out != null)
            out.close();
        if (in != null)
            in.close();
        // MissionLee ： 为什么要用随机命名，然后重命名 ？ 因为如果程序被中断，可能留下错误文件，
        //                而程序的验证机制是验证名称。
        new File(file.getPath() + "\\" + UUID).renameTo(new File(file.getPath() + "\\" + filename));
        return 0;
    }

    private static void formatConnection(String referer, HttpURLConnection connection) throws ProtocolException {
        connection.setConnectTimeout(50000);
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

