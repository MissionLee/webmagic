package pers.missionlee.chan.webdrivertest;



import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;

public class Test {
    public static void main(String[] args){
        // 1. 指定调试端口，启动 chrome浏览器
//        try {
//            System.out.println("启动edge 调试端口9292");
//            Runtime.getRuntime().exec("chrome.exe --remote-debugging-port=9292");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        // 2. Webdriver 链接已经启动的浏览器
//        EdgeOptions options = new EdgeOptions();
//        options.setPageLoadStrategy();
//        options.setCapability();
//        options.setPageLoadStrategy();
//        options("debuggerAddress","127.0.0.1:9292");


        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress","127.0.0.1:9292");
//        System.setProperty("webdriver.chrome.driver","C:\\msedgedriver.exe");
        System.setProperty("webdriver.chrome.driver","C:\\chromedriver-win64\\chromedriver.exe");
        WebDriver driver = new ChromeDriver(options);

        driver.get("https://www.baidu.com");
        WebElement e = driver.findElement(By.name("123"));
        driver.get("https://www.google.com");
        Set<Cookie> cookies =driver.manage().getCookies();
        for (Cookie cookie : cookies) {
            System.out.println(cookie);
        }
        driver.close();
    }
}
