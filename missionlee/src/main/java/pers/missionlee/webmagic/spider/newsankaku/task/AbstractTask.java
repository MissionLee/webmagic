package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.List;

public  class AbstractTask implements Task{

    private AimType aimType;
    private String[] startUrls;
    private long sleepTime;
    private List<String> storedSanCode;
    private List<String> aimSanCode;
    private WorkMode workMode;
    private String tempPath;
    private int retryLimit;
    @Override
    public void setAimType(AimType aimType) {
        this.aimType = aimType;
    }

    @Override
    public AimType getAimType() {
        return aimType;
    }

    @Override
    public String[] getStartUrls() {
        return startUrls;
    }

    @Override
    public void setStartUrls(String[] urls) {
        this.startUrls = urls;
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
        String sanCode = fullUrl.substring(fullUrl.lastIndexOf("/")+1);
        if(storedSanCode.contains(sanCode)) return false;
        else if(aimSanCode.contains(sanCode)) return false;
        else{
            aimSanCode.add(sanCode);return true;
        }
    }

    @Override
    public boolean confirmRel(String fullUrl) {
        return false;
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
        return tempPath;
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
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo) {
        return false;
    }
}
