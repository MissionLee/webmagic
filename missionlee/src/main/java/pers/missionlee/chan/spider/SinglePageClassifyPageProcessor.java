package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;

import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-10 21:14
 */
public class SinglePageClassifyPageProcessor extends AbstractPageProcessor {
    public Set<String> singleUrl;
    public Set<String> parentUrl;
    public Set<String> bookUrl;

    public SinglePageClassifyPageProcessor(Set<String> singleUrl, Set<String> parentUrl, Set<String> bookUrl, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        this.singleUrl = singleUrl;
        this.parentUrl = parentUrl;
        this.bookUrl = bookUrl;
    }

    @Override
    public void onDownloadSuccess(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void onDownloadFail(Page page, ArtworkInfo artworkInfo) {

    }

    @Override
    public void doProcess(Page page) {
        String pageString = page.getHtml().toString();
        if (pageString.contains("这个帖子已经删除")
                || pageString.contains("This post was deleted")
                || pageString.contains("您没有查看该内容所需要的访问权限")
                || pageString.contains("You lack the access rights required to view this content")
        ) {
            logger.info("文件被删除，或者没有访问权限，跳过这个作品：" + page.getUrl().toString());
        } else {
            // 1.提取target信息
            AbstractPageProcessor.Target target = extractDownloadTargetInfoFromDetailPage(page.getHtml());
            // 2.提取 ArtworkInfo 信息
            ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
            List<String> artists = artworkInfo.getTagArtist();
            boolean needDownload = true;
            if (null != artists && artists.size() > 0) {
                for (int i = 0; i < artists.size(); i++) {
                    // 如果
                    if (dataBaseService.checkArtistIsTarget(artists.get(i))) {
                        needDownload = false;
                    }
                }
            }
            if (!needDownload) {
                logger.info("这个作品属于某个已经收藏的作者，所以跳过");
                return;
            }
            if (pageString.contains("This post has") && pageString.contains("child post")) { // 如果当前页面是Parent中的母页面，直接加入带下载列表
                logger.info("ParentPage:" + page.getUrl());

//            System.out.println("“有些”子作品没有记录作品信息（例如作者），所以交给ParentSpider的页面是“母作品”页面，而不是作品列表页面");
                logger.info("添加一个parentUrl："+page.getUrl().toString());
                this.parentUrl.add(page.getUrl().toString());

            } else if (pageString.contains("This post belongs to") && pageString.contains("a parent post")) {// 如果当前页面是Parent中的子页面，将母页面加入下载列表
                logger.info("ParentPage:" + page.getUrl());
//            System.out.println("当前页面是某个Parent的子页面，跳转Parent页面");
                List<String> href = page.getHtml().$("#parent-preview + div").$("a", "href").all();
                logger.info("添加一个parentUrl："+"https://chan.sankakucomplex.com" + href.get(0));
                this.parentUrl.add("https://chan.sankakucomplex.com" + href.get(0));
//            page.addTargetRequest("https://chan.sankakucomplex.com" + href.get(0));

            } else if (pageString.contains("Books") && pageString.contains("&gt;&gt;") && pageString.contains("legacy")) { // 怕 Books 可能不准确，所以多验证
                List<String> href = page.getHtml().$(".content>.status-notice").$("a", "href").all();
                bookUrl.add(href.get(0));

                logger.info("BookUrl:" + href.get(0));
            } else {
                logger.info("单纯Single：直接下载");
                artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.SINGLE.storePlace;
                artworkInfo.artistType = ArtworkInfo.ARTIST_TYPE.UNKNOWN.artistType;
                artworkInfo.isSingle = true;
                artworkInfo.PBPrefix = "B";
                artworkInfo.fileSaveName = artworkInfo.fileName;
                boolean download = downloadAndSaveFileFromShowPage(target, artworkInfo, page);
                if (download) {
                    dataBaseService.saveArtworkInfo(artworkInfo);
                }
                singleUrl.add(page.getUrl().toString());
            }
        }


    }

    public static boolean belongsToBook(String pageString) {
        return pageString.contains("Books") && pageString.contains("&gt;&gt;") && pageString.contains("legacy");
    }
}
