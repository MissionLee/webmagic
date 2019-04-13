package pers.missionlee.webmagic.spider.sankaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.utils.TimeLimitedHttpDownloader;
import us.codecraft.webmagic.Page;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:24
 *
 * @Deprecated: 原本SankakuDownloadSpider 使用 SankakuDownloadUtils 提供的 [重下载] 机制调用TimeLimitedHttpDownloader进行下载
 *              ，并且在这个过程中，下载成功与否的状态值，
 */
@Deprecated
public class SankakuDownloadUtils {
    private static Logger logger = LoggerFactory.getLogger(SankakuDownloadUtils.class);
    private static List<String> fatalErrorPageURL = new ArrayList<String>();
    private static Map<String, Integer> pageRedoCounter = new HashMap<String, Integer>();
    private static Map<String, Integer> downloadErrorCounter = new HashMap<String, Integer>();
    public static boolean download(String downloadURL,String filename,String savePath,Page page){
        return download(downloadURL,filename,savePath,page,page.getUrl().toString());
    }
    public static boolean download(String downloadURL, String filename, String savePath, Page page, String pageURL) {

        boolean downloadStatus = false;
        try {
            logger.info("下载文件："+filename+" 下载次数: " + (downloadErrorCounter.containsKey(downloadURL) ? downloadErrorCounter.get(downloadURL) + 1 : 1));
            if (!new File(savePath +  filename).exists()) {
                downloadStatus = TimeLimitedHttpDownloader.download(downloadURL, filename, savePath, pageURL);
                if (downloadStatus) {
                    // 下载成功 remove 机制
                    logger.info("下载成功：" + filename);
                    downloadErrorCounter.remove(downloadURL);
                    pageRedoCounter.remove(pageURL);
                }else{
                    // 只有下载超时 是在TimeLimitedHttpDownloader 里面捕捉处理的
                    logger.warn("下载失败：下载超时 "+filename);
                    redownload(downloadURL,filename,savePath,page,pageURL);
                }
            } else {
                downloadStatus = true;
                logger.info("已经存在："+filename+" [跳过]");
            }
            return downloadStatus;
        } catch (Exception e) { // 如果下载失败
            logger.warn("下载失败：其他错误 "+filename);
            e.printStackTrace();
            return redownload(downloadURL, filename, savePath, page, pageURL);
        }

    }

    private static boolean redownload(String downloadURL, String filename, String savePath, Page page, String pageURL) {
        logger.warn("downloader -err " + downloadURL);
        if (!downloadErrorCounter.containsKey(downloadURL)) {
            downloadErrorCounter.put(downloadURL, 1);
            logger.warn("downloader -retry: 1" + downloadURL);
            return download(downloadURL, filename, savePath, page, pageURL);
        } else {
            downloadErrorCounter.put(downloadURL, downloadErrorCounter.get(downloadURL) + 1);
            if (downloadErrorCounter.get(downloadURL) < 4) {
                // 首先出发重新下载支持3次
                logger.warn("downloader -retry：" + downloadErrorCounter.get(downloadURL) + "  " + downloadURL);
                return download(downloadURL, filename, savePath, page, pageURL);
            } else {
                return false;
                // 原本设计 如果连续下载 N次 出错，给予一次重新加入spider队列的机会
//                    //
//                    downloadErrorCounter.remove(downloadURL);
//                    if (!pageRedoCounter.containsKey(pageURL)) {
//                        pageRedoCounter.put(pageURL, 1);
//                        logger.warn("downloader -put back to Request list" + pageURL);
//                        page.addTargetRequest(pageURL);
//                        return true;
//                    } else {
//                        return false;
//                    }
            }
        }
    }
}
