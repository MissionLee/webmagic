package pers.missionlee.webmagic.spider.newsankaku.task;

import com.alibaba.fastjson.JSON;
import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTaskController implements TaskController {
    protected String[] aimKeys;//搜索关键词 例如作者名称
    protected AimType aimType;
    protected int aimNum;
    protected String[] startUrls;
    protected long sleepTime = 100;
    protected List<String> storedSanCode;
    protected List<String> aimSanCode = new ArrayList<>();
    protected int saveNum = 0;// 本次下载成功保存数量

    protected NewSourceManager sourceManager;


    public AbstractTaskController(NewSourceManager newSourceManager) {
        this.sourceManager = newSourceManager;
    }

    @Override
    public void setStartUrls(String[] urls) {

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
        String sanCode = fullUrl.substring(fullUrl.lastIndexOf("/")+1);
        System.out.println(sanCode);
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
    public String getNumberCheckUrl() {
        return null;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    @Override
    public int getSaveNum() {
        return saveNum;
    }
}
