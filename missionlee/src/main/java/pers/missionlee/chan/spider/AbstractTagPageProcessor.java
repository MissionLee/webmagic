package pers.missionlee.chan.spider;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
//import org.eclipse.jetty.util.ajax.JSON;
import pers.missionlee.chan.service.DataBaseService;
import pers.missionlee.chan.service.DiskService;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;
import us.codecraft.webmagic.Page;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-14 21:49
 */
public abstract class AbstractTagPageProcessor extends AbstractPageProcessor {
    List<String> tags;
    @Deprecated
    Set<String> storedSanCodes;
    List<String> relatedArtist;
    Map<String, String> storedFilesMd5; // 当前tag 存储目录下的文件列表
    //    Map<String, String> relatedStoredFilesMd5;// 当前tag 关联存储的 file:path 键值对
    Set<String> delFileName;
    Set<String> toBeDownloadSanCodes;
    public static int allowedPageNum = 50;
    public static boolean nextMode = true;

    public AbstractTagPageProcessor(List<String> tags, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        this.tags = tags;
        this.toBeDownloadSanCodes = new HashSet<>();
        this.storedSanCodes = initStoredSanCodes();
        this.relatedArtist = new ArrayList<>();
        this.relatedArtist.add(tags.get(0));
        logger.warn("注意，多个tag的时候，只把第一个tag放入 related tag 里面");
    }

    public AbstractTagPageProcessor(String tag, DataBaseService dataBaseService, DiskService diskService) {
        super(dataBaseService, diskService);
        List<String> tags = new ArrayList<>();
        tags.add(tag);
        this.tags = tags;
        this.toBeDownloadSanCodes = new HashSet<>();
        this.relatedArtist = new ArrayList<>();
        this.relatedArtist.add(tag);
    }

    @Deprecated
    public abstract Set<String> initStoredSanCodes();

    public abstract Map<String, String> initStoredFilesMd5();

