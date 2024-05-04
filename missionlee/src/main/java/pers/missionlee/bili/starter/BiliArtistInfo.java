package pers.missionlee.bili.starter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BiliArtistInfo {
    public String bid;
    public List<String> empty;
    public List<String> unknown;
    public List<String> member;
    public void save(String root) throws IOException {
        String iJson = JSON.toJSONString(this);
        FileUtils.writeStringToFile(new File(PathUtils.buildPath(root,bid,"i.json")),iJson,"UTF8",false);// i.json不可改动
    }
    public boolean isEmpty(String opusId){
        return empty.contains(opusId);
    }
    public boolean isUnknown(String opusId){
        return  unknown.contains(opusId);
    }
    public boolean isMember(String opusId){
        return  member.contains(opusId);
    }
    public boolean skip(String opusId){
        return isEmpty(opusId)||isUnknown(opusId)||isMember(opusId);
    }
}
