package pers.missionlee.webmagic.spider.sankaku.pageprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.SankakuFileUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.task.SankakuSpiderTask;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;
import pers.missionlee.webmagic.spider.sankaku.manager.SpiderTask;
import pers.missionlee.webmagic.utils.TimeLimitedHttpDownloader;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description: 对于每一次下载工作（下载某个tag对应的内容），都要创建一个此对象
 * @author: Mission Lee
 * @create: 2019-03-02 16:20
 */
public class SankakuDownloadSpider extends AbstractSankakuSpider {
    Logger logger = LoggerFactory.getLogger(SankakuDownloadSpider.class);
    @Deprecated
    SankakuSpiderTask task;

    // root 路径
    //public String rootPath;
    /**
     * 已经下载成功的作品信息（初始状态为本地存储的部分，下载过程中动态添加）
     */
    @Deprecated
    public List<ArtworkInfo> artworkInfos;
    // 作者名/tag  同时也是当前作者/tag本地文件夹名称
    //public String artistName;
    // 扫描列表页因为存在 不同的排序情况同时使用的可能，借助这个addedList 排除artworkInfos里面没有，但是已经被添加到待下载列表中的文件
    @Deprecated // addedList 功能交给 SpiderTask
    public List<String> addedList;
    // == 以下用于下载出错后重新下载的机制
    // 重下载机制交给 下载器 TimeLimitedHttpDownloader代理
    @Deprecated
    private Map<String, Integer> pageRedoCounter = new HashMap<String, Integer>();
    @Deprecated
    private Map<String, Integer> downloadErrorCounter = new HashMap<String, Integer>();

    public SankakuDownloadSpider(Site site, SpiderTask task) {
        super(site, task);
        // addedList 已经由 task代为管理了
        // this.addedList = new ArrayList<String>();
    }

    @Deprecated
    public SankakuDownloadSpider(Site site, SankakuSpiderTask task) throws IOException {
        this(site, task.rootPath, task.currentDownloadTask.artistName, task);
    }

    @Deprecated
    public SankakuDownloadSpider(Site site, String rootPath, String artistName, SankakuSpiderTask task) throws IOException {
        super(site, rootPath, artistName);
        this.task = task;
        //this.rootPath = rootPath;
        //this.artistName = artistName;
        this.artworkInfos = SankakuFileUtils.getArtworkInfoList(rootPath, artistName);
        this.addedList = new ArrayList<String>();
        //this.artistFile =new File(rootPath+artistName);
        // 可能出现因为网络原因，或者目标nginx阻拦，导致某个作者没有被下载任何作品，但是执行流程
        // 结束，作者从 name.md文件夹里面出名，此时如果文件夹已经建立，那么在update的时候，还能补救这种情况
    }

    @Deprecated
    private boolean hasDownloaded(String URL) {
        for (ArtworkInfo info :
                artworkInfos) {
            if (info.getAddress().equals(URL))
                return true;
        }
        return false;
    }


    private boolean hasDownloaded2(String url) {
        for (ArtworkInfo info : spiderTask.artworkInfoList)
            if (info.getAddress().equals(url))
                return true;
        return false;
    }

    @Override
    public void process(Page page) {
        String URL = page.getUrl().toString();
        if (URL.contains("tags")) { // 如果访问的时列表页面
            logger.debug("从列表页中提取待爬取详情页");
            processListPage(page, URL);
        } else if (page.getUrl().toString().startsWith("https://chan.sankakucomplex.com/post/show/")) {
            //processDetailPage(page);
            logger.debug("从详情页中提取/下载目标数据");
            processDetailPage2(page);
        } else {
            logger.warn("位置的页面位置: " + page.getUrl());
        }
        //logger.info("下载信息: [作者:"+artistName+" 下载情况" + d_suc+ "/" +d_err + "/" + d_added  + "]-原始收录: " + d_skip);
        //logger.info(task.getCurrentTaskProgress());
        logger.info(spiderTask.getTaskProgress());
    }


