package pers.missionlee.chan.downloader;

import pers.missionlee.webmagic.spider.newsankaku.utlis.Downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-25 13:46
 */
public class CallableHttpRangeDownloader implements Callable {
    String url;
    String referer;
    RandomAccessFile file;
    String range;
    CountDownLatch latch;
    Map<String,Boolean> rangeDownloaded;
    public static int readBufferSize = 1024*16;
    int fileSeekPos;
    public static int retryLimit = 3;

    public CallableHttpRangeDownloader(String url, String referer, RandomAccessFile file, String range, CountDownLatch latch, Map<String, Boolean> rangeDownloaded, int fileSeekPos) {
        this.url = url;
        this.referer = referer;
        this.file = file;
        this.range = range;
        this.latch = latch;
        this.rangeDownloaded = rangeDownloaded;
        this.fileSeekPos = fileSeekPos;
    }

    @Override
    public Object call() throws Exception {
        boolean success = false;
        boolean countDowned = false;
        int retry = retryLimit;
        while (retry > 0 && !success) {
            InputStream inputStream = null;
            try {
                retry--;
//                URL url = new URL(this.url);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                HttpURLConnection connection = HCaptchaConnectionFormat.format(this.url,this.referer, "GET");
                connection.setRequestProperty("Range", this.range);
                System.out.println("CountDown下载：" + this.range);
                System.out.println("重复次数："+(retryLimit-retry)+"/"+retryLimit);
                inputStream = connection.getInputStream();
                byte[] buff = new byte[readBufferSize];
                int bytesRead = -1;
                int offset = this.fileSeekPos;
                int i = 0;
                while ((bytesRead = inputStream.read(buff, 0, buff.length)) != -1) {
                    i ++;
                    seekAndWriteFile(file, offset, buff, bytesRead);
                    offset += bytesRead;
                    if(i%20 ==1 )
                    System.out.println(this.url+"_"+this.range+"====已经处理到offset："+offset);
                }
//                    inputStream.close();
                System.out.println("处理完了一个 block lath.countDown");
                latch.countDown();
                countDowned = true;
                success =true;
                rangeDownloaded.put(this.range,true);// 只有下载成功运行到这里，才会标记下载成功
            } catch (IOException e) {
                System.out.println("CountDown下载：报错");
                e.printStackTrace();
            } finally {
                if (!(inputStream == null)) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        System.out.println("InputStream 在finally之前就被意外关闭了");
                        e.printStackTrace();
                    }
                }
            }
        }
        if (!countDowned) {
            latch.countDown();
        }
        return null;
    }
    public static synchronized void seekAndWriteFile(RandomAccessFile file, int offset, byte[] buff, int length) throws IOException {
        file.seek(offset);
        file.write(buff, 0, length);
    }
}