    /**
     * 将 ListPage的目标添加到带下载目录里面，返回添加的数量
     */
    public int extractUrlFromListPage(Page page) {
        List<String> urlList = page.getHtml()
                .$(".thumb")
                .$("a", "href")
                .all();
        if (urlList != null && urlList.size() > 0) {
            int added = 0;
            for (String url :
                    urlList) {
                if (url.contains("plus")) {
                    logger.info("SanCode:[解析到会员页面] 跳过");
                } else {
                    String sanCode = url.substring(url.lastIndexOf("/") + 1);
                    if (storedSanCodes.contains(sanCode)) {
                        logger.info("SanCode:[" + sanCode + "] 已有");
                    } else if (toBeDownloadSanCodes.contains(sanCode)) {
                        logger.info("SanCode:[" + sanCode + "] 已在目标列表");
                    } else {
                        logger.info("SanCode:[" + sanCode + "] 加入队列");
                        page.addTargetRequest(url);
                        added++;
                    }
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

    public int extractUrlFromListPageWithFileNameFilter(Page page) {
        try {
            Thread.sleep(1000 * 3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AtomicInteger added = new AtomicInteger(0);
//        System.out.println(page.getHtml());
        List<String> src = page.getHtml()
                .$(".content>div>div>.posts-container  .post-preview")
                .$("img", "src")
                .all();
        List<String> url = page.getHtml()
                .$(".posts-container .post-preview")
                .$("a", "href")
                .all();
        logger.info("检测到列表页面，分析作品信息");
//        logger.info(JSON.toJSONString(src));
        logger.info(" src size "+src.size());
//        logger.info(JSON.toJSONString(url));
        logger.info(" url size "+url.size());
        if (null != src && src.size() > 0) {
            src.forEach((String fullSrc) -> {
                if (fullSrc.contains("no-visibility")) {
                    logger.info("[解析到会员页面] 跳过");
                } else {
                    int start = fullSrc.lastIndexOf("/") + 1;
                    int end = fullSrc.lastIndexOf(".");
                    String md5 = fullSrc.substring(start, end);

                    if(StringUtils.isEmpty(md5) || md5.length()<25){
                        logger.warn("检测到一场的md5 不下载此页面 "+md5);
                    }else{
                        if (storedFilesMd5.containsKey(md5)) {
                            logger.info("本作者MD5[" + md5 + "]已存在:" + storedFilesMd5.get(md5));
                        } else if (this.delFileName!= null && this.delFileName.contains(md5)) {
                            logger.info("本作者删除列表MD5[" + md5 + "]已存在");
                        } else {
                            int index = getIndexFromList(src, fullSrc);
                            String fullUrl = url.get(index);
                            String sanCode = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
                            if (toBeDownloadSanCodes.contains(sanCode)) {
                                logger.info("SanCode:[" + sanCode + "] 已在目标列表");

                            } else if (storedSanCodes.contains(sanCode)) {
                                //  md5 判断不存在，但是  sanCode 判断存在
                                // 此时通过已存在的 sanCode 关联作者，判断作者 是否已经被关联
                                //  1. 被关联了：认为作品需要下载
                                //  2. 没被关联：关联这个作者，重新验证作品 md5存在性
                                // TODO: 4/21/2021
                                extractArtistFileMd5BySanCode(sanCode);
                                if (storedFilesMd5.containsKey(md5)) {
                                    logger.info("SanCode存在，此作品在其他关联作者目录下，跳过下载");
                                } else {
                                    logger.info("SanCode存在，但SanCode关联的作者名下都没有这个文件md5，作品加入下载");
                                    this.toBeDownloadSanCodes.add(sanCode);
//                                logger.info("临时：代码里面把 文件不确认存在的时候下载的功能删除了 AbstractTagPageProcessor 136");
                                    page.addTargetRequest(fullUrl);
                                    added.getAndIncrement();
                                }

                            } else {
                                logger.info("验证MD5[" + md5 + "]不存在  SanCode[" + sanCode + "]不存在");
                                if (this instanceof ArtistPageProcessor
                                        && ((ArtistPageProcessor) this).onlyTryTen
                                ) {// 只有 1-sanX 并且配置 onlyTryTen 才会 有 onlyTryTen
                                    if (this.toBeDownloadSanCodes.size() < 5) {
                                        this.toBeDownloadSanCodes.add(sanCode);
                                        page.addTargetRequest(fullUrl);
                                        added.getAndIncrement();
                                    }
                                    {
                                        logger.info("启用了 临时限制了 每个作者只下载 10个");
                                    }
                                } else {
                                    this.toBeDownloadSanCodes.add(sanCode);
                                    page.addTargetRequest(fullUrl);
                                    added.getAndIncrement();
                                }

                            }
                    }

                    }
                }
            });
        }
        return added.get();
    }

    public synchronized boolean extractArtistFileByArtworkInfo(ArtworkInfo artworkInfo) {
        AtomicBoolean addNew = new AtomicBoolean(false);
        List<String> artists = artworkInfo.getTagArtist();
        artists.forEach((String name) -> {
            if (this instanceof CopyrightPageProcessor || this instanceof StudioPageProcessor || this.relatedArtist.contains(name)) {

            } else {
                Map<String, String> newMd5 = diskService.getArtistFileMd5Path(name);
                if (newMd5.size() > 0) {
//                    this.relatedStoredFilesMd5.putAll(newMd5);
                    this.storedFilesMd5.putAll(newMd5);
                    this.relatedArtist.add(name);
                    addNew.set(true);
                }
            }
        });
        return addNew.get();
    }

    public synchronized boolean extractArtistFileMd5BySanCode(String sanCode) {
        AtomicBoolean addNew = new AtomicBoolean(false);
        List<String> names = dataBaseService.getArtistBySanCode(sanCode);
        names.forEach((String name) -> {
            if (this.relatedArtist.contains(name)) {

            } else {
                Map<String, String> newMd5 = diskService.getArtistFileMd5Path(name);
                if (newMd5.size() > 0) {
//                    this.relatedStoredFilesMd5.putAll(newMd5);
                    this.storedFilesMd5.putAll(newMd5);
                    this.relatedArtist.add(name);
                    addNew.set(true);
                }
            }
        });
        return addNew.get();
    }

    public int getIndexFromList(List<String> list, String listItem) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(listItem)) return i;
        }
        return -1;
    }

    public void addNextPageAsTarget(Page page) {

        if (nextMode || true) {
            logger.info("--nextMode: 从网页中寻找下一页URL(代码逻辑中已经强制执行nextMode发现下一页)");
            List<String> nextPages = page.getHtml().$(".pagination", "next-page-url").all();
            if (nextPages.size() > 0) {
                logger.info("搜索到的URL："+nextPages.get(0));
                page.addTargetRequest((SpiderUtils.BASE_URL + nextPages.get(0))
                        .replaceAll("&amp;", "&")
                        .replaceAll("%2528","(")
                        .replaceAll("%2529",")")
                );
            }

        } else {
            String url = page.getUrl().toString();

            String thisPage = url.substring(url.lastIndexOf("=") + 1);
            int thisPageNum = Integer.valueOf(thisPage);
            String urlPrefix = url.substring(0, url.lastIndexOf("=") + 1);
            ++thisPageNum;
            if (thisPageNum <= allowedPageNum)
                page.addTargetRequest(urlPrefix + (thisPageNum));
        }

    }

}