    private void processListPage(Page page, String URL) {
        try {
            Thread.sleep(new Random().nextInt(40000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int thisPageAdded = 0;

        thisPageAdded = extractDetainPageUrlFromListPageAndAddToTargetRequest(page, thisPageAdded);
        // TODO: 2019/3/17 用于内置的 update模式
        /**
         * 更新模式下，如果一个页面查询到的待更新内容超过1个，就添加下一个页面
         * 更新模式通过  DATE 关键字自动检测
         * @update 20190327 实际使用中发现大量的本页面更新一两个就够了，找下一页纯属浪费，
         *          并且在外层processor里面有udpate数量不过尝试遍历作者的方法 所以added改为 > 15
         * */
        if (thisPageAdded > 10 && URL.contains("date")) {
            String thisPage = URL.substring(URL.length() - 1);
            int thisPageNum = Integer.valueOf(thisPage);
            if (thisPageNum < 50) {
                String urlPrefix = URL.substring(0, URL.length() - 1);
                page.addTargetRequest(urlPrefix + (++thisPageNum));
                logger.info("⭐ add： " + urlPrefix + thisPageNum);
            }
        }
    }

    /**
     * 分析列表页面，将需要下载的详情页面加入 TargetRequest
     */
    private int extractDetainPageUrlFromListPageAndAddToTargetRequest(Page page, int thisPageAdded) {
        List<String> urlList = page.getHtml().$(".thumb").$("a", "href").all();
        /**
         * 通过 存档记录 判断是否需要添加页面中的子页面
         * */
        if (urlList != null && urlList.size() > 0) {
            for (String url : urlList
            ) {

                if (!hasDownloaded2(BASE_URL + url) && !spiderTask.targetUrl.contains(url)) {
                    logger.info("⭐ add " + BASE_URL + url);
                    page.addTargetRequest(BASE_URL + url);
                    spiderTask.targetUrl.add(url);
                    //addedList.add(url);
                    thisPageAdded++;
                    //d_added++;
                    spiderTask.added++;
                    //task.currentDownloadTask.added++;
                } else {
//                    d_skip++;
                    logger.info("⭐ skip " + BASE_URL + url);

                }
            }

        }
        return thisPageAdded;
    }

    private void processDetailPage2(Page page) {
        Html html = page.getHtml();
        Target target = extractDownloadTargetInfoFromDetailPage(html);
        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        if (download(target.targetUrl, target.targetName, page)) {
            try {
                spiderTask.appendArtworkInfo(artworkInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            spiderTask.downloaded++;
        } else {
            spiderTask.failed++;
        }
    }

    /**
     * 1. 解析作品信息
     * 2. 解析下载目标信息 URL 文件名
     * 3. 下载文件，下载成功后将作品信息写入文档记录
     */
    @Deprecated
    private void processDetailPage(Page page) {
        // 详情页面
        Html html = page.getHtml();
        Target target = extractDownloadTargetInfoFromDetailPage(html);
        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        if (download(target.targetUrl, target.targetName, SankakuFileUtils.buildPath(task.rootPath, task.currentDownloadTask.artistName, target.subFix), page)) {
            artworkInfos.add(artworkInfo);
            try {
                //infoUtils.appendInfo(artworkInfo);
                SankakuFileUtils.appendArtworkInfo(artworkInfo, task.rootPath, task.currentDownloadTask.artistName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //d_suc++;
            task.currentDownloadTask.downloaded++;
        } else {
            //d_err++;
            task.currentDownloadTask.failed++;
        }
    }

    private ArtworkInfo extractArtworkInfoFromDetailPage(Page page, Target target) {
        Html html = page.getHtml();
        // 提取标签信息
        Selectable tagSideBar = html.$("#tag-sidebar");
        //  标签-版权
        List<String> copyRightHtmlList = tagSideBar.$(".tag-type-copyright").$("a").all();
        List<String> copyRightList = new ArrayList<String>();
        extractTags(copyRightHtmlList, copyRightList);
        logger.debug("spider - process copyRightHtmlList:" + copyRightList);
        //  标签-工作室
        List<String> studioHtmlList = tagSideBar.$(".tag-type-studio").$("a").all();
        List<String> studioList = new ArrayList<String>();
        extractTags(studioHtmlList, studioList);
        logger.debug("spider - process studioHtmlList:" + studioList);
        //  标签-角色
        List<String> characterHtmlList = tagSideBar.$(".tag-type-character").$("a").all();
        List<String> characterList = new ArrayList<String>();
        extractTags(characterHtmlList, characterList);
        logger.debug("spider - process characterList:" + characterList);
        //  标签-作者
        List<String> artistHtmlList = tagSideBar.$(".tag-type-artist").$("a").all();
        List<String> artistList = new ArrayList<String>();
        extractTags(artistHtmlList, artistList);
        logger.debug("spider - process artistList:" + artistList);

        //  标签-媒体
        List<String> mediumHtmlList = tagSideBar.$(".tag-type-medium").$("a").all();
        List<String> mediumList = new ArrayList<String>();
        extractTags(mediumHtmlList, mediumList);
        logger.debug("spider - process mediumList:" + mediumList);

        //  标签-通用
        List<String> generalHtmlList = tagSideBar.$(".tag-type-general").$("a").all();
        List<String> generalList = new ArrayList<String>();
        extractTags(generalHtmlList, generalList);
        logger.debug("spider - process generalList:" + generalList);
        //  标签-风俗 体裁 样式
        List<String> genreHtmlList = tagSideBar.$(".tag-type-genre").$("a").all();
        List<String> genreList = new ArrayList<String>();
        extractTags(genreHtmlList, genreList);
        //  标签- meta
        List<String> metaHtmlList = tagSideBar.$(".tag-type-meta").$("a").all();
        List<String> metaList = new ArrayList<String>();
        extractTags(metaHtmlList, metaList);
        // 提取文件信息
        Selectable stats = html.$("#stats");
        List<String> statsLiList = stats.$("ul li").all();
        String postDate = "";
        String fileSize = "0 bytes";
        String fileSizeInfo = "";
        String rating = "";
//            String original = "notFound";
        for (String str : statsLiList) {
            if (str.contains("Posted")) {
                postDate = extractStats(str, htmlTitlePattern);
            } else if (str.contains("Original")) {
                fileSize = extractStats(str, htmlTitlePattern);
                fileSizeInfo = extractStats(str, resolutionPattern);
//                    original = extractStats(str, htmlHrefPattern);
            } else if (str.contains("Rating")) {
                String preRat = extractStats(str, htmlTextPattern);
                if (preRat != null && !"".equals(preRat) && preRat.contains("Rating"))
                    rating = preRat.replace("Rating: ", "");
            }
        }
        ArtworkInfo artworkInfo = new ArtworkInfo();
        artworkInfo.setAddress(page.getUrl().toString());
        artworkInfo.setName(target.targetName);
        artworkInfo.setFileSize(fileSize);
        artworkInfo.setResolutionRatio(fileSizeInfo);
        artworkInfo.setRating(rating);
        artworkInfo.setFormat(target.targetName.split("\\.")[1]);
        artworkInfo.setPostDate(postDate);
        artworkInfo.setTagArtist(artistList);
        artworkInfo.setTagCharacter(characterList);
        artworkInfo.setTagCopyright(copyRightList);
        artworkInfo.setTagGeneral(generalList);
        artworkInfo.setTagMedium(mediumList);
        artworkInfo.setTagArtist(artistList);
        artworkInfo.setTagStudio(studioList);
        artworkInfo.setTagGenre(genreList);
        artworkInfo.setTagMeta(metaList);
        artworkInfo.setTakeTime(System.currentTimeMillis());
        return artworkInfo;
    }

    private class Target {
        String targetUrl; //目标URL
        String targetName;//目标文件名称
        String subFix;    //目标文件格式
    }

    /**
     * 用于分析详情页Html，获取真正要下载的文件信息（主要处理了 1.页面展示缩略图/原图的情况，2.页面为图片/视频/flash文件的情况）
     */
    private Target extractDownloadTargetInfoFromDetailPage(Html html) {
        Target target = new Target();
        List<String> maybe = html.$("#image-link", "href").all();
        if (maybe != null && maybe.size() > 0) {  //如果页面内容是个图片，则存在 #image-link
            if (!maybe.get(0).equals("")) { // 有非缩略地址
                target.targetUrl = "https:" + maybe.get(0);
            } else { // 自身就是原图
                target.targetUrl = "https:" + html.$("#post-content").$("img", "src").all().get(0).replace("&amp;", "&");
            }

        } else if (html.$("#post-content").$("video").all().size() > 0) { // 如果页面是个video 则存在直接找video
            target.targetUrl = "https:" + html.$("#post-content").$("video", "src").all().get(0).replace("&amp;", "&");

        } else { // swf 格式
            target.targetUrl = "https:" + html.$("#post-content").$("embed", "src").all().get(0).replace("&amp;", "&");
        }

        String[] split = target.targetUrl.split("/");
        target.targetName = split[split.length - 1].split("\\?")[0];
        String nameLow = target.targetName.toLowerCase();
        target.subFix = "/pic";
        if (SourceManager.isVideo(nameLow)) {
            target.subFix = "/vid";
        }
        return target;
    }

    public boolean download(String downloadUrl, String filename, Page page) {
        boolean returnStatus = false;
        if (!spiderTask.exists(filename)) {
            logger.info("开始下载: " + filename + " " + page.getUrl());
            try {
                returnStatus = TimeLimitedHttpDownloader.downloadWithAutoRetry(downloadUrl, filename, page.getUrl().toString(), spiderTask);
            } catch (IOException e) {
                logger.error("下载失败[开始重试]:[下载过程正常]文件保存/重命名/流操作失败" + e.getMessage());
                try {
                    returnStatus = TimeLimitedHttpDownloader.downloadWithAutoRetry(downloadUrl, filename, page.getUrl().toString(), spiderTask);
                } catch (IOException e1) {
                    logger.error("下载失败[放弃下载]:[下载过程正常]文件保存/重命名/流操作失败" + e1.getMessage());
                }
            }
        } else {
            logger.warn("跳过下载:"+filename+"已存在");
            returnStatus = true;
        }
        return returnStatus;
    }

    @Deprecated
    public boolean download(String downloadURL, String filename, String savePath, Page page) {
        boolean returnStatus = false;
        if (!new File(savePath + filename).exists()) {
            logger.info("开始下载: " + filename + " " + page.getUrl());
            try {
                returnStatus = TimeLimitedHttpDownloader.downloadWithAutoRetry(downloadURL, filename, savePath, page.getUrl().toString(), 3);
            } catch (IOException e) {
                // 此处报错可能原因是 downloadWithAutoRetry 执行技术，在finally部分 in.close out.close发生的错误
                logger.error("下载失败[开始重试]:[下载过程正常]文件保存/重命名/流操作失败" + e.getMessage());
                try {
                    returnStatus = TimeLimitedHttpDownloader.downloadWithAutoRetry(downloadURL, filename, savePath, page.getUrl().toString(), 3);
                } catch (IOException e1) {
                    logger.error("下载失败[放弃下载]:[下载过程正常]文件保存/重命名/流操作失败");
                }
            }
        } else {
            logger.info("已经存在: " + filename);
            returnStatus = true;
        }
        return returnStatus;
    }
}
