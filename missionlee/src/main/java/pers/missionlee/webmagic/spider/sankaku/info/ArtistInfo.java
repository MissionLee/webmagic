package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;

import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 13:40
 */
public class ArtistInfo {
    private String name;
    private Map<String,Long> fileList;
    private long updateTime;
    private int artworkNum;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Long> getFileList() {
        return fileList;
    }

    public void setFileList(Map<String, Long> fileList) {
        this.fileList = fileList;
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
