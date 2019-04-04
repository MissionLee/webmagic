package pers.missionlee.webmagic.spider.sankaku.pageprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.SankakuDownloadUtils;
import pers.missionlee.webmagic.spider.sankaku.SankakuInfoUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:20
 */
public class SankakuDownloadSpider extends AbstractSankakuSpider {
    Logger logger = LoggerFactory.getLogger(SankakuDownloadSpider.class);

    public List<ArtworkInfo> artworkInfos;
    public Map<String, String> artistInfo;

    // root 路径
    public String rootPath;

    // 作者名
    public String artistName;
    // 作者文件夹
    public File artistFile;
    public List<String> addedList;
    // 下载清空数量
    public int d_suc=0;
    public int d_skip=0;
    public int d_err=0;
    public int d_added=0;

    public SankakuDownloadSpider(Site site,String rootPath,String artistName) throws IOException {
        super(site);
        this.rootPath =rootPath;
        this.artistName = artistName;
        this.artworkInfos =SankakuInfoUtils.getArtworkInfoMap(rootPath+artistName);
        this.addedList = new ArrayList<String>();
        this.artistFile =new File(rootPath+artistName);
        if(!artistFile.exists()||!artistFile.isDirectory()){
            // 可能出现因为网络原因，或者目标nginx阻拦，导致某个作者没有被下载任何作品，但是执行流程
            // 结束，作者从 name.md文件夹里面出名，此时如果文件夹已经建立，那么在update的时候，还能补救这种情况
            artistFile.mkdir();
        }
    }

    public List<ArtworkInfo> getArtworkInfos() {
        return artworkInfos;
    }

    public Map<String, String> getArtistInfo() {
        return artistInfo;
    }

    private  boolean hasDownloaded(String URL) {
        for (ArtworkInfo info :
                artworkInfos) {
            if (info.getAddress().equals(URL))
                return true;
        }
        return false;
    }
    @Override
    public void process(Page page) {
        String URL = page.getUrl().toString();
        if (URL.contains("tags")) { // 如果访问的时列表页面
            processListPage(page, URL);
        } else if (page.getUrl().toString().contains("https://chan.sankakucomplex.com/post/show/")) {
            processDetailPage(page);
        } else {
            logger.warn("Went to page: " + page.getUrl());
        }
        logger.info("-suc: [" + d_suc + "/"+d_added+"] -err: [" + d_err + "/"+d_added+ "] -skip: " + d_skip);
    }

    private void processDetailPage(Page page) {
        // 详情页面
        Html html = page.getHtml();
        Target target= getTrgetInfo(html);
        ArtworkInfo artworkInfo = extractArtworkInfo(page, html, target);
        // TODO: 2019/3/2 1.下载 2.内存记录 3.日志记录
        logger.info("spider - FILE START DOWNLOAD:" + target.targetUrl);

        if (SankakuDownloadUtils.download(target.targetUrl, target.targetName, rootPath+artistName + target.subFix, page, page.getUrl().toString())) {
            // 下载成功
            // TODO: 2019/3/9  【完成】
            artworkInfos.add(artworkInfo);
            try {
                //infoUtils.appendInfo(artworkInfo);
                SankakuInfoUtils.appendArtworkInfo(artworkInfo,rootPath+artistName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            d_suc++;
        } else {
            d_err++;
        }
    }

    private void processListPage(Page page, String URL) {
        try {
            Thread.sleep(new Random().nextInt(40000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int thisPageAdded = 0;

        thisPageAdded = addNewArtworkToTarget(page, thisPageAdded);
        // TODO: 2019/3/17 用于内置的 update模式
        /**
         * 更新模式下，如果一个页面查询到的待更新内容超过1个，就添加下一个页面
         * 更新模式通过  DATE 关键字自动检测
         * @update 20190327 实际使用中发现大量的本页面更新一两个就够了，找下一页纯属浪费，
         *          并且在外层processor里面有udpate数量不过尝试遍历作者的方法 所以added改为 > 15
         * */
        if(thisPageAdded>15 && URL.contains("date")){
            String thisPage = URL.substring(URL.length()-1);
            int thisPageNum = Integer.valueOf(thisPage);
            if(thisPageNum<50){
                String urlPrefix = URL.substring(0,URL.length()-1);
                page.addTargetRequest(urlPrefix+ (++thisPageNum));
                logger.info("⭐ add： "+urlPrefix+thisPageNum);
            }
        }
    }

    private ArtworkInfo extractArtworkInfo(Page page,Html html,Target target) {
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
        extractTags(genreHtmlList,genreList);
        //  标签- meta
        List<String> metaHtmlList = tagSideBar.$(".tag-type-meta").$("a").all();
        List<String> metaList = new ArrayList<String>();
        extractTags(metaHtmlList,metaList);
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

        logger.info("spider - FILE END DOWNLOAD:" + target.targetUrl);
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
    private class Target{
        String targetUrl;
        String targetName;
        String subFix;
    }
    private int addNewArtworkToTarget(Page page, int thisPageAdded) {
        List<String> urlList = page.getHtml().$(".thumb").$("a", "href").all();
        /**
         * 通过 存档记录 判断是否需要添加页面中的子页面
         * */
        if (urlList != null && urlList.size() > 0) {
            for (String url : urlList
            ) {

                if (!hasDownloaded(BASE_URL + url) && !addedList.contains(url)) {
                    logger.info("⭐ add " + BASE_URL + url);
                    page.addTargetRequest(BASE_URL + url);
                    addedList.add(url);
                    thisPageAdded++;
                    d_added++;
                } else {
                    d_skip++;
                }
            }

        }
        return thisPageAdded;
    }
    private Target getTrgetInfo(Html html){
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
        target.subFix="/pic";
        if (nameLow.endsWith(".mp4")
                || nameLow.endsWith(".webm")
                || nameLow.endsWith(".avi")
                || nameLow.endsWith(".rmvb")
                || nameLow.endsWith(".flv")
                || nameLow.endsWith(".3gp")
                || nameLow.endsWith(".mov")
                || nameLow.endsWith(".swf")) {
            target.subFix = "/vid";
        }
        return target;
    }
}
