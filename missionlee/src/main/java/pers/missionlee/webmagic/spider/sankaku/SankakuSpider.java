package pers.missionlee.webmagic.spider.sankaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 11:04
 */
public class SankakuSpider implements PageProcessor {
    private static Logger logger = LoggerFactory.getLogger(SankakuSpider.class);

    private static Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(100000)
            .addCookie("__atuvc", "1%7C9")
            .addCookie("__atuvs", "5c791f63ddd2a1f5000")
            .addCookie("_pk_id.2.42fa", "adde0e4a1e63d583.1551189849.23.1551441764.1551440466..1551189849.16.1551362603.1551361875.")
            .addCookie("_pk_ses.2.42fa", "1")
            .addCookie("_sankakucomplex_session", "BAh7BzoMdXNlcl9pZGkD5lgGOg9zZXNzaW9uX2lkIiU5NWZhNGMyZjk2Y2M5MGJkZTNmOTZiMGM5ZmNmYzY3OQ%3D%3D--9d80a0ba02f9c4e31c13c7db0a08eb2cd035b80f%3D%3D--2d44e3f79213fc98bd4cb3b167394ecf18ded724")
            .addCookie("auto_page", "0")
            .addCookie("blacklisted_tags","")
            .addCookie("loc","MDAwMDBBU0NOSlMyMTQ0Mjk4NDA3NjAwMDBDSA==")
            .addCookie("locale", "en")
            .addCookie("login", "zuixue3000")
            .addCookie("mode", "view")
            .addCookie("na_id","2018122723475293368621024808")
            .addCookie("na_tc","Y")
            .addCookie("ouid","5c2564a80001da35a1ed736217e8a4379998383b2fa5f1877d3a")
            .addCookie("pass_hash", "b1f471dcd8cc8df0ed2b84f033ba2baae5de013b")
            .addCookie("uid", "5c2564a827f935b5")
            .addCookie("uvc","9%7C5%2C0%7C6%2C3%7C7%2C13%7C8%2C46%7C9")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Accept-Encoding", "gzip, deflate, br")
            .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Host", "chan.sankakucomplex.com")
            .addHeader("Pragma", "no-cach")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");

    static Pattern htmlTextPattern = Pattern.compile(">(.+?)<");
    static Pattern htmlTitlePattern = Pattern.compile("title=\"(.+?)\"");
    static Pattern htmlHrefPattern = Pattern.compile("href=\"(.+?)\"");
    static Pattern resolutionPattern = Pattern.compile("bytes\">(.+?)<");


    private boolean hasDownloaded(String URL) {
        for (ArtworkInfo info : SankakuSpiderProcessor.artworkInfoList) {
            if (info.getAddress().equals(URL))
                return true;
        }
        return false;
    }

    @Override
    public void process(Page page) {
        String URL = page.getUrl().toString();
        logger.info("spider - process : start" + URL);
        if (URL.contains("tags")) { // 如果访问的时列表页面
            try {
                Thread.sleep(10000L+new Random().nextInt(10000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            List<String> urlList = page.getHtml().$(".thumb").$("a", "href").all();
            if (urlList != null && urlList.size() > 0) {
                for (String url : urlList
                ) {
                    // TODO: 2019/2/28  当前list里面有这个链接，则不会把这个链接加入待处理列表
                    //                  实际上可以判断文件夹里面有没有这个文件，但是我希望有文件
                    //                  同时必须有json描述，保证两部分的一致性
                    //                  所以在读取json的时候，也是判断如果 json里面有文件信息，但是没有文件，就删除json信息
                    //                  但是又文件，没有json的时候，不去删除文件，而是在即将下载文件的时候，跳过下载（此时会保存json信息）
                    if (!hasDownloaded("https://chan.sankakucomplex.com" + url)) {
                        logger.info("spider - process ⭐ add TargetRequest " + "https://chan.sankakucomplex.com" + url);
                        page.addTargetRequest("https://chan.sankakucomplex.com" + url);
                    } else {
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
                    || nameLow.endsWith(".mov")) {
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
            SankakuSpiderProcessor.artworkInfoList.add(artworkInfo);
            logger.info("spider - FILE START DOWNLOAD:" + target);
            SankakuFileDownloadProcessor.download(target, name, SankakuSpiderProcessor.PARENT_PATH + SankakuSpiderProcessor.TAG + subFix, page, page.getUrl().toString());

        } else {
            // 走到了神奇的页面里面了
        }
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
    public Site getSite() {
        return site;
    }
}
