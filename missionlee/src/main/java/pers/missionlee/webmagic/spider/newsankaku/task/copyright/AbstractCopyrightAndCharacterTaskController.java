package pers.missionlee.webmagic.spider.newsankaku.task.copyright;

import com.sun.istack.internal.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.AbstractTaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-22 11:17
 */
public abstract class AbstractCopyrightAndCharacterTaskController extends AbstractTaskController {
    public  List<String> DEFAULT_CHARACTERS = new ArrayList<>();
    public  List<String> DEFAULT_COPYRIGHTS = new ArrayList<>();
    public  List<String> DEFAULT_TAGS = new ArrayList<>();
    public String copyRight;
    public String character;
    public String[] searchTags;

    public AbstractCopyrightAndCharacterTaskController(SourceManager artistSourceManager) {
        super(artistSourceManager);
    }



    @Override
    public boolean confirmRel(String fullUrl) {
        return false;
    }


    @Override
    public String getNumberCheckUrl() {
        return null;
    }

    public String[] getDefaultCommonStartUrls(){
        String[] characterUrls = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_CHARACTERS,null,null);
        String[] copyRights = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_COPYRIGHTS,null,null);
        return  ArrayUtils.addAll(characterUrls,copyRights);
    }
    public String[] getDefault3DStartUrls(){
        String[] keys = new String[1];
        keys[0] = "3d";
        String[] characterUrls = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_CHARACTERS,keys,null);
        String[] copyRights = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_COPYRIGHTS,keys,null);
        return ArrayUtils.addAll(characterUrls,copyRights);
    }
    public String[] getDefaultNone3DStartUrls(){
        String[] limit = new String[1];
        limit[0] = "3d";
        String[] characterUrls = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_CHARACTERS,null,limit);
        String[] copyRights = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_COPYRIGHTS,null,limit);
        return  ArrayUtils.addAll(characterUrls,copyRights);
    }
    public String[] getDefaultVideoStartUrls(){
        String[] keys = new String[1];
        keys[0] = "video";
        String[] characterUrls = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_CHARACTERS,keys,null);
        String[] copyRights = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_COPYRIGHTS,keys,null);
        return ArrayUtils.addAll(characterUrls,copyRights);
    }
    public String[] getDefaultNoneVideoStartUrls(){
        String[] limit = new String[1];
        limit[0] = "video";
        String[] characterUrls = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_CHARACTERS,null,limit);
        String[] copyRights = getStartUrlsWithKeysAndLimitBaseOnAimList(DEFAULT_COPYRIGHTS,null,limit);
        return ArrayUtils.addAll(characterUrls,copyRights);

    }
    public String[] getStartUrlsWithKeysAndLimitBaseOnAimList(List<String> aimList, @Nullable String[] keys,@Nullable String[] limits){
        if(keys==null) keys = new String[0];

        if(limits==null) {limits = new String[0];}else{
            for (int i = 0; i < limits.length; i++) {
                if(!limits[i].startsWith("-")) limits[i] = "-"+limits[i].trim();
            }
        }
        String[] generatedUrls = new String[0];
        int keyNum = keys.length+limits.length+1;
        String[] generatedKeys = new String[keyNum];
        System.arraycopy(keys,0,generatedKeys,0,keys.length);
        System.arraycopy(limits,0,generatedKeys,keys.length,limits.length);
        if (workMode == WorkMode.UPDATE) {
            for (String character : aimList
            ) {
                generatedKeys[generatedKeys.length-1] = character;
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, generatedKeys));
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, generatedKeys));
            }

        } else if (workMode == WorkMode.UPDATE_ALL || workMode == WorkMode.NEW) {
            for (String character : aimList) {
                generatedKeys[generatedKeys.length-1] = character;
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, generatedKeys));
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, generatedKeys));
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, generatedKeys));
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, generatedKeys));
                generatedUrls = ArrayUtils.add(generatedUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, generatedKeys));
            }
        }
        return generatedUrls;
    }
}
