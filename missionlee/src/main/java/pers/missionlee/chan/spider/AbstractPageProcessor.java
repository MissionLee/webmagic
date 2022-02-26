package pers.missionlee.chan.spider;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.downloader.FileDownloader;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-10 20:48
 */
public abstract class AbstractPageProcessor implements PageProcessor {
    public void sleep(int second) {
        try {
            Thread.sleep(second * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    Logger logger = LoggerFactory.getLogger(AbstractPageProcessor.class);
    public DiskService diskService;
    public DataBaseService dataBaseService;

    public AbstractPageProcessor(DataBaseService dataBaseService, DiskService diskService) {
        this.diskService = diskService;
        this.dataBaseService = dataBaseService;
    }

    public abstract void onDownloadSuccess(Page page, ArtworkInfo artworkInfo);

    public abstract void onDownloadFail(Page page, ArtworkInfo artworkInfo);

    public abstract void doProcess(Page page);

    @Override
    public void process(Page page) {
//        logger.info("AbstractPageProcessor#process 里面添加了Sleep，因为用文件验证存在性 是否需要下载之后，会出现访问不少show页面，但是实际上每个都不下载的情况");

        if (processCode429Page(page)) {
            doProcess(page);
        }
    }

    @Override
    public Site getSite() {
        return SpiderUtils.site;
    }

    public boolean processCode429Page(Page page) {
        // TODO: 2021/9/19 内置到了  Spider 里面 
//        if (page.getStatusCode() == 429 || page.getStatusCode() == 502) {
//            logger.info("触发429 / 502 返回机制");
//            page.addTargetRequest(page.getUrl().toString());
//            return false;
//        }
        return true;
    }

    public static String unicodeToString(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), "_");
        }
        return str;
    }

    public Map<String, Object> getJsonStringFromRestPage(Page page) {
//        String html = page.getHtml().$("body").all().get(0).replaceAll("\n", "").replaceAll(" ", "");
//        String data = SpiderUtils.extractTag(html);
//        System.out.println("处理前："+data.length());
//        int start =0;
//        while ((start = data.indexOf("\"source\""))>0){
//            int end = data.indexOf(",",start);
//            System.out.println(data.substring(start,end));
//            data = data.substring(0,start)+data.substring(end+1);
//        }
//        try {
//            objectMapper.readValue(data,Map.class);
//        } catch (IOException e) {
////            System.out.println(data);
//            System.out.println(data.length());
//            e.printStackTrace();
//        }
//        return (Map<String, Object>) JSON.parse(data);
        return (Map<String, Object>) JSON.parse(page.getJson().toString());
    }

    public boolean downloadAndSaveFileFromShowPage(AbstractPageProcessor.Target target, ArtworkInfo artworkInfo, Page page) {
        String med = artworkInfo.getFileName().substring(0, artworkInfo.getFileName().indexOf("."));
//        if(dataBaseService.sanCodeExist(artworkInfo.sanCode)){
//            logger.info("sanCode 存在-跳过");
//            return true;
//        }
        if (diskService.copyFileFromRelatedArtist(artworkInfo)) {
            //  因为 book 或者 parent之类的，为了保证完整性，不能因为
            //  别的地方有就不下载了，所以从不妙目录复制文件
            logger.info("YYYYYYYYYYYYYYYYY 从其他作者名下复制了这个文件");
            return true;
        }
        if (!diskService.artworkExistOnDisk(artworkInfo, artworkInfo.PBPrefix)) {
            logger.info("存在判断：文件不存在，可以开始下载");
            File tempFile = FileDownloader.download(target.targetUrl, page.getUrl().toString(), diskService.getTempPath());
            if (null != tempFile && tempFile.exists() && tempFile.isFile() && tempFile.length() > 10) {
                if (diskService.saveFile(tempFile, artworkInfo, artworkInfo.PBPrefix, artworkInfo.fileSaveName)) {
                    onDownloadSuccess(page, artworkInfo);
                    return true;
                } else {
                    onDownloadFail(page, artworkInfo);
                    return false;
                }
            } else {
                onDownloadFail(page, artworkInfo);
                return false;
            }
        } else {
            logger.info("存在判断：已经存在");
            return true;
        }
    }

    public boolean saveFileFromRelatedArtist(ArtworkInfo artworkInfo) {
        return diskService.copyFileFromRelatedArtist(artworkInfo);
    }

