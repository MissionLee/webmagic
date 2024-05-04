package pers.missionlee.chan.pagedownloader;

import com.mysql.cj.log.Log;
import com.sun.jna.platform.unix.X11;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
//import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.PlainText;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * MissionLee：使用ChromeDriver创建的Downloader
 * WebDriver需要启动浏览器实例，如果用其加载虽有页面会产生大量 图片、视频流量，用此实现兼顾 爬取与效率两者
 * 2024-02-20
 */
public class MixDownloader implements Downloader {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String chromePath;
    private String chromeDriverPath;
    private String debuggingPort;
    private Set<String> urlPatterns;
    private HttpClientDownloader httpClientDownloader;
    private ChromeDriver chromeDriver;

    /**
     * chromePath: 爬虫用的谷歌浏览器路径，用于系统启动浏览器
     * chromeDriverPath: Chrome浏览器WebDriver路径
     * debuggingPort: 启动爬虫用浏览器的调试端口
     * urlPatterns: 需要使用WebDriver的地址
     */
    public MixDownloader(String chromePath, String chromeDriverPath, String debuggingPort) throws IOException {
        this(chromePath, chromeDriverPath, debuggingPort, null);
    }


    public MixDownloader(String chromePath, String chromeDriverPath, String debuggingPort, Set<String> urlPatterns) throws IOException {
        if (null == urlPatterns) {
            urlPatterns = new HashSet() {
                {
                    add(".*page.*");
                    add("*tags*");
                }
            };
        }
        this.chromePath = chromePath;
        this.chromeDriverPath = chromeDriverPath;
        this.debuggingPort = debuggingPort;
        this.urlPatterns = urlPatterns;
        this.init();
    }

    private void init() throws IOException {
        logger.warn("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#");
        logger.warn("爬虫系统将使用WebDriver与HttpClient混合模式工作"
                + "\nChrome浏览器路径 " + chromePath
                + "\nChrome浏览器WebDriver路径" + chromeDriverPath
                + "\n调试端口 " + debuggingPort
                + "\n识别URL " + this.urlPatterns
        );
        logger.warn("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#");

        try {
            refresh();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // 配置HttpClientDownloader
//        this.httpClientDownloader = new HttpClientDownloader();
    }

    Process process;
    boolean debugPortFlag = true;

    public void refresh() throws IOException, InterruptedException {
        // 1.干掉所有 chrome浏览器
        WebDriverUtils.killChrome();
        // 2.释放 chromeDrier
        chromeDriver = null;
        // 3.获取一个可用的 port
        debuggingPort = "" + WebDriverUtils.getFreePort(Integer.parseInt(debuggingPort));

        logger.info("Refresh-新端口" + debuggingPort);
        // 启动
        String cmd = "chrome.exe --remote-debugging-port=" + debuggingPort;
        Thread.sleep(5000);

        Runtime.getRuntime().exec(cmd);
        logger.info("Refresh-启动浏览器");
        // 配置 chromeDriver
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        logger.info("重启浏览器 重新配置ChromeDriver sleep10秒");
        Thread.sleep(10000);
        // 判断给定端口是否存活
//        try {
//            Socket socket = new Socket("127.0.0.1", Integer.parseInt(this.debuggingPort));
//            logger.info("端口 " + this.debuggingPort + " 可以链接");
//            socket.close();
//        } catch (IOException e) {
//            logger.error("端口 " + this.debuggingPort + " 无法链接；请确认浏览器在指定端口启动");
//            e.printStackTrace();
//            System.exit(-1);
//            throw new RuntimeException(e);
//        }
        // 配置 WebDriver
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:" + this.debuggingPort);
        this.chromeDriver = new ChromeDriver(options);
        logger.info("Refresh-创建ChromeDriver");
//        chromeDriver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);


    }


    @Override
    public Page download(Request request, Task task) {
        // 抄写HttpClientDownloader的验证
        if (task == null || task.getSite() == null) {
            throw new NullPointerException("task or site can not be null");
        }
        String url = request.getUrl();
        logger.info("判断链接类型，选择Downloader");
        if (true || url.contains("tags")) {
            logger.info("使用WebDriver下载");
            try {
                return downloadWithChromeDriver(request, task);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
            }
        } else {
            logger.info("使用HttpClient下载");
            return httpClientDownloader.download(request, task);
        }
    }

    private static int counter = 0;
    public static int calledTime = 33; // 自动翻页机制
    public static boolean touchBottom;
    //    public static int last_length = 0;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    static class ChromeDriverDownloadTask implements Callable<ChromeDriver> {
        Logger logger = LoggerFactory.getLogger(ChromeDriverDownloadTask.class);
        public ChromeDriver chromeDriver;
        public Request request;
        public static String scrollY = "800";
        public ChromeDriverDownloadTask(ChromeDriver chromeDriver, Request request) {
            this.chromeDriver = chromeDriver;
            this.request = request;
        }

        @Override
        public ChromeDriver call() throws Exception {

            chromeDriver.get(request.getUrl());
            if (request.getUrl().contains("/article") && request.getUrl().contains("space")) {
                int l_orig = 0;
                logger.info("本次将进行 "+calledTime+" 此翻页，三次翻页 "+scrollY+" 像素");
                for (int i = 0; i < calledTime; i++) {
//                    ((JavascriptExecutor) chromeDriver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    ((JavascriptExecutor) chromeDriver).executeScript("window.scrollTo(0, window.scrollY+"+scrollY+");");
//                    logger.info("自动翻页次数" + i + "/" + calledTime);
//                    int length = getPageStringLengthFromChromeDriver(chromeDriver);
//                    logger.info("上个循环页面长度/本次长度 "+l_orig+"/"+length);
//                    if (length - l_orig > 5) {
//                        MixDownloader.touchBottom = false;
//                        l_orig = length;
//                        logger.info("页面长度增加超过20，应该是加载了新页面了");
//                    } else {
//                        MixDownloader.touchBottom = true;
//                        logger.info("页面长度增加不到20，应该是到头了吧");
//                        break;
//                    }
                    Thread.sleep(3000);
                }
                calledTime++;


            }

//            String content = chromeDriver.findElement(By.xpath("/html")).getAttribute("outerHTML");
//            if (content.contains("article-content")) {
//                for (int i = 0; i < 30; i++) {
//                    ((JavascriptExecutor) chromeDriver).executeScript("window.scrollTo(0, window.scrollY+1000);");
//                    Thread.sleep(1000);
//                }
//            }


            return chromeDriver;
        }
    }

