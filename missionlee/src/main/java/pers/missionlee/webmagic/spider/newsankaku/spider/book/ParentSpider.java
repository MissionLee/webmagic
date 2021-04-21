package pers.missionlee.webmagic.spider.newsankaku.spider.book;

import pers.missionlee.webmagic.spider.newsankaku.source.FakeSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.spider.AbstractSpocessSpider;
import pers.missionlee.webmagic.spider.newsankaku.task.ParentTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-27 17:41
 */
public class ParentSpider extends AbstractSpocessSpider {
    public ParentTaskController taskController ;

    public ParentSpider(TaskController task) {
        super(task);
        taskController = (ParentTaskController) task;
        System.out.println("XXX 请注意 有的页面 既有child 又有 parent 当前代码没法处理这种情况，但是不会出现无限循环BUG，例如：https://chan.sankakucomplex.com/post/show/24771598");
    }

    @Override
    public void doProcess(Page page) {
        String url = page.getUrl().toString();
        if(!taskController.parentPageDownloaded && url.contains("/show/")  ){
            String subFix = processParentPage(page);
            page.addTargetRequest("https://chan.sankakucomplex.com"+subFix);

        }else if(url.contains("parent")){
             processListPage(page);




        }else if(url.contains("/show/")){
//            processAim(page);
        }
    }

    private void processListPage(Page page) {
        System.out.println(page.getHtml().toString());
        List<String> hrefs = page.getHtml().$("#post-list").$(".content").$("span.thumb").$("a", "href").all();
        List<String> fileNames = page.getHtml().$("#post-list").$(".content").$("span.thumb").$("a").$(".preview","src").all();
        System.out.println("fileNames: "+fileNames);
        System.out.println(hrefs); // /post/show/24787374
        List<String> needDownloadHrefsSubFix = new ArrayList<>();
        for (int i = 0; i < hrefs.size(); i++) {
            String sanCode = hrefs.get(i).substring(11);
            String fileName = fileNames.get(i).substring(fileNames.get(i).lastIndexOf("/")+1);
            taskController.codeNamePairs.put(sanCode,fileName);
        }
        System.out.println(taskController.codeNamePairs);
        //
        for (int i = 0; i < hrefs.size(); i++) {
            System.out.println("根据页面结构， href 与 fileNames 获得的 List： hrefs 和 fileNames 顺序一致，所以直接取巧处理");
            String fileName = fileNames.get(i).substring(fileNames.get(i).lastIndexOf("/")+1);
            taskController.bookParentInfo.getArtworkInfo().setFileName(fileName);
        }
        List<String> nextPage = page.getHtml().$("#paginator").$(".pagination","next-page-url").all();

        // !!!! 必须先处理完所有页面，不然这里没法定位文件顺序
        if(!nextPage.isEmpty()){ // 如果有下一页，先处理下一页
            String subFix = nextPage.get(0); // /?next=24650900&amp;tags=parent%3A24636293&amp;page=2
            page.addTargetRequest(SpiderUtils.BASE_URL+subFix);
        }else{ // 如果没有下一页了，转移已有页面或加入下载列表
            // TODO: 3/27/2021  根据已经完成的  taskController.codeNamePairs  转移文件，或新增下载页面
        }

    }

    private String processParentPage(Page page) {
        taskController.parentPageDownloaded = true;
        Html html = page.getHtml();
        Target target = extractDownloadTargetInfoFromDetailPage(html);
        ArtworkInfo artworkInfo = extractArtworkInfoFromDetailPage(page, target);
        taskController.bookParentInfo.setArtworkInfo(artworkInfo);
        taskController.bookParentInfo.setArtistName(artworkInfo.getTagArtist());
        taskController.bookParentInfo.setInformation(artworkInfo);
        taskController.bookParentInfo.setArtistId(2);
        taskController.bookParentInfo.setCreatedAt(artworkInfo.getPostDate());
        taskController.bookParentInfo.setRating(artworkInfo.getRating());
        taskController.bookParentInfo.setUpdatedAt(artworkInfo.getPostDate());

        List<String> href = page.getHtml().$("#child-preview + div").$("a","href").all();
        String subFix = href.get(0); // 例如： /?tags=parent%3A24787325
        String id = subFix.substring(16);
        taskController.bookParentInfo.setId(Integer.valueOf(id));
        System.out.println("subFix / id:"+subFix+"_"+id);
        return subFix;
    }

    public static void main(String[] args) {
        ParentSpider p = new ParentSpider(new ParentTaskController(new FakeSourceManager(),"F://ROOT","F://ROOT"));
        Spider.create(p).addUrl("https://chan.sankakucomplex.com/post/show/24636293").thread(1).run();
    }
}
