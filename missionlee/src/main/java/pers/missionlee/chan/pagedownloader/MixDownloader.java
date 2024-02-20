package pers.missionlee.chan.pagedownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;

import java.util.HashSet;
import java.util.Set;

/**
 * MissionLee：使用ChromeDriver创建的Downloader
 * WebDriver需要启动浏览器实例，如果用其加载虽有页面会产生大量 图片、视频流量，用此实现兼顾 爬取与效率两者
 * 2024-02-20
 * */
public class MixDownloader implements Downloader{

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String chromePath;
    private String chromeDriverPath;
    private String debuggingPort;
    private Set<String> urlPatterns;
    /**
     * chromePath: 爬虫用的谷歌浏览器路径，用于系统启动浏览器
     * chromeDriverPath: Chrome浏览器WebDriver路径
     * debuggingPort: 启动爬虫用浏览器的调试端口
     * urlPatterns: 需要使用WebDriver的地址
     * */
    public MixDownloader(String chromePath,String chromeDriverPath, String debuggingPort) {
        this(chromePath,chromeDriverPath,debuggingPort,null);
    }
    public MixDownloader(String chromePath,String chromeDriverPath, String debuggingPort, Set<String> urlPatterns) {
        if(null == urlPatterns){
            urlPatterns = new HashSet(){
                {
                    add(".*page.*");
                }
            };
        }
        this.chromePath=chromePath;
        this.chromeDriverPath=chromeDriverPath;
        this.debuggingPort=debuggingPort;
        this.urlPatterns=urlPatterns;
        logger.warn("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#");
        logger.warn("爬虫系统将使用WebDriver与HttpClient混合模式工作"
                +"\nChrome浏览器路径 "+chromePath
                +"\nChrome浏览器WebDriver路径"+chromeDriverPath
                +"\n调试端口 "+debuggingPort
                +"\n识别URL "+this.urlPatterns
        );
        logger.warn("#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#*#");
        // 配置 chromeDriver
        System.setProperty("webdriver.chrome.driver",chromeDriverPath);
    }

    @Override
    public Page download(Request request, Task task) {
        return null;
    }

    @Override
    public void setThread(int threadNum) {

    }
}
