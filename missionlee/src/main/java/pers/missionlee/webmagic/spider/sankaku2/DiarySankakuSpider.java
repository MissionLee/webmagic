package pers.missionlee.webmagic.spider.sankaku2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.SankakuFileDownloadProcessor;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:20
 */
public class DiarySankakuSpider implements PageProcessor {
    Logger logger = LoggerFactory.getLogger(DiarySankakuSpider.class);
    Site site;
    SankakuInfoUtils infoUtils;
    List<ArtworkInfo> artworkInfos;
    static Pattern htmlTextPattern = Pattern.compile(">(.+?)<");
    static Pattern htmlTitlePattern = Pattern.compile("title=\"(.+?)\"");
    static Pattern resolutionPattern = Pattern.compile("bytes\">(.+?)<");
    private SankakuSpiderProcessor processor ;
    public DiarySankakuSpider(Site site, SankakuInfoUtils infoUtils, SankakuSpiderProcessor processor) {
        this.site = site;
        this.infoUtils = infoUtils;
        this.processor = processor;
        try {
            this.artworkInfos = infoUtils.getArtworkInfoMap();
        } catch (IOException e) {
            this.artworkInfos = new ArrayList<ArtworkInfo>();
            e.printStackTrace();
        }
    }

    private boolean hasDownloaded(String URL) {
        for (ArtworkInfo info :
                artworkInfos) {
            if (info.getAddress().equals(URL))
                return true;
        }
        return false;
    }

    private String extractStats(String sourceStr, Pattern pattern) {
        String aim = "";
        Matcher textMatcher = pattern.matcher(sourceStr);
        if (textMatcher.find())
            aim = textMatcher.group(1);
        return aim;
    }

    private void extractTags(List<String> copyRightHtmlList, List<String> copyRight) {
        int flag = 0;
        for (String str : copyRightHtmlList
        ) {
            if (flag % 2 == 0) {

                Matcher matcher = htmlTextPattern.matcher(str);
                if (matcher.find())
                    copyRight.add(matcher.group(1));
            }
            flag++;

        }
    }

    @Override
    public void process(Page page) {
        String URL = page.getUrl().toString();
        if (URL.contains("tags")) { // 如果访问的时列表页面
            try {
                Thread.sleep(10000L + new Random().nextInt(10000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<String> urlList = page.getHtml().$(".thumb").$("a", "href").all();
            /**
             *
             * */
            if (urlList != null && urlList.size() > 0) {
                for (String url : urlList
                ) {
                    if (!hasDownloaded("https://chan.sankakucomplex.com" + url)) {
                        logger.info("spider - process ⭐ add TargetRequest " + "https://chan.sankakucomplex.com" + url);
                        page.addTargetRequest("https://chan.sankakucomplex.com" + url);
                    } else {
                        processor.d_skip++;
                        logger.info("spider - process ⭐ already Downloaded " + "https://chan.sankakucomplex.com" + url);
                    }
                }

            }
        } else if (page.getUrl().toString().contains("https://chan.sankakucomplex.com/post/show/")) {
            // 防止被抓,设置一个比较长的睡觉时间 10~30秒
            try {
                Thread.sleep(15000L + new Random().nextInt(10000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 详情页面
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
            String target;
            List<String> maybe = html.$("#image-link", "href").all();
            if (maybe != null && maybe.size() > 0) {  //如果页面内容是个图片，则存在 #image-link
                if (!maybe.get(0).equals("")) { // 有非缩略地址
                    target = "https:" + maybe.get(0);
                } else { // 自身就是原图
                    target = "https:" + html.$("#post-content").$("img", "src").all().get(0).replace("&amp;", "&");
                }

            } else { // 如果页面是个video 则存在直接找video
                target = "https:" + html.$("#post-content").$("video", "src").all().get(0).replace("&amp;", "&");

            }

            String[] split = target.split("/");
            String name = split[split.length - 1].split("\\?")[0];
            String subFix = "/pic";
            String nameLow = name.toLowerCase();
            if (nameLow.endsWith(".mp4")
                    || nameLow.endsWith(".webm")
                    || nameLow.endsWith(".avi")
                    || nameLow.endsWith(".rmvb")
                    || nameLow.endsWith(".flv")
                    || nameLow.endsWith(".3gp")
                    || nameLow.endsWith(".mov")
                    || nameLow.endsWith(".swf")) {
                subFix = "/vid";
            }
            logger.info("spider - FILE END DOWNLOAD:" + target);
            ArtworkInfo artworkInfo = new ArtworkInfo();
            artworkInfo.setAddress(page.getUrl().toString());
            artworkInfo.setName(name);
            artworkInfo.setFileSize(fileSize);
            artworkInfo.setResolutionRatio(fileSizeInfo);
            artworkInfo.setRating(rating);
            artworkInfo.setFormat(name.split("\\.")[1]);
            artworkInfo.setPostDate(postDate);
            artworkInfo.setTagArtist(artistList);
            artworkInfo.setTagCharacter(characterList);
            artworkInfo.setTagCopyright(copyRightList);
            artworkInfo.setTagGeneral(generalList);
            artworkInfo.setTagMedium(mediumList);
            artworkInfo.setTagArtist(artistList);
            artworkInfo.setTagStudio(studioList);
            artworkInfo.setTakeTime(System.currentTimeMillis());
            // TODO: 2019/3/2 1.下载 2.内存记录 3.日志记录
            logger.info("spider - FILE START DOWNLOAD:" + target);
            if(SankakuDownloadUtils.download(target,name,processor.ROOT_PATH+processor.TAG+subFix,page,page.getUrl().toString())){
                // 下载成功
                artworkInfos.add(artworkInfo);
                try {
                    infoUtils.appendInfo(artworkInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                processor.d_suc++;
            }else{
                processor.d_err++;
            }

        } else{
            logger.warn("Went to page: "+page.getUrl());
        }
        logger.info("suc: "+processor.d_suc+" /err: "+processor.d_err+" /skip: "+processor.d_skip);
    }

    @Override
    public Site getSite() {
        return site;
    }
}