    public static Page getPageFromChromeDriver(ChromeDriver driver, Request request) {
        WebElement webElement = driver.findElement(By.xpath("/html"));
        String content = webElement.getAttribute("outerHTML");
        Page page = new Page();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        return page;
    }

    public static int getPageStringLengthFromChromeDriver(ChromeDriver driver) {
        WebElement webElement = driver.findElement(By.xpath("/html"));
        System.out.println(webElement.getSize());
        return webElement.getSize().getHeight();
    }

    private synchronized Page downloadWithChromeDriver(Request request, Task task) throws IOException, InterruptedException {
        logger.info("downloading page " + request.getUrl());
        // 下载页面
        counter++;
        Future<ChromeDriver> future = executor.submit(new ChromeDriverDownloadTask(chromeDriver, request));
        try {
            chromeDriver = future.get(3000, TimeUnit.SECONDS);

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
//            throw new RuntimeException(e);
            logger.error("XXYYXXYY  发生了超时问题 超过 300秒 ChromeDriver没有下载完成 ");
            logger.error("XXYYXXYY  chromeDriver置为null 调用Refresh() 重启应用");
            chromeDriver = null;
            refresh();
            counter = 0;
            downloadWithChromeDriver(request, task);
        }
//        try{
//            chromeDriver.get(request.getUrl()); // ⭐⭐ 此处 readtimeout
//        }catch (Exception e){
//            if(e instanceof SocketTimeoutException){
//                refresh();
//                counter = 0;
//                downloadWithChromeDriver(request,task);
//            }
//        }

        // 获取cookie
        // TODO: 2024/2/29 暂时不操作，测试一下情况
        Set<Cookie> cookies = chromeDriver.manage().getCookies();
        Site site = task.getSite();
        for (Cookie c : cookies
        ) {
            site.addCookie(c.getName(), c.getValue());
        }
        // 处理页面
        WebElement webElement = chromeDriver.findElement(By.xpath("/html"));
//        chromeDriver.executeScript()

        logger.info("加载页面后等待3秒");
        Thread.sleep(3000);

        String content = webElement.getAttribute("outerHTML");
        Page page = new Page();
        page.setRawText(content);
//        page.setHtml(new Html(content, request.getUrl()));
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        if (counter > restartLimit) {
            counter = 0;
            refresh();
        } else {
            logger.info("第 " + counter + "/5000 次");
        }
        return page;
    }

    public static int restartLimit = 100;

    @Override
    public void setThread(int threadNum) {

    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().exec("chrome.exe --remote-debugging-port=" + "9292");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
