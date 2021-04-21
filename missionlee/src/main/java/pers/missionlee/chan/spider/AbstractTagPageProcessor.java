package pers.missionlee.chan.spider;

import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.newsankaku.spider.AbstractSpocessSpider;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-14 21:49
 */
public abstract class AbstractTagPageProcessor extends AbstractPageProcessor {
    List<String> tags;
    Set<String> storedSanCodes;
    Set<String> toBeDownloadSanCodes;
    public AbstractTagPageProcessor(List<String> tags, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        this.tags = tags;
        this.toBeDownloadSanCodes = new HashSet<>();
        this.storedSanCodes = initStoredSanCodes();
    }

    public AbstractTagPageProcessor(String tag, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        List<String> tags = new ArrayList<>();
        tags.add(tag);
        this.tags = tags;
        this.toBeDownloadSanCodes = new HashSet<>();
    }

    public abstract Set<String> initStoredSanCodes();
    /**
     * 将 ListPage的目标添加到带下载目录里面，返回添加的数量
     * */
    public int extractUrlFromListPage(Page page){
        List<String> urlList = page.getHtml().$(".thumb").$("a", "href").all();
        if (urlList != null && urlList.size() > 0) {
            int added = 0;
            for (String url :
                    urlList) {
                String sanCode = url.substring(url.lastIndexOf("/")+1);
                if(storedSanCodes.contains(sanCode)){
                    logger.info("SanCode:["+sanCode+"]已有");
                }else if(toBeDownloadSanCodes.contains(sanCode)){
                    logger.info("SanCode:["+sanCode+"]已在目标列表");
                }else{
                    logger.info("SanCode:["+sanCode+"]加入队列");
                    page.addTargetRequest(url);
                    added++;
                }
                // 此处获得 url形式为 /post/show/5287781
//                if (task.addTarget(url)) {
//                    page.addTargetRequest(url);
//                    added++;
//                } else {
//                    task.confirmRel(SpiderUtils.BASE_URL + url);
//                }
            }
            return added;
        }
        return 0;
    }
    public void addNextPageAsTarget(Page page){
        String url = page.getUrl().toString();
        String thisPage = url.substring(url.lastIndexOf("=") + 1);
        int thisPageNum = Integer.valueOf(thisPage);
        String urlPrefix = url.substring(0, url.lastIndexOf("=") + 1);
        page.addTargetRequest(urlPrefix + (++thisPageNum));
    }
}
