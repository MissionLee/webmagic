package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 13:40
 */
public class ArtistInfo {
    private String name;
    private long updateTime;
    private int artworkNum;
    private Map<String,Integer> hotTags;
    private List<String> safePics;
    private List<String> questionablePics;
    private List<String> explicitPics;

    private List<String> safeVids;
    private List<String> questionableVids;
    private List<String> explicitVids;

    public ArtistInfo() {
        this.hotTags = new HashMap<String, Integer>();
        this.safePics = new ArrayList<String>();
        this.questionablePics = new ArrayList<String>();
        this.explicitPics = new ArrayList<String>();
        this.safeVids = new ArrayList<String>();
        this.questionableVids = new ArrayList<String>();
        this.explicitVids = new ArrayList<String>();
    }

    public Map<String, Integer> getHotTags() {
        return hotTags;
    }

    public void setHotTags(Map<String, Integer> hotTags) {
        this.hotTags = hotTags;
    }

    public List<String> getSafePics() {
        return safePics;
    }

    public void setSafePics(List<String> safePics) {
        this.safePics = safePics;
    }

    public List<String> getQuestionablePics() {
        return questionablePics;
    }

    public void setQuestionablePics(List<String> questionablePics) {
        this.questionablePics = questionablePics;
    }

    public List<String> getExplicitPics() {
        return explicitPics;
    }

    public void setExplicitPics(List<String> explicitPics) {
        this.explicitPics = explicitPics;
    }

    public List<String> getSafeVids() {
        return safeVids;
    }

    public void setSafeVids(List<String> safeVids) {
        this.safeVids = safeVids;
    }

    public List<String> getQuestionableVids() {
        return questionableVids;
    }

    public void setQuestionableVids(List<String> questionableVids) {
        this.questionableVids = questionableVids;
    }

    public List<String> getExplicitVids() {
        return explicitVids;
    }

    public void setExplicitVids(List<String> explicitVids) {
        this.explicitVids = explicitVids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public int getArtworkNum() {
        return artworkNum;
    }

    public void setArtworkNum(int artworkNum) {
        this.artworkNum = artworkNum;
    }
    @Override
    public String toString(){
        return JSON.toJSONString(this);
    }
}
