package pers.missionlee.webmagic.spider.sankaku.pageprocessor;

import pers.missionlee.webmagic.spider.sankaku.manager.SpiderTask;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @description:
 *  在该作者的页面找到作者作品数量
 * @author: Mission Lee
 * @create: 2019-03-22 14:47
 */
public class SankakuNumberSpider extends AbstractSankakuSpider {
    boolean offical;
    public SankakuNumberSpider(Site site, SpiderTask task){
        super(site,task);
        this.offical =task.isOfficial();
    }

    @Deprecated
    public SankakuNumberSpider(Site site,String rootPath,String artistName,boolean offical) {
        super(site,rootPath,artistName);
        this.offical = offical;
    }

    private int num=0;
    @Override
    public void process(Page page) {
        List<String> list = new ArrayList<String>();
        if(offical){
            list=page.getHtml().$(".tag-type-none").$("a").all();
        }else{
            list = page.getHtml().$(".tag-count").all();
        }

        System.out.println(list);
        Matcher matcher = htmlTextPattern.matcher(list.get(0));
        matcher.find();
        num = Integer.valueOf(matcher.group(1).trim().replaceAll(",",""));
    }

    public int getNum() {
        return num;
    }
}
