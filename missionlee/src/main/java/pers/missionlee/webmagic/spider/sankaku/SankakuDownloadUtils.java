package pers.missionlee.webmagic.spider.sankaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.utils.TimeLimitedHttpDownloader;
import us.codecraft.webmagic.Page;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:24
 */
public class SankakuDownloadUtils {
    private static Logger logger = LoggerFactory.getLogger(SankakuInfoUtils.class);
    private static List<String> fatalErrorPageURL = new ArrayList<String>();
    private static Map<String, Integer> pageRedoCounter = new HashMap<String, Integer>();
    private static Map<String, Integer> downloadErrorCounter = new HashMap<String, Integer>();

    public static boolean download(String downloadURL, String filename, String savePath, Page page, String pageURL) {
        int downloadStatus = 3;
        try {
            logger.info("[PAGE-RETRY:" + (pageRedoCounter.containsKey(pageURL) ? pageRedoCounter.get(pageURL) + 1 : 1)
                    + " / DO-RETRY:" + (downloadErrorCounter.containsKey(downloadURL) ? downloadErrorCounter.get(downloadURL) + 1 : 1)
                    + "] " + downloadURL);
            File aimFile = new File(savePath + "/" + filename);
            if (!new File(savePath + "/" + filename).exists()) {
                logger.info("downloader - start download");
                downloadStatus = TimeLimitedHttpDownloader.download(downloadURL, filename, savePath, pageURL);
                System.out.println("downloadStatus:" + downloadStatus);
//                if(downloadStatus ==1){
//                    throw new Exception("retry");
//                    //doRetry(downloadURL,filename,savePath,page,pageURL,new Exception("time out"));
//                }else
                if (downloadStatus == 0) {
                    // 下载成功 remove 机制
                    logger.info("downloader -suc " + downloadURL);
                    downloadErrorCounter.remove(downloadURL);
                    pageRedoCounter.remove(pageURL);
                    return true;
                }
                return false;
            } else {
                logger.info("downloader - skip download");
                return true;
            }
        } catch (Exception e) { // 如果下载失败
            logger.warn("downloader -err" + downloadURL);
//            doRetry(downloadURL, filename, savePath, page, pageURL, e);
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
                } else { // 重新下载已经超过1次仍然报错
                    downloadErrorCounter.remove(downloadURL);
                    if (!pageRedoCounter.containsKey(pageURL)) {
                        pageRedoCounter.put(pageURL, 1);
                        logger.warn("downloader -put back to Request list" + pageURL);
                        page.addTargetRequest(pageURL);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }

    }

//    private static void doRetry(String downloadURL, String filename, String savePath, Page page, String pageURL, Exception e) {
//        if (!downloadErrorCounter.containsKey(downloadURL)) {
//            downloadErrorCounter.put(downloadURL, 1);
//            logger.warn("downloader -retry: 1" + downloadURL);
//            download(downloadURL, filename, savePath, page, pageURL);
//        } else {
//            downloadErrorCounter.put(downloadURL, downloadErrorCounter.get(downloadURL) + 1);
//            if (downloadErrorCounter.get(downloadURL) < 4) {
//                // 首先出发重新下载支持3次
//                logger.warn("downloader -retry："+ downloadErrorCounter.get(downloadURL)+"  " + downloadURL);
//                download(downloadURL, filename, savePath, page, pageURL);
//            } else { // 重新下载已经超过3次仍然报错
//                downloadErrorCounter.remove(downloadURL);
//                if (!pageRedoCounter.containsKey(pageURL)) {
//                    pageRedoCounter.put(pageURL, 1);
//                    logger.warn("downloader -put back to Request list" + pageURL);
//                    page.addTargetRequest(pageURL);
//                } else {
//                    pageRedoCounter.put(pageURL, pageRedoCounter.get(pageURL) + 1);
//                    if (pageRedoCounter.get(pageURL) < 4) {
//                        page.addTargetRequest(pageURL);
//                    } else {
//                        logger.error("downloader -drop -" + pageURL + " - " + downloadURL + "\n" + e);
//                        fatalErrorPageURL.add(pageURL);
//                        pageRedoCounter.remove(pageURL);
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//}
}
