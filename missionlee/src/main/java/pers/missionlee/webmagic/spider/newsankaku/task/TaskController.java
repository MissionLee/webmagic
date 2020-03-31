package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;

/**
 * @description: 特殊目标下载任务
 * @author: Mission Lee
 * @create: 2020-03-30 19:16
 */
public interface TaskController {
    public void setAimKeys(String... name);
    public String[] getAimKeys();

    public void setAimType(AimType aimType);
    public AimType getAimType();

    public void setAimNum(int num);
    public int getAimNum();

    public String[] getStartUrls();
    public void setStartUrls(String[] urls);

    public void setSleepTime(long millis);
    public long getSleepTime();

    public boolean addTarget(String fullUrl);

    public boolean confirmRel(String fullUrl);

    public void setWorkMode(WorkMode workMode);
    public WorkMode getWorkMode();

    public void setTempPath(String path);
    public String getTempPath();

    public void setRetryLimit(int limit);
    public int getRetryLimit();

    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo);

    public String getNumberCheckUrl();

    public Boolean existOnDisk(String filename);
}
