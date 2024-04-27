package pers.missionlee.bili.spider;

import pers.missionlee.bili.starter.BiliArtistInfo;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

public class BidPageProcessor implements PageProcessor {
    BiliArtistInfo artistInfo;
    public BidPageProcessor(BiliArtistInfo info) {
        this.artistInfo = info;
    }

    @Override
    public void process(Page page) {


    }

    @Override
    public Site getSite() {
        return null;
    }
}
