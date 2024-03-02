package pers.missionlee.chan.filedownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-25 13:46
 */
public class CallableHttpRangeDownloader implements Callable {
    Logger logger = LoggerFactory.getLogger(CallableHttpRangeDownloader.class);
    String url;
    String referer;
    RandomAccessFile file;
    String range;
    CountDownLatch latch;
    Map<String,Boolean> rangeDownloaded;
    public static int readBufferSize = 1024*16;
    int fileSeekPos;
    public static int retryLimit = 1;

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
            retry--;
            try {
//                URL url = new URL(this.url);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                HttpURLConnection connection = HCaptchaConnectionFormat.format(this.url,this.referer, "GET");
                connection.setRequestProperty("Range", this.range);
                logger.info("下载 "+this.range);
                logger.info("下载 "+(retryLimit-retry)+"/"+retryLimit);
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
                        logger.info("下载 "+this.range+" offset:"+offset+" "+this.url);
                }
//                    inputStream.close();
                logger.info("下载 完成一个 latch.countDown");
                latch.countDown();
                countDowned = true;
                success =true;
                rangeDownloaded.put(this.range,true);// 只有下载成功运行到这里，才会标记下载成功
            } catch (IOException e) {
                logger.error("下载失败 "+this.url);
                e.printStackTrace();
            } finally {
                if (!(inputStream == null)) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
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
