package pers.missionlee.webmagic.utils;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.UUID;

/**
 * @description: 用于下载网络资源 ： 图片 视频 音频等
 * @author: Mission Lee
 * @create: 2019-02-27 10:05
 */
public class WebFileDownloader {
    /**
     * @Description:
     * @Param: [urlStr  网络资源全限定名字符串
     * , filename  文件名
     * , savePath  要放的目录
     * , referer
     * ]
     * @return: void
     * @Author: Mission Lee
     * @date: 2019/3/2
     */
    public static void download(String urlStr, String filename, String savePath, String referer) throws IOException {

        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
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
            return;
        }
        //请求超时时间
        connection.setConnectTimeout(50000);
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
            return;
        }
        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
        connection.setRequestProperty("cache-control", "no-cache");
        connection.setRequestProperty("pragma", "no-cache");
        connection.setRequestProperty("referer", referer);
        connection.setRequestProperty("upgrade-insecure-requests", "1");
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
        //输入流
        InputStream in = null;
        int size = 0;
        long start = System.currentTimeMillis();
        try {
            in = connection.getInputStream();
            // TODO: 2019/3/5 注意： InputStream在网络传输情况 available的大小不是整个流真正的大小，可以通过下面方法获取实际长度
            size = connection.getContentLength();
        } catch (IOException e) {
            if (in != null)
                in.close();
            e.printStackTrace();
            // 原本这里报错之后没有return 导致后面出现 空指针异常
            return;
        }
        System.out.println("web downloader getInputStream - time" + (System.currentTimeMillis()-start));
        //缓冲数据
        byte[] bytes = new byte[1024 * 8]; //
        //数据长度
        int len;
        // 父目录不存在的时候，创建这个文件夹
        File file = new File(savePath);
        if (!file.exists())
            file.mkdirs();

        OutputStream out = null;
        String UUID = java.util.UUID.randomUUID().toString();
        try {
            /**  关于FileNotFoundException
             * 1. 如果给定name指向一个目录
             * 2. 给定文件不存在 同时 不能被创建  【暗示会创建文件】
             * 3. 没法打开
             *
             * ！！！ 这里因为我本机的环境，基本不会出现报错的
             * */

            out = new FileOutputStream(file.getPath() + "\\" + UUID);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //先读到bytes中
        int i = 0;
        try {
            while ((len = in.read(bytes)) != -1) {
                i++;
                //再从bytes中写入文件
                if(i%128 == 0){ // buffer size 64k 每 1MB打印一次正在下载
                    System.out.println(Thread.currentThread()+"downloading-["+i*8*1024+"/"+size+"]");
                }
                out.write(bytes, 0, len);
            }
        } catch (IOException e) {
            /**
             * 如果写文件过程中发生错误，删除文件
             * */
            new File(savePath + "\\" + filename).delete();
            e.printStackTrace();
        }

        //关闭IO
        if (out != null)
            out.close();
        if (in != null)
            in.close();
        // MissionLee ： 为什么要用随机命名，然后重命名 ？ 因为如果程序被中断，可能留下错误文件，
        //                而程序的验证机制是验证名称。
        new File(file.getPath() + "\\" + UUID).renameTo(new File(file.getPath() + "\\"+filename));
    }
}

