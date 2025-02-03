package pers.missionlee.bili.starter;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BiliArtistInfo {
    public String bid;
    public List<String> empty;
    public List<String> unknown;
    public List<String> member;
    public HashSet<String> del;
    public String path;
    public void save(String root) throws IOException {
        String iJson = JSON.toJSONString(this);
        FileUtils.writeStringToFile(new File(PathUtils.buildPath(path,"i.json")),iJson,"UTF8",false);// i.json不可改动
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
    public boolean isDel(String opusId){
        return del.contains(opusId);
    }
    public boolean skip(String opusId){
//        return false;
        return isEmpty(opusId)||isUnknown(opusId)||isMember(opusId)||isDel(opusId);
    }

    /***
     * ⭐ 这个依赖于 root 只能在有root的时候嗲用
     * @param root
     */
    public void dealDelFile(String root){
        File delFile = new File(PathUtils.buildPath(root,"zdel"));
        if(null == del){
            del = new HashSet<>();
        }
        if(delFile.exists()){
            File[] files = delFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                if(files[i].isDirectory()){
                    // 处理删除文件夹
                    String opusId = files[i].getName();
                    del.add(opusId);
                    File[] subs = files[i].listFiles();
                    for (int j = 0; j < subs.length; j++) {
                        subs[j].delete();
                    }
                    files[i].delete();
                    System.out.println("删除文件 "+opusId);
                }else{
                    // 处理删除文件
                    String fileName = files[i].getName();
                    String opusId = fileName.substring(0,fileName.indexOf("_"));
                    del.add(opusId);
                    files[i].delete();
                    System.out.println("删除文件 "+opusId);
                }
            }
        }
        try {
            this.save(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        BiliArtistInfo info = new BiliArtistInfo();
        info.bid = "44233161";
        info.dealDelFile("G:\\C-B-SLOW\\");
    }
}
