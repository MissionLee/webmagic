package pers.missionlee.bili.spider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.bili.starter.BiliArtistInfo;
import pers.missionlee.bili.starter.BiliSetting;
import pers.missionlee.chan.filedownloader.FileDownloader;
import pers.missionlee.chan.pagedownloader.MixDownloader;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BidPageProcessor implements PageProcessor {
    Logger logger = LoggerFactory.getLogger(BidPageProcessor.class);
    BiliArtistInfo artistInfo;
    List<String> oneList;
    BiliSetting biliSetting;
    public int newPageAdded;

    public BidPageProcessor(BiliArtistInfo info, BiliSetting setting) {
        this.artistInfo = info;
        this.biliSetting = setting;
        init();
    }

    /**
     * 首次创建和每次访问 article列表页面的收，调用本方法
     * 1. newPageAdded 重置为 0  次数据参与 UPDATE模式下，是否继续在 article页面下进行下滚操作
     * 2. 重置 one 目录下作品清单，如果重置，会影响作品是否已经下载的判断
     */
    public void init() {
        newPageAdded = 0;
        File root = new File(PathUtils.buildPath(biliSetting.ROOT, artistInfo.bid));
        if (root.exists()) {
            root.setLastModified(System.currentTimeMillis());
        }
        File one = new File(PathUtils.buildPath(biliSetting.ROOT, artistInfo.bid, "one"));
        if (one.exists()) {
            oneList = Arrays.asList(one.list());
        } else {
            oneList = new ArrayList<>();
        }

    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        // 1.
        if (url.contains("article")) {
            init();
            processArticle(page);
        } else if (url.contains("opus")) {
            if (exitsDisk(page)) {
                return;
            }
            String stringPage = page.getRawText();
            if (stringPage.contains("6元充电")) {
                artistInfo.member.add(getSer(page));
                return;
            }
            if (stringPage.contains("opus-para-pic center")) {
                logger.warn("检测到【纵向列表式】图片分享页面");
                processOpusCenter(page);
            } else if (stringPage.contains("horizontal-scroll-album")) {
                logger.warn("检测到【轮播页】图片分享页面");
                processOpusScroll(page);
            } else if (stringPage.contains("bili-album__preview grid")) {
                logger.warn("检测到【宫格式】图片分享页面");
                processOpusGrid(page);
            } else if (stringPage.contains("bili-album__preview one")) {
                logger.warn("检测到【单图式】图片分享页面");
                processOpusOne(page);
            } else if (stringPage.contains("article-content")) {
                logger.warn("检测到【文章】图片分享页面");
                processArticleRead(page);
            } else if (stringPage.contains("opus-module-top")) {
                //⭐ 注意  scroll 也是 opus-module-top  scroll优先，放在上面
                logger.warn("检测到【TOP图】图片分享页面");
                processOpusTop(page);
            } else {
                artistInfo.unknown.add(getSer(page));
                logger.warn("当前页面未检测到特征");
            }


        } else {

        }


    }

    public String getSer(Page page) {
        String url = page.getUrl().toString();
        String ser = url.substring(url.lastIndexOf("/") + 1);
        logger.info("Ser:" + ser);
        return ser;
    }

    public boolean exitsDisk(Page page) {
        String ser = getSer(page);
        return exitsDisk(ser);
    }

    public boolean exitsDisk(String ser) {
        String path = PathUtils.buildPath(biliSetting.ROOT, artistInfo.bid, ser);
        File opusFile = new File(path);
        if (opusFile.exists() && opusFile.listFiles().length > 1) {
            logger.info("当前页面已经下载过，跳过");
            return true;
        } else {
            for (int i = 0; i < oneList.size(); i++) {
                if (oneList.get(i).contains(ser)) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getArtworkNum() {
        String root = PathUtils.buildPath(biliSetting.ROOT, artistInfo.bid);
        File rootFile = new File(root);
        if (rootFile.exists()) {
            return getFileNum(new File(root));
        } else {
            return 0;
        }

    }

    public int getFileNum(File file) {
        File[] sub = file.listFiles();
        int num = 0;
        for (int i = 0; i < sub.length; i++) {
            if (sub[i].isDirectory()) {
                num += getFileNum(sub[i]);
            } else {
                num++;
            }
        }
        return num;
    }

    public void downloadWithUnCleanedUrl(List<String> urls, Page page) {
        String ser = getSer(page);
        for (int i = 0; i < urls.size(); i++) {
            String fullUrl = urls.get(i);
            logger.info("原始SRC: " + fullUrl);
            if (StringUtils.isEmpty(fullUrl)) {
                continue;
            }
            String origUrl = "https:" + fullUrl;
            if (origUrl.contains("@"))
                origUrl = "https:" + fullUrl.substring(0, fullUrl.indexOf("@"));
            else {
                continue;
            }


            String fileName = origUrl.substring(origUrl.lastIndexOf("/") + 1);
            logger.info("转换URL: " + origUrl);
            logger.info("文件名:  " + fileName);
            logger.info("开始下载");
            File aimFile = new File(PathUtils.buildPath(biliSetting.ROOT,artistInfo.bid,ser,fileName) );
            if (aimFile.exists()) {
                logger.warn("检测到文件存在，continue 跳过本次循环");
                continue;
            }
            File tempFile = FileDownloader.download(origUrl, page.getUrl().toString(), PathUtils.buildPath(biliSetting.ROOT,"tmp"), getSite());
            if (null != tempFile && tempFile.exists() && tempFile.isFile() && tempFile.length() > 10) {
                try {

                    FileUtils.moveFile(tempFile, aimFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                onDownloadFail(page);

            }
        }
    }

    public void downloadOneWithUnCleanedUrl(List<String> urls, Page page) {
        String ser = getSer(page);
        for (int i = 0; i < urls.size(); i++) {
            String fullUrl = urls.get(i);
            if (StringUtils.isEmpty(fullUrl)) {
                continue;
            }
            String origUrl = "https:" + fullUrl;
            if (origUrl.contains("@"))
                origUrl = "https:" + fullUrl.substring(0, fullUrl.indexOf("@"));
            else {
                continue;
            }
//            String origUrl = "https:" + fullUrl.substring(0, fullUrl.indexOf("@"));
            String fileName = origUrl.substring(origUrl.lastIndexOf("/") + 1);
            logger.info("原始SRC: " + fullUrl);
            logger.info("转换URL: " + origUrl);
            logger.info("文件名:  " + fileName);
            logger.info("开始下载");
            File tempFile = FileDownloader.download(origUrl, page.getUrl().toString(), "G:\\C-B-ALL\\tmp\\", getSite());
            if (null != tempFile && tempFile.exists() && tempFile.isFile() && tempFile.length() > 10) {
                try {

                    FileUtils.moveFile(tempFile, new File("G:\\C-B-ALL\\" + artistInfo.bid + "\\one\\" + ser + "_" + fileName));
                } catch (IOException e) {

                    throw new RuntimeException(e);
                }
            } else {
                onDownloadFail(page);

            }
        }
    }

    public List<String> cleanUrls(List<String> origUrl) {
        List<String> cleam = new ArrayList<>();
        for (int i = 0; i < origUrl.size(); i++) {
            if (StringUtils.isEmpty(origUrl.get(i))) {

            } else {
                cleam.add(origUrl.get(i));
            }
        }
        return cleam;
    }

    public void processOpusCenter(Page page) {
        List<String> urls = page.getHtml()
                .$(".bili-opus-view .opus-para-pic")
                .$("img", "src")
                .all();
        download(urls, page);

    }

    public void processOpusScroll(Page page) {
        List<String> urls = page.getHtml()
                .$(".opus-module-top")
                .$(".horizontal-scroll-album__indicator")
                .$("img", "src")
                .all();
        if (urls == null || urls.size() == 0) {
            urls = page.getHtml()
                    .$(".horizontal-scroll-album__pic")
                    .$("img", "src")
                    .all();
        }
        download(urls, page);


    }

    public void download(List<String> urls, Page page) {
        List<String> clean = cleanUrls(urls);
        if (clean.size() > 2) {
            downloadWithUnCleanedUrl(clean, page);
        } else if (clean.size() > 0) {
            downloadOneWithUnCleanedUrl(clean, page);
        }else {
            artistInfo.empty.add(getSer(page));
        }
    }

    public void processOpusTop(Page page) {
        List<String> urls = page.getHtml()
                .$(".opus-module-top")
                .$("img", "src")
                .all();
        download(urls, page);
    }

    public void processArticleRead(Page page) {
//        List<String> urlsBanner = page.getHtml()
//                .$(".banner-container")
//                .$()
        List<String> urlsContent = page.getHtml()
                .$(".article-content .img-box")
                .$("img", "data-src")
                .all();

        download(urlsContent, page);
    }

    public void processOpusOne(Page page) {
        List<String> urls = page.getHtml()
                .$(".bili-opus-view .opus-para-pic")
                .$("img", "src")
                .all();
        download(urls, page);
    }

    public void processArticle(Page page) {
//        System.out.println(page.getHtml());
        List<String> urls = page.getHtml()
                .$(".waterfall-content .container")
                .$(".item .article-card  a", "href")
                .all();
        List<String> src = page.getHtml()
                .$(".waterfall-content .container")
                .$(".item .article-card  img", "src")
                .all();
        for (int i = 0; i < urls.size(); i++) {
            // src="//i0.hdslb.com/bfs/activity-plat/static/20231026/3b3c5705bda98d50983f6f47df360fef/gjM5HuoMus.png@320w_240h_1c.webp"
            String newurl = "https://" + urls.get(i).substring(2);
            String ser = newurl.substring(newurl.lastIndexOf("/") + 1);
            if (artistInfo.skip(ser)) {
                logger.info("需跳过: " + ser + " 无图页面/无法解析页面/会员页面(部分会员页面会被识别成无图)");
                continue;
            }
            if (urls.size() == src.size())
                if (src.get(i).contains("gjM5HuoMus.png")) {
                    logger.info("检测到要充钱的，跳过");
                    artistInfo.member.add(ser);
                    continue;
                }
            if (exitsDisk(ser)) {
                logger.info("已存在: " + ser);
            } else {
                newPageAdded++;
                logger.info("新页面: " + ser);
                page.addTargetRequest(newurl);
            }

        }
    }

    public void processOpusGrid(Page page) {
        List<String> urls = page.getHtml()
                .$(".bili-opus-view .opus-para-pic")
                .$(".bili-album__preview__picture")
                .$("img", "src")
                .all();
        download(urls, page);
    }

    public void onDownloadSuccess(Page page) {

    }

    public void onDownloadFail(Page page) {

    }


    @Override
    public Site getSite() {
        return SpiderUtils.site_bili;
    }

    public static void main(String[] args) throws IOException {


    }
}
