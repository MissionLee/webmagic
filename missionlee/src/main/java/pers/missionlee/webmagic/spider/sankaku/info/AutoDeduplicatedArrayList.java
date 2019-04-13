package pers.missionlee.webmagic.spider.sankaku.info;

import java.util.ArrayList;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-08 16:38
 */
public class AutoDeduplicatedArrayList<ArtworkInfo> extends ArrayList<ArtworkInfo> {
    @Override
    public boolean add(ArtworkInfo e) {
        for (ArtworkInfo artistInfo:this
             ) {
            if(artistInfo.equals(e)){
                return false;
            }
        }
        super.add(e);
        return true;
    }
}
