package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;

/**
 * @description: 特殊目标下载任务
 * @author: Mission Lee
 * @create: 2020-03-30 19:16
 */
public interface Task {
    public void setAimType();
    public AimType getAimType();

    public String[] getStartUrls();
    public void setStartUrls(String[] urls);

    public void setSleepTime(long millis);
    public long getSleepTime();

    public boolean addTarget(String fullUrl);

    public boolean confirmRel(String fullUrl);

    public void setWorkMode(WorkMode workMode);
    public WorkMode getWorkMode();
}
