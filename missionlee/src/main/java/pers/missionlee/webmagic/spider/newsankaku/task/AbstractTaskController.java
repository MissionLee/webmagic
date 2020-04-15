package pers.missionlee.webmagic.spider.newsankaku.task;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractTaskController implements TaskController {
    protected String[] aimKeys;//搜索关键词 例如作者名称
    protected AimType aimType;
    protected int aimNum;
    protected String[] startUrls;
    protected long sleepTime = 100;
    protected Set<String> storedSanCode;
    protected List<String> aimSanCode = new ArrayList<>();
    protected int saveNum = 0;// 本次下载成功保存数量

    protected SourceManager sourceManager;


    public AbstractTaskController(SourceManager artistSourceManager) {
        this.sourceManager = artistSourceManager;

    }

    @Override
    public void setStartUrls(String[] urls) {
        this.startUrls = urls;
    }

    protected WorkMode workMode;
    protected String tempPath;
    protected int retryLimit = 3;

    @Override
    public void setAimKeys(String... keys) {
        this.aimKeys = keys;
    }

    @Override
    public String[] getAimKeys() {
        return aimKeys;
    }

    @Override
    public void setAimType(AimType aimType) {
        this.aimType = aimType;
    }

    @Override
    public AimType getAimType() {
        return aimType;
    }

    @Override
    public void setAimNum(int num) {
        this.aimNum = num;
    }

    @Override
    public int getAimNum() {
        return aimNum;
    }

    @Override
    public void setSleepTime(long millis) {
        this.sleepTime = millis;
    }

    @Override
    public long getSleepTime() {
        return sleepTime;
    }

    @Override
    public boolean addTarget(String fullUrl) {
        String sanCode = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
        System.out.println(sanCode);
        if (storedSanCode.contains(sanCode)) return false;
        else if (aimSanCode.contains(sanCode)) return false;
        else {
            aimSanCode.add(sanCode);
            return true;
        }
    }



    @Override
    public void setWorkMode(WorkMode workMode) {
        this.workMode = workMode;
    }

    @Override
    public WorkMode getWorkMode() {
        return workMode;
    }

    @Override
    public void setTempPath(String path) {
        this.tempPath = path;
    }

    @Override
    public String getTempPath() {
        return sourceManager.getTempPath();
    }

    @Override
    public void setRetryLimit(int limit) {
        this.retryLimit = limit;
    }

    @Override
    public int getRetryLimit() {
        return retryLimit;
    }


    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    @Override
    public int getSaveNum() {
        return saveNum;
    }

    @Override
    public Boolean existOnDisk(ArtworkInfo artworkInfo) {
        String parentPath = sourceManager.getAimDic(this, artworkInfo);
        String fileName = artworkInfo.getName();
        return new File(parentPath + fileName).exists();
    }

    @Override
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo, boolean infoOnly) {
        if(StringUtils.isEmpty(artworkInfo.getName())){
            System.out.println("XXXXXXXXXXXXXXXXXXXX");
            artworkInfo.setName(fileName);
        }else{
            System.out.println("file name: "+fileName );
            System.out.println("info file name:"+artworkInfo.getName());
        }
        if(infoOnly){
            sourceManager.saveArtworkInfo(artworkInfo);
            return true;
        }else{
            String aimDic = sourceManager.getAimDic(this,artworkInfo);
            try {
                if(!new File(aimDic+fileName).exists())
                    FileUtils.moveFile(tempFile,new File(aimDic+fileName));
                System.out.println("文件存储成功 "+ aimDic+"/"+fileName);
                artworkInfo.relativePath = aimDic.substring(aimDic.indexOf(":")+1);
                System.out.println("保存前artworkInfo： "+artworkInfo);
                sourceManager.saveArtworkInfo(artworkInfo);
                this.saveNum++;
                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return  false;
    }
}
