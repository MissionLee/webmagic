package pers.missionlee.webmagic.spider.newsankaku.task;

import com.alibaba.fastjson.JSON;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;

import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-14 10:54
 */
public class OfficialTaskController extends AbstractTaskController{
    public OfficialTaskController(SourceManager sourceManager,String officialName){
        super(sourceManager);
        aimKeys = new String[2];
        aimKeys[0] = officialName;
        aimKeys[1] = "official art";
        Set<String> codes = sourceManager.getStoredSanCode(this);
        storedSanCode = codes;
        System.out.println("当前official art已存作品： "+storedSanCode.size() + "xxxxxxxxxxxxxxxxxxxx");
        System.out.println(JSON.toJSONString(storedSanCode));

    }

    String [] startUrls;
    @Override
    public String[] getStartUrls() {
        if(startUrls != null){
            return startUrls;
        }
        if(workMode == WorkMode.NEW){
            startUrls = SpiderUtils.getStartUrls(aimNum,aimKeys);
        }else if(
             workMode == WorkMode.UPDATE
                || workMode == WorkMode.UPDATE_10_DATE_PAGE
                || workMode == WorkMode.UPDATE_20_DATE_PAGE
        ){
            startUrls = new String[1];
            startUrls[0] = SpiderUtils.getUpdateStartUrl(getAimKeys());
        }
        return startUrls;
    }

    @Override
    public boolean confirmRel(String fullUrl) {
        return false;
    }

//    @Override
//    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo, boolean infoOnly,boolean storeOnly) {
//
//        return false;
//    }

    @Override
    public String getNumberCheckUrl() {
        return SpiderUtils.getNumberCheckUrl(aimKeys);
    }

//    @Override
//    public Boolean existOnDisk(ArtworkInfo artworkInfo) {
//        return null;
//    }


}
