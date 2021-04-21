package pers.missionlee.webmagic.spider.newsankaku.spider.book;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.bookparentsingle.BookParentSingleSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.spider.AbstractSpocessSpider;
import pers.missionlee.webmagic.spider.newsankaku.task.BookTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-21 20:09
 */
public class BookSpider extends AbstractSpocessSpider {
    public BookTaskController taskController;

    public BookSpider(TaskController task) {
        super(task);
        taskController = (BookTaskController) task;
    }

    String BOOK_PAGE_PREFIX = "https://beta.sankakucomplex.com/books/";
    String BOOK_INFO_RESTFUL_PREFIX = "https://capi-v2.sankakucomplex.com/pools/";
    String BOOK_DETAIL_RESTFUL_PREFIX = "https://capi-v2.sankakucomplex.com/posts/keyset";
    String SHOW_PAGE_PREFIX = "https://chan.sankakucomplex.com/post/show/";

    @Override
    public void doProcess(Page page) {

        // 主要思路：希望能够先保存book基础信息，再下载保存作品信息，防止作品保存的时候，book信息在数据库中还不存在
        String url = page.getUrl().toString();
        if (url.startsWith(BOOK_PAGE_PREFIX)) { // 如果是books 页面，提取books信息，放到 task里面
            System.out.println("因为代码结构问题，BookSpider 不方便一次完成一个作者的所有 book的分别下载，需要前置Spider确认book列表后，一个个使用BookSpider进行下载，*****请确保****在每个BookSpider运行之初，进行初始化工作（清空 BookTaskController 保存的 artworkSequence artworkNameSequence）");
            String bookId = url.substring(url.indexOf("books/") + 6, url.length());
            taskController.bookParentInfo.setId(Integer.valueOf(bookId));
            processBookPage(page);
            page.addTargetRequest(BOOK_INFO_RESTFUL_PREFIX + bookId + "?lang=en");
        } else if (url.startsWith(BOOK_INFO_RESTFUL_PREFIX)) {
            processPoolPageRestful(page);
            page.addTargetRequest(BOOK_DETAIL_RESTFUL_PREFIX + "?lang=en&default_threshold=1&hide_posts_in_books=in-larger-tags&limit=40&tags=pool:" + taskController.bookParentInfo.getId());
        } else if (url.startsWith(BOOK_DETAIL_RESTFUL_PREFIX)) {// pool 页面 获取作品顺序列表
            processPoolDetailPageRestful(page);
        } else if (url.startsWith(SHOW_PAGE_PREFIX)) {
            processShowPage(page);
        }
    }