    /**
     * 用于分析详情页Html，获取真正要下载的文件信息（主要处理了 1.页面展示缩略图/原图的情况，2.页面为图片/视频/flash文件的情况）
     */
    // TODO: 4/17/2021  原页面逻辑变了
    public AbstractPageProcessor.Target extractDownloadTargetInfoFromDetailPage(Html html) {
        AbstractPageProcessor.Target target = new AbstractPageProcessor.Target();
        List<String> maybe = html.$("#image-link", "href").all();

        if (maybe != null && maybe.size() > 0) {  //如果页面内容是个图片，则存在 #image-link
            if (!maybe.get(0).equals("")) { // 有非缩略地址
                target.targetUrl = "https:" + maybe.get(0);
            }
//            else { // 自身就是原图
//                target.targetUrl = "https:" + html.$("#post-content").$("img", "src").all().get(0).replace("&amp;", "&");
//            }

        }
        if (null == target.targetUrl || StringUtils.isEmpty(target.targetUrl)) {
            if (html.$("#post-content").$("img", "src").all().size() > 0) {
                target.targetUrl = "https:" + html.$("#post-content").$("img", "src").all().get(0).replace("&amp;", "&");
            } else if (html.$("#post-content").$("video").all().size() > 0) { // 如果页面是个video 则存在直接找video
                target.targetUrl = "https:" + html.$("#post-content").$("video", "src").all().get(0).replace("&amp;", "&");

            } else { // swf 格式
                target.targetUrl = "https:" + html.$("#post-content").$("embed", "src").all().get(0).replace("&amp;", "&");
            }
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

    public class Target {
        String targetUrl;
        String targetName;
        String subFix;
    }

    public ArtworkInfo extractArtworkInfoFromDetailPage(Page page, AbstractPageProcessor.Target target) {
        logger.info("提醒：从show页面提取的ArtworkInfo parent_id book_id = -2 表示属于某个BP, -3表示 不属于某个BP");
        Html html = page.getHtml();
        // 提取标签信息
        Selectable tagSideBar = html.$("#tag-sidebar");
        //  标签-版权
        List<String> copyRightHtmlList = tagSideBar.$(".tag-type-copyright").$("a").all();
        List<String> copyRightList = new ArrayList<String>();
        SpiderUtils.extractTags(copyRightHtmlList, copyRightList);
        logger.debug("spider - process copyRightHtmlList:" + copyRightList);
        //  标签-工作室
        List<String> studioHtmlList = tagSideBar.$(".tag-type-studio").$("a").all();
        List<String> studioList = new ArrayList<String>();
        SpiderUtils.extractTags(studioHtmlList, studioList);
        logger.debug("spider - process studioHtmlList:" + studioList);
        //  标签-角色
        List<String> characterHtmlList = tagSideBar.$(".tag-type-character").$("a").all();
        List<String> characterList = new ArrayList<String>();
        SpiderUtils.extractTags(characterHtmlList, characterList);
        logger.debug("spider - process characterList:" + characterList);
        //  标签-作者
        List<String> artistHtmlList = tagSideBar.$(".tag-type-artist").$("a").all();
        List<String> artistList = new ArrayList<String>();
        SpiderUtils.extractTags(artistHtmlList, artistList);
        logger.debug("spider - process artistList:" + artistList);

        //  标签-媒体
        List<String> mediumHtmlList = tagSideBar.$(".tag-type-medium").$("a").all();
        List<String> mediumList = new ArrayList<String>();
        SpiderUtils.extractTags(mediumHtmlList, mediumList);
        logger.debug("spider - process mediumList:" + mediumList);

        //  标签-通用
        List<String> generalHtmlList = tagSideBar.$(".tag-type-general").$("a").all();
        List<String> generalList = new ArrayList<String>();
        SpiderUtils.extractTags(generalHtmlList, generalList);
        logger.debug("spider - process generalList:" + generalList);
        //  标签-风俗 体裁 样式
        List<String> genreHtmlList = tagSideBar.$(".tag-type-genre").$("a").all();
        List<String> genreList = new ArrayList<String>();
        SpiderUtils.extractTags(genreHtmlList, genreList);
        //  标签- meta
        List<String> metaHtmlList = tagSideBar.$(".tag-type-meta").$("a").all();
        List<String> metaList = new ArrayList<String>();
        SpiderUtils.extractTags(metaHtmlList, metaList);
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
                postDate = SpiderUtils.extractStats(str, SpiderUtils.htmlTitlePattern);
            } else if (str.contains("Original")) {
                fileSize = SpiderUtils.extractStats(str, SpiderUtils.htmlTitlePattern);
                fileSizeInfo = SpiderUtils.extractStats(str, SpiderUtils.resolutionPattern);
//                    original = extractStats(str, htmlHrefPattern);
            } else if (str.contains("Rating")) {
                String preRat = SpiderUtils.extractStats(str, SpiderUtils.htmlTextPattern);
                if (preRat != null && !"".equals(preRat) && preRat.contains("Rating"))
                    rating = preRat.replace("Rating: ", "");
            }
        }
        ArtworkInfo artworkInfo = new ArtworkInfo();
        artworkInfo.setAddress(page.getUrl().toString());
        artworkInfo.setFileName(target.targetName);
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
        artworkInfo.setTagStudio(studioList);
        artworkInfo.setTagGenre(genreList);
        artworkInfo.setTagMeta(metaList);
        artworkInfo.setTakeTime(System.currentTimeMillis());
        artworkInfo.sanCode = page.getUrl().toString().substring(page.getUrl().toString().lastIndexOf("/") + 1);
        String pageString = page.getHtml().toString();
        if (pageString.contains("This post has")
                || pageString.contains("child post")
                || pageString.contains("This post belongs to")
                || pageString.contains("a parent post")
        ) {
            artworkInfo.parentId = -2;
        } else {
            artworkInfo.parentId = -3;
        }
        if (pageString.contains("Books") && pageString.contains("legacy")) {
            artworkInfo.bookId = -2;
        } else {
            artworkInfo.bookId = -3;
        }
        return artworkInfo;
    }

    public static void main(String[] args) {
        String x = "1qwerq.sadfa";
        System.out.println(x.substring(0, x.indexOf(".")));
    }

}
