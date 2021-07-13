package pers.missionlee.webmagic.spider.newsankaku.utlis;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.source.FakeSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.ArtistTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.FakeTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Downloader {
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private static Logger logger = LoggerFactory.getLogger(Downloader.class);
    private static int downloadSpeedLimit = 1;
    private static DecimalFormat df = new DecimalFormat(".00");
    private static int mb = 1024 * 1024;
    private static int blockSize = 32 * 1024 * 1024; // 每个连接做多下载 256mb大小内容
    public static HttpClient httpClient = HttpClientBuilder.create().build();
    public static CloseableHttpClient closeableHttpClient = HttpClients.custom().build();

    public static RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(2 * 60 * 60 * 1000)
            .setSocketTimeout(2 * 60 * 60 * 1000)
            .build();

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
                            logger.info("【Callable】下载情况:[" + df.format(100 * totallen / size) + "%]-[" + (size / mb) + "M] | " + df.format(totallen * 1000 / 1024 / (System.currentTimeMillis() - start)) + "K/S " + filename);

                        } else {
                            logger.info("【Callable】下载情况:[" + df.format(100 * totallen / size) + "%]-[" + (size / 1024) + "K] | " + df.format(totallen * 1000 / 1024 / (System.currentTimeMillis() - start)) + "K/S " + filename);
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
                logger.info("【Callable】下载情况:[100.0%]-[" + (size / 1024) + "K] | " + df.format((totallen * 1000 / 1024) / (System.currentTimeMillis() - start)) + "K/S " + filename);
                returnStatus = true;
            } catch (Exception e) {
                // TODO: 2019/4/10 call方法在下载线程中报错，在 downloadWithAutoRetry方法中无法捕捉，
                //
                logger.error("【Callable】下载情况:失败 " + e.getMessage());
                return false;

            }

            return returnStatus;
        }
    }

    public static boolean download2(String urlStr, String filename, String referer, TaskController task, ArtworkInfo artworkInfo) throws IOException {
        System.out.println("download");
        boolean success = false; // 下载成功
        boolean stored = false; // 保存成功
        String tmpPath = task.getTempPath();
        int retry = task.getRetryLimit();
        while (!(success && stored) && retry-- > 0) { // 如果下载和保存不成功，并且还没超过重试限制，级重试

            if (task.existOnDisk(artworkInfo) || task.fileNameExistsInDB(artworkInfo)) {
                // 如果当前不是 book大类下载，那么只要数据库中有，就算有
                System.out.println("已经存在该文件 " + filename);
                task.storeFile(new File(""), filename, artworkInfo, true, false);
                success = true;
                stored = true;
            } else {
                System.out.println("尝试下载：" + (task.getRetryLimit() - retry));
                InputStream in = null;
                OutputStream out = null;
                HttpURLConnection connection = null;
                String randomName = "random";
                try {
//                  以下四行是 HttpClient 下载方法用的代码
//                    HttpGet get = new HttpGet(urlStr);
//                    formatHttpGet(urlStr,referer,get);
//                    HttpResponse response = httpClient.execute(get);
//                    int responseCode = response.getStatusLine().getStatusCode();

//                    以下三行 是 URL 下载方法用的代码
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    formatConnection(referer, connection);
                    long startTime = System.currentTimeMillis();
                    int fileSize = connection.getContentLength();
                    int responseCode = connection.getResponseCode();
                    System.out.println("响应码 ResponseCode:" + responseCode);
                    randomName = java.util.UUID.randomUUID().toString();
                    if (200 == responseCode || 206 == responseCode) {
                        if (fileSize < blockSize) {
                            // 这3行是 HttpClient下载方法用的
//                        HttpEntity entity = response.getEntity();
//
//                        in = entity.getContent();
//                        int fileSize = (int)entity.getContentLength();
                            // 这一行是URL 下载方法用的 获取连接文件内容的 输入流
                            in = connection.getInputStream();
                            System.out.println("文件：" + filename + " -> 大小 -> " + fileSize * 1.0 / (1024 * 1024) + "MB");
                            long getInputStreamTime = System.currentTimeMillis();
                            // 创建一个文件输出流
                            out = new FileOutputStream(tmpPath + randomName);
                            // 创建一个限制时的下载任务
                            CallableStreamDownloader downloader = new CallableStreamDownloader(in, out, fileSize, filename);
                            // 提交下载任务
                            Future<Object> future = executorService.submit(downloader);
                            long timeout = fileSize * 8 / (downloadSpeedLimit * 1024);

//                        if (timeout > MAX_DOWNLOAD_LIMIT_SECONDS)
//                            timeout = MAX_DOWNLOAD_LIMIT_SECONDS;
                            // 设置超时时间，并且获取下载结果 成功与否
                            boolean downloaded = (Boolean) future.get(timeout, TimeUnit.SECONDS);
                            if (downloaded)
                                success = true;
                            long endTime = System.currentTimeMillis();
                            logger.info("下载成功[" + (3 - retry) + "]:[大小:" + fileSize + " | 总耗时:" + (endTime - startTime) / 1000 + " | 速度[K/S]:" + ((fileSize * 1000 / 1024) / (endTime - getInputStreamTime)) + " | " + filename + "]");

                        } else {
                            int blockNum = new Double(Math.ceil(fileSize / blockSize)).intValue();
                            int offset = 0;
                            for (int i = 0; i < blockNum; i++) {
                                long end = offset + blockSize - 1;
                                if (end > blockSize) end = blockSize - 1;
                                String rangeStr = "bytes=" + offset + "-" + end;

                                //================= 启动下载
                                HttpGet httpGet = new HttpGet(urlStr);
                                httpGet.addHeader("Range", rangeStr);
                                HttpContext context = new BasicHttpContext();
                                CloseableHttpResponse response = closeableHttpClient.execute(httpGet, context);
                                BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());

                                byte[] buff = new byte[blockSize];
                                int bytesRead;
                                File block = new File(tmpPath + randomName);
                                RandomAccessFile raf = new RandomAccessFile(block, "rw");
                                while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
                                    raf.seek(offset);
                                    raf.write(buff, 0, bytesRead);
                                }
                                raf.close();
                                bis.close();

                                offset += blockSize;

                            }
                        }

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
                    logger.error("下载失败[" + (3 - retry) + "]:下载超时" + filename + " " + e.getMessage());


//                    HttpEntity entity = MultipartEntityBuilder.create().build();
//                    entity.

                } finally {
                    // 下面三个 if的顺序是有要设计的，只要 out.close执行成功，就可以进一步对文件进行操作
                    // in.close 可能会报错，并且因为 InputStream是从网络资源中获取的，所以报错概率也很大
                    // 但是此时实际上已经成功下载了文件，所以我们用下面的顺序来处理 finally
                    if (out != null)
                        out.close();

                    if (success) {// 如果下载成功 临时名称，改为真正名称
                        File tmpFile = new File(tmpPath + randomName);
                        stored = task.storeFile(tmpFile, filename, artworkInfo, false, false);
                        if (stored)
                            logger.info("临时文件转存成功 " + filename);
                        else {
                            logger.error("临时文件转存失败 " + filename);

                        }
                    } else {//如果下载失败 （超时等其他错误）  注意 stream 必须close之后，文件才能delete
                        new File(tmpPath + randomName).delete();
                    }
                    try {
                        Thread.sleep(1000 * 3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (in != null)

                        in.close();
                    if (connection != null)
                        connection.disconnect();

                }
            }
//            }


        }
        return success && stored;

    }

    public static boolean download(String url, String filename, String referer, TaskController taskController, ArtworkInfo artowrkInfo) throws IOException, InterruptedException {

        System.out.println("== 开始下载：" + filename + " URL:" + url);
        boolean downloadSuccess = false;
        boolean storeSuccess = false;
        String tempPath = taskController.getTempPath();
//        String tempPath = "F://ROOT/tmp-test/";
        int retryLimit = taskController.getRetryLimit();
//        int retryLimit = 3;
        while ((!(downloadSuccess && storeSuccess) )&& retryLimit-- > 0) {
            boolean exists = false;
            if(taskController instanceof ArtistTaskController){
                exists = taskController.existOnDisk(artowrkInfo);
            }else{
                exists = (taskController.existOnDisk(artowrkInfo) || taskController.fileNameExistsInDB(artowrkInfo));
            }

            if (exists) {
                System.out.println("文件已存在：" + filename + "_" + JSON.toJSONString(artowrkInfo));
                taskController.storeFile(new File(""), filename, artowrkInfo, true, false);
                downloadSuccess = true;
                storeSuccess = true;
            } else {
                System.out.println("尝试下载：" + (taskController.getRetryLimit() - retryLimit));
                // 先用 HEAD 请求获取文件长度
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                Downloader.formatConnection(referer, httpURLConnection);
                httpURLConnection.setRequestMethod("HEAD");
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode >= 400) {
                    System.out.println("下载问题：HEAD 请求失败");
                    break;
                } else {
                    int fileSize = httpURLConnection.getContentLength();
                    System.out.println("Downloader 添加了根据 fileSize Sleep的机制");
                    long sleepTime = 1000;
                    if(fileSize > 48*1024*1024){
                        sleepTime *= 0;
                    }else if(fileSize > 24*1024*1024){
                        sleepTime *= 0;
                    }else if(fileSize > 12*1024*1024){
                        sleepTime *= 0;
                    }else if(fileSize > 6*1024*1024){
                        sleepTime *= 1;
                    }else if(fileSize > 3*1024*1024){
                        sleepTime *=2;
                    }else if(fileSize > 1024*1024){
                        sleepTime *= 3;
                    }else if(fileSize > 512*1024){
                        sleepTime *= 4;
                    }else{
                        sleepTime *= 5;
                    }
                    Thread.sleep(sleepTime);
                    System.out.println(filename + "_总大小_" + (fileSize * 1.0 / (1024 * 1024)) + "MB[" + fileSize + "bytes]");
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
                        CallableHttpRangeDownloader downloader = new CallableHttpRangeDownloader(url, referer, tempFileAccess,start, range, latch, rangeDownload);
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
                        storeSuccess = taskController.storeFile(tempFile, filename, artowrkInfo, false, false);
//                        FileUtils.moveFile(tempFile,new File(tempPath+filename));
                        if (storeSuccess) {
                            logger.info("临时文件转存成功 " + filename);
                        } else {
                            logger.error("临时文件转存失败 " + filename);
                        }
                    }

                }
            }
        }
        return downloadSuccess && storeSuccess;
    }

    public static class CallableHttpRangeDownloader implements Callable {
        String url;
        String referer;
        RandomAccessFile file;
        String range;
        CountDownLatch latch;
        Map<String, Boolean> rangeDownloaded;
        public static int readBufferSize = 1024 * 16;
        int fileSeekPos;

        public CallableHttpRangeDownloader(String url, String referer, RandomAccessFile file, int fileSeekPos, String range, CountDownLatch latch, Map<String, Boolean> rangeDownloaded) {
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
            int retry = 3;
            while (retry > 0 && !success) {
                InputStream inputStream = null;
                try {
                    URL url = new URL(this.url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    Downloader.formatConnection(this.referer, connection);
                    connection.setRequestProperty("Range", this.range);
                    System.out.println("CountDown下载：" + this.range);
                    inputStream = connection.getInputStream();
                    byte[] buff = new byte[readBufferSize];
                    int bytesRead = -1;
                    int offset = this.fileSeekPos;
                    while ((bytesRead = inputStream.read(buff, 0, buff.length)) != -1) {
                        seekAndWriteFile(file, offset, buff, bytesRead);
                        offset += bytesRead;
                    }
//                    inputStream.close();
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


    private static void formatConnection(String referer, HttpURLConnection connection) throws ProtocolException {
        connection.setConnectTimeout(3000000);
        connection.setReadTimeout(3000000);

//        connection.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//        connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
//        connection.setRequestProperty("accept-language", "zh-CN,zh;q=0.9");
//        connection.setRequestProperty("cache-control", "no-cache");
//        connection.setRequestProperty("pragma", "no-cache");
        System.out.println(referer);
        connection.setRequestProperty("Referer", referer);
//        connection.setRequestProperty("upgrade-insecure-requests", "1");
        connection.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36");
        connection.setRequestProperty("cookie","__cfduid=dbf7e7773e9cff987a17a97a7985956eb1619305796; cf_chl_2=d75f3598eff7b80; cf_chl_prog=a9; cf_clearance=38dc314abf4c8bb334a48a8e34a898fb19612a26-1619314705-0-250; _sankakucomplex_session=BAh7CDoMdXNlcl9pZGkD5lgGIgpmbGFzaElDOidBY3Rpb25Db250cm9sbGVyOjpGbGFzaDo6Rmxhc2hIYXNoewAGOgpAdXNlZHsAOg9zZXNzaW9uX2lkIiU4ODJhNmQxOThlZTUxN2I3MjI0Y2Y5N2VjYTdlYTZlNw==--9c7b73bee2ac4e9e47ca127737b669aa22cc34f5");
        connection.setRequestMethod("GET");

    }
    // https://accounts.hcaptcha.com/verify_email/63fecc64-8ccc-4636-b541-2effd53027e8

    private static void formatHttpGet(String urlStr, String referer, HttpGet get) {
//        get.setProtocolVersion(new HttpVersion(2,0));

        get.setConfig(requestConfig);
//        get.setProtocolVersion(HttpVersion.HTTP_1_1);
        get.setHeader(":authority", "s.sankakucomplex.com");
        get.setHeader(":method", "GET");
        get.setHeader(":path", urlStr.substring("https:/".length()));
        get.setHeader(":scheme", "https");
        get.setHeader("accept", "*/*");
        get.setHeader("accept-encoding", "identity;q=1, *;q=0");
        get.setHeader("accept-language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
        get.setHeader("cache-control:", "no-cache");
        get.setHeader("cookie", "_sankakucomplex_session=BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiViY2IxMzExNThmMmQ1Mzc5MTcwZGVmMzhjMzk5NTMyNg%3D%3D--d2e080877159e86c18876580449a04f4c0f449bf");
        get.setHeader("pragma", "no-cache");
        get.setHeader("referer", referer);
//        get.setHeader("sec-fetch-dest","");
        get.setHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        download2("https://s.sankakucomplex.com/data/0f/b6/0fb69b4162a2e4ad1a60857d7cc35f95.mp4?e=1617505050&m=7EmSv1YTaiIzE1CB0ft9bQ"
        ,"0fb69b4162a2e4ad1a60857d7cc35f95.mp4",
                "https://chan.sankakucomplex.com/post/show/24356257",
                new FakeTaskController(new FakeSourceManager()),
                new ArtworkInfo());
        // https://s.sankakucomplex.com/data/e4/52/e4528f52cb7d82737ca200a39d16818e.gif?e=1617448910&amp;m=t52G8rRHVrOEBqnkUOwrOg
        // https://s.sankakucomplex.com/data/0f/b6/0fb69b4162a2e4ad1a60857d7cc35f95.mp4?e=1617442015&amp;m=wQJ3rSxWmS5pd2KHfjE9gQ
//        String url = "https://s.sankakucomplex.com/data/0f/b6/0fb69b4162a2e4ad1a60857d7cc35f95.mp4?e=1617463802&m=H07jkL5KHH9XQo8FR33yfA";
//        int blockSize = 1024 * 1024 * 128; // blockSize 1MB
//        String fileName = "0fb69b4162a2e4ad1a60857d7cc35f95.mp4";
//        String filePrefix = "F://ROOT/tmp-test/";
//        // 获取文件长度
//        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
//        Downloader.formatConnection("https://chan.sankakucomplex.com/post/show/24356257", httpURLConnection);
//        httpURLConnection.setRequestMethod("HEAD");
//        int responseCode = httpURLConnection.getResponseCode();
//        if (responseCode >= 400) {
//            System.out.println("error");
//        } else {
//            // 获取文件大小，预先创建指定大小的文件
//            int fileSize = httpURLConnection.getContentLength();
//            fileSize += 0;
//            System.out.println("总大小：" + fileSize);
//            RandomAccessFile cre =
//                    new RandomAccessFile(new File(filePrefix + fileName), "rw");
//            cre.setLength(fileSize);
//            cre.close();
//            System.out.println("文件大小： " + fileSize * 1.0 / (1024 * 1024));
//
//            // 循环写入
//            int blockNum = new Double(Math.ceil(fileSize * 1.0 / blockSize)).intValue();
//            System.out.println("分块个数：" + blockNum);
//            int offset = 0;
//
//            while (offset < fileSize) {// 起点必须效于 fileSize
//                long end = offset + blockSize - 1;
//                if (end >= fileSize) end = fileSize - 1;
//
//                String rangeStr = "bytes=" + offset + "-" + end;
//                //================= 启动下载
////                HttpGet httpGet = new HttpGet(url);
////                httpGet.addHeader("Range", rangeStr);
////                Downloader.formatHttpGet(url, "https://chan.sankakucomplex.com/post/show/24714191", httpGet);
////                HttpContext context = new BasicHttpContext();
////                CloseableHttpResponse response = closeableHttpClient.execute(httpGet, context);
////                BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
//
//                //================启动下载 另一种
//                URL uuu = new URL(url);
//                HttpURLConnection connection = (HttpURLConnection) uuu.openConnection();
//                Downloader.formatConnection("https://chan.sankakucomplex.com/post/show/24356257", connection);
//                connection.setRequestProperty("Range", rangeStr);
//                System.out.println(rangeStr);
//                InputStream in = connection.getInputStream();
//
//                byte[] buff = new byte[blockSize];
//                int bytesRead;
//                File block = new File(filePrefix + fileName);
//                RandomAccessFile raf = new RandomAccessFile(block, "rw");
//                //
//                while ((bytesRead = in.read(buff, 0, buff.length)) != -1) {
//                    System.out.println("read:" + bytesRead + "  offset:" + offset);
//                    raf.seek(offset);
//                    raf.write(buff, 0, bytesRead);
//                    offset += bytesRead;
//                    System.out.println("offset after write:" + offset);
//                }
//                raf.close();
//                in.close();
//
//
////                offset += blockSize;
//            }
//        }
//:authority: s.sankakucomplex.com
//:method: GET
//:path: /data/57/16/571687fd276b2f7745c8ce0a162f3d88.mp4?e=1610373363&m=-B8rraI7T9nYBJm0COy10Q
//:scheme: https
//    accept: */*
//accept-encoding: identity;q=1, *;q=0
//accept-language: en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7
//cache-control: no-cache
//cookie: _sankakucomplex_session=BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiViY2IxMzExNThmMmQ1Mzc5MTcwZGVmMzhjMzk5NTMyNg%3D%3D--d2e080877159e86c18876580449a04f4c0f449bf
//pragma: no-cache
//range: bytes=0-
//referer: https://chan.sankakucomplex.com/post/show/21578162
//sec-fetch-dest: video
//sec-fetch-mode: no-cors
//sec-fetch-site: same-site
//user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36
    }
}
