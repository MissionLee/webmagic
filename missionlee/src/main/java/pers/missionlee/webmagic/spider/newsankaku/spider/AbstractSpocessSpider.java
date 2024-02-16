package pers.missionlee.webmagic.spider.newsankaku.spider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.Downloader;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractSpocessSpider implements PageProcessor {
    public boolean processCode429Page(Page page)  {
        if(page.getStatusCode() == 429){
            try {
                Thread.sleep(300000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            page.addTargetRequest(page.getUrl().toString());
        return false;
        }
        return  true;
    }
    Logger logger = LoggerFactory.getLogger(ArtistSpider.class);
    public static Map<Integer,String> tagType = new HashMap<>();
    static {
        // meta 蓝青色  例如 第三方编辑
        tagType.put(0,"general");//橙色
        tagType.put(1,"artist");//深红 jp06
        tagType.put(2,"studio"); // 粉红色  idolmaster
        tagType.put(3,"copyright");//紫色 fate/grand_order
        tagType.put(4,"character");//绿色  jougasaki_mika
        tagType.put(5,"genre");// 土黄色  例如强奸
        tagType.put(6,"");
        tagType.put(7,"");//
        tagType.put(8,"medium");//蓝色 uncensored doujinshi
        tagType.put(9,"meta");//蓝青色 tagme
    }
    protected TaskController task;

    public AbstractSpocessSpider(TaskController task) {

        this.task = task;
    }
    public abstract void doProcess(Page page);
    @Override
    public void process(Page page) {
        if(processCode429Page(page)){
            doProcess(page);
        }
    }

    @Override
    public Site getSite() {
        return SpiderUtils.site;
    }

    /**
     * 将当前分页下一页加入爬虫任务
     */
    protected void getNextPage(Page page) {
        String url = page.getUrl().toString();
        String thisPage = url.substring(url.lastIndexOf("=") + 1);
        System.out.println("当前是第 " + thisPage + " 页");
        int thisPageNum = Integer.valueOf(thisPage);
        if ((task.getWorkMode() == WorkMode.UPDATE && thisPageNum < 50)
                || (task.getWorkMode() == WorkMode.UPDATE_20_DATE_PAGE && thisPageNum < 20)
                || (task.getWorkMode() == WorkMode.UPDATE_10_DATE_PAGE && thisPageNum < 10)) {
            String urlPrefix = url.substring(0, url.lastIndexOf("=") + 1);
            System.out.println("添加下一页");
            page.addTargetRequest(urlPrefix + (++thisPageNum));
        }
    }

    public class ListNum {
        public int added;
        public int all;

        public ListNum(int all, int added) {
            this.added = added;
            this.all = all;
        }

        @Override
        public String toString() {
            return "本页总数：" + all + " | 添加总数：" + added;
        }
    }

    /**
     * 从列表中提取详情页
     */
    protected ListNum processList(Page page) {
//        try {
//            Thread.sleep(task.getSleepTime());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        List<String> urlList = page.getHtml().$(".content").$(".thumb").$("a", "href").all();

        if (urlList != null && urlList.size() > 0) {
            int added = 0;
            for (String url :
                    urlList) {
                // 此处获得 url形式为 /post/show/5287781
                if (task.addTarget(url)) {
                    page.addTargetRequest(url);
                    added++;
                } else {
                    task.confirmRel(SpiderUtils.BASE_URL + url);
                }
            }
            logger.info("新增：" + added + " 页面:" + page.getUrl().toString());
            ListNum l = new ListNum(urlList.size(), added);
            System.out.println(l);
            return l;
        }
        return new ListNum(0, 0);
    }

    /**
     * 从详情页开始下载
     */
    protected void processAim(Page page) {
        try {
            Thread.sleep(task.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Html html = page.getHtml();
        ArtistSpider.Target target = extractDownloadTargetInfoFromDetailPage(html);
        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        System.out.println(artworkInfo);

        try {

            Downloader.download(target.targetUrl, target.targetName, page.getUrl().toString(), task, artworkInfo);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 1. 解析作品信息
     * 2. 解析下载目标信息 URL 文件名
     * 3. 下载文件，下载成功后将作品信息写入文档记录
     */


    protected ArtworkInfo extractArtworkInfoFromDetailPage(Page page, ArtistSpider.Target target) {
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
        artworkInfo.setTagArtist(artistList);
        artworkInfo.setTagStudio(studioList);
        artworkInfo.setTagGenre(genreList);
        artworkInfo.setTagMeta(metaList);
        artworkInfo.setTakeTime(System.currentTimeMillis());

        artworkInfo.sanCode = page.getUrl().toString().substring(page.getUrl().toString().lastIndexOf("/") + 1);

        return artworkInfo;
    }

    public class Target {
        String targetUrl;
        String targetName;
        String subFix;
    }

    /**
     * 用于分析详情页Html，获取真正要下载的文件信息（主要处理了 1.页面展示缩略图/原图的情况，2.页面为图片/视频/flash文件的情况）
     */
    protected Target extractDownloadTargetInfoFromDetailPage(Html html) {
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

    public static void main(String[] args) {
        String x = "sadfasfasdfx=88";
        String thisPage = x.substring(x.lastIndexOf("=") + 1);
        System.out.println(thisPage);
        System.out.println(x.substring(0, x.lastIndexOf("=") + 1));
    }


}
