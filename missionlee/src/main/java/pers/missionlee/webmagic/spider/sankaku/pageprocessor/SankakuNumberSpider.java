package pers.missionlee.webmagic.spider.sankaku.pageprocessor;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;

import java.util.List;
import java.util.regex.Matcher;

/**
 * @description:
 *  在该作者的页面找到作者作品数量
 * @author: Mission Lee
 * @create: 2019-03-22 14:47
 */
public class SankakuNumberSpider extends AbstractSankakuSpider {
    public SankakuNumberSpider(Site site) {
        super(site);
    }

    private int num=0;
    @Override
    public void process(Page page) {
        List<String> list = page.getHtml().$(".tag-count").all();
        System.out.println(list);
        Matcher matcher = htmlTextPattern.matcher(list.get(0));
        matcher.find();
        num = Integer.valueOf(matcher.group(1).trim().replaceAll(",",""));
    }

    public int getNum() {
        return num;
    }
}
