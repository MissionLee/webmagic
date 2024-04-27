package pers.missionlee.bili.starter;

import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;

import java.io.File;

public class BiliDiskService {
    public String BILI_BASE;

    public BiliDiskService(String BILI_BASE) {
        this.BILI_BASE = BILI_BASE;
    }
    private  void init(){
        File[] base = new File(this.BILI_BASE).listFiles();

    }
    public String getArtistPath(String bid){
        return PathUtils.buildPath(BILI_BASE,bid);
    }
    public String getArtistPath(String... path){
        String[] paths = new String[path.length+1];
        paths[0]=BILI_BASE;
        for (int i = 0; i < path.length; i++) {
            paths[i+1] = path[i];
        }
        return PathUtils.buildPath(paths);
    }
}