    protected void processPoolPageRestful(Page page) {
        String html = page.getHtml().$("body").all().get(0).replaceAll("\n", "").replaceAll(" ", "");
        String data = SpiderUtils.extractTag(html);
        Map<String, Object> bookInfoMap = (Map<String, Object>) JSON.parse(data);
        System.out.println(JSON.toJSONString(bookInfoMap));
        taskController.bookParentInfo.setId((Integer) bookInfoMap.get("id"));
        taskController.bookParentInfo.setName((String) bookInfoMap.get("name"));
        taskController.bookParentInfo.setCreatedAt((String) bookInfoMap.get("created_at"));
        taskController.bookParentInfo.setArtistId((Integer) ((Map<String, Object>) (bookInfoMap.get("author"))).get("id"));
        List<Map<String, Object>> artists = (List<Map<String, Object>>) bookInfoMap.get("artist_tags");
        List<String> artistNames = new ArrayList<>();
        for (int i = 0; i < artists.size(); i++) {
            artistNames.add((String) artists.get(0).get("name"));
        }
        taskController.bookParentInfo.setArtistName(artistNames);
        // bookInfoMap里面内容比较多，减少一点
        List<Map<String, Object>> posts = (List<Map<String, Object>>) bookInfoMap.get("posts");
        List<Map<String, Object>> simplePosts = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", posts.get(i).get("id"));
            info.put("file_url", posts.get(i).get("file_url"));
        }
        bookInfoMap.put("posts", simplePosts);
        taskController.bookParentInfo.setInformation(bookInfoMap);
        taskController.saveBookInfo();
    }

    protected void processPoolDetailPageRestful(Page page) {
        // 通过page房问ajax接口会倍基础 <body>标签包裹，这里去除内容
        String html = page.getHtml().$("body").all().get(0).replaceAll("\n", "").replaceAll(" ", "");
        String data = SpiderUtils.extractTag(html);
        Map<String, Object> json = (Map<String, Object>) JSON.parse(data);
        // 处理 返回数据中的 data字段
        List<Map<String, Object>> detailData = (List<Map<String, Object>>) json.get("data");
        System.out.println(detailData);
        for (int i = 0; i < detailData.size(); i++) { //保存 文件排序  key : sanCode / value :sequence
            taskController.sanCodeSequence.put(detailData.get(i).get("id").toString(), detailData.get(i).get("sequence").toString());
            System.out.println("preview url: " + detailData.get(i).get("preview_url"));
            String fileName = detailData.get(i).get("preview_url").toString().substring(detailData.get(i).get("preview_url").toString().lastIndexOf("/") + 1);
            System.out.println("file Name : " + fileName);
            taskController.filenameSequence.put(fileName, detailData.get(i).get("sequence").toString());
        }
        // 处理返回数据中的 meta 字段
        Map<String, Object> meta = (Map<String, Object>) json.get("meta");
        if (meta.containsKey("next") && !"null".equals(meta.get("next")) && !(null == meta.get("next"))) { // 如果有下一页，解析下一页
            page.addTargetRequest(BOOK_DETAIL_RESTFUL_PREFIX + "?lang=en&next=" + meta.get("next") + "&default_threshold=1&hide_posts_in_books=in-larger-tags&limit=40&tags=pool:" + taskController.bookParentInfo.getId());
        } else {// 如果没有下一页，则房问 内容页面(注意：不直接下载 data信息中给出的数据，因为想获取普通页面提供的tag内容之类的)
            System.out.println(taskController.sanCodeSequence);
            Map<String, String> sanCodeMap = taskController.sanCodeSequence;
            Set<String> sanCodeSet = sanCodeMap.keySet();
            List<String> sanCodeList = new ArrayList<>();
            sanCodeList.addAll(sanCodeSet);
            for (int i = 0; i < sanCodeList.size(); i++) {
                String sanCode = sanCodeList.get(i);
                String sanCodeSeq = sanCodeMap.get(sanCode);
                if (taskController.sanCodeExist(sanCode)) {
                    Map<String, String> fileNameMap = taskController.filenameSequence;
                    Set<String> fileNameSet = fileNameMap.keySet();
                    List<String> fileNameList = new ArrayList<>();
                    fileNameList.addAll(fileNameSet);
                    String matchFileName = "";
                    for (int j = 0; j < fileNameList.size(); j++) {
                        String fileName = fileNameList.get(j);
                        String fileNameSeq = fileNameMap.get(fileName);
                        if (sanCodeSeq.equals(fileNameSeq)) {
                            matchFileName = fileName;
                        }
                    }
                    ArtworkInfo info = new ArtworkInfo();
                    info.setFileName(matchFileName);
                    List<String> artistNames = taskController.bookParentInfo.getArtistName();
                    info.setTagArtist(artistNames);
                    // TODO: 3/26/2021  
                    taskController.artistController.setAimKeys(artistNames.get(0));
                    boolean existInArtistPath = taskController.artistController.existOnDisk(info);
                    if (existInArtistPath) {
                        System.out.println("此文件存在再硬盘上================" + info.getFileName());
                        String storedPath = taskController.artistController.sourceManager.getAimDic(taskController.artistController, info);
                        System.out.println(storedPath);
                        File storedFile = new File(storedPath + matchFileName);
                        System.out.println(storedFile.exists());
                        String prefix = sanCodeSeq;
                        int x = 4 - sanCodeSeq.length();
                        for (int j = 0; j < x; j++) {
                            prefix = "0" + prefix;
                        }
                        try {
                            FileUtils.copyFile(storedFile, new File(taskController.sourceManager.getAimDic(taskController, info) + prefix + "_" + matchFileName));
                            FileUtils.moveFile(storedFile, new File(storedPath + "booked/" + matchFileName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("缺失：" + info.getFileName());
                    }
                }

            }
//            taskController.artworkSequence.forEach((String sanCode,String seq)->{
//                fileName = "";
//                System.out.println(sanCode);
//                if(taskController.sanCodeExist(sanCode)){
//                    // 如果这个sanCode已经存在了
//
//                    taskController.artworkNameSequence.forEach((String name,String s)->{
//                        if(seq.equals(s)){
//                            fileName = name;
//                        }
//                    });
//                    List<String> artists = taskController.bookInfo.getArtistName();
//                    System.out.println(artists+"_"+fileName);
//                    ArtworkInfo info = new ArtworkInfo();
//                    info.setName(fileName);
//                    boolean find = false;
//                    for (int i = 0; i < artists.size(); i++) {
//                        String artistNameKey = artists.get(i);
//                        // 转移文件过程中，需要通过 artistTag 确认存储目录 ，所以这里模拟一下
//                        List<String> artistTag = new ArrayList<>();
//                        artistTag.add(artistNameKey);
//                        info.setTagArtist(artistTag);
//                        System.out.println("作者："+artistNameKey);
//                        taskController.artistController.setAimKeys(artistNameKey);
//                        boolean existsInArtistPath = taskController.artistController.existOnDisk(info);
//                        System.out.println("作者目录中有此作品："+existsInArtistPath);
//                        // TODO: 3/25/2021  book的 exists on disk 判定有问题，因为 作品名前面有个前缀
//                        boolean existsInBookPath = taskController.existOnDisk(info);
//                        System.out.println("book目录有次作品："+existsInBookPath);
//                        if(existsInArtistPath ){
//                            System.out.println("此文件存在再硬盘上================"+info.getName());
//                            String storedPath = taskController.artistController.sourceManager.getAimDic(taskController.artistController,info);
//                            System.out.println(storedPath);
//                            File storedFile = new File(storedPath+fileName);
//                            System.out.println(storedFile.exists());
//                            String prefix = seq;
//                            int x = 4-seq.length();
//                            for (int j = 0; j < x; j++) {
//                                prefix = "0"+prefix;
//                            }
//                            try {
//                                FileUtils.copyFile(storedFile,new File(taskController.sourceManager.getAimDic(taskController,info)+prefix+"_"+fileName));
//                                FileUtils.moveFile(storedFile,new File(storedPath+"booked/"+fileName));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            find = true;
//                        }
//
//                    }
//                    if(!find ){ // 如果sanCode 存在，但是硬盘上没找到
//                        System.out.println("sanCode 找到了 但是硬盘上没有，所以下载");
//                        //  page.addTargetRequest(SHOW_PAGE_PREFIX+sanCode);
//                    }
//                }else {
//                    // 如果 sanCode 都不存在，直接下载
////                 page.addTargetRequest(SHOW_PAGE_PREFIX+sanCode);
//                }
//            });
        }
    }

    protected void processDetailPage(Page page) {
        try {
            Thread.sleep(task.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void processShowPage(Page page) {
        processAim(page);
    }

    protected void processBookPage(Page page) {
//        // 提取book标题
//        List<String> urlList = page.getHtml().$("h1").all();
//        String bookName = SpiderUtils.extractTag(urlList.get(0));
//        taskController.setBookName(bookName);
//        System.out.println("BookName: "+bookName);
//        // 提取 总数
//        List<String> total = page.getHtml().$("strong").all();
//        String num = SpiderUtils.extractTag(total.get(0));
//        taskController.setBookNum(Integer.valueOf(num));
//        System.out.println(total);
//        System.out.println("num "+num);
//        // 提取bookId / poolId  两个是同一个
//        String url = page.getUrl().toString();
//        String bookId = url.substring(url.indexOf("books/")+6,url.length());
//        taskController.setBookId(bookId);
    }

    public static void main(String[] args) {
        BookTaskController controller = new BookTaskController(new BookParentSingleSourceManager("I://ROOT", "I://ROOT,H://ROOT,I://ROOT-整理,I://ROOT-特别,H://ROOT-3D图片,H://ROOT-视频"), "I://ROOT", "I://ROOT,H://ROOT,I://ROOT-整理,I://ROOT-特别,H://ROOT-3D图片,H://ROOT-视频");
        BookSpider spider = new BookSpider(controller);
        Spider.create(spider).addUrl("https://beta.sankakucomplex.com/books/375250").thread(3).run();
    }
}
