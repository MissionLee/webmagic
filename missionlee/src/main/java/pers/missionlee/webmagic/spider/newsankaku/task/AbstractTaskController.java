package pers.missionlee.webmagic.spider.newsankaku.task;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.info.BookParentInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTaskController implements TaskController {
    protected String[] aimKeys;//搜索关键词 例如作者名称
    protected AimType aimType;
    protected int aimNum;
    protected String[] startUrls;
    protected long sleepTime = 100;
    protected Set<String> storedSanCode;
    protected List<String> aimSanCode = new ArrayList<>();
    protected int saveNum = 0;// 本次下载成功保存数量

    public SourceManager sourceManager;


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
        if (storedSanCode.contains(sanCode) || aimSanCode.contains(sanCode)) {
            System.out.println(sanCode + "已收录或已加入下载列表");
            return false;
        }

        else {
            System.out.println(sanCode + "加入下载列表");
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
        if(this instanceof ArtistTaskController){
            Set<String> possibleArtists = new HashSet<>();
            List<String> artistList = artworkInfo.getTagArtist();
            possibleArtists.addAll(artistList);
            possibleArtists.add(this.getAimKeys()[0]);
            ArtistSourceManager artistSourceManager = (ArtistSourceManager) sourceManager;
            String filename = artworkInfo.getFileName();
            System.out.println("验证所有可能的存放作者位置："+possibleArtists);
            AtomicBoolean exists = new AtomicBoolean(false);
            possibleArtists.forEach((String name)->{
                String parentPath = artistSourceManager.getArtworkDicOfAimArtist(AimType.ARTIST,filename,name);
                if(new File(parentPath+filename).exists()){
                    System.out.println("找到了："+parentPath+filename);
                    exists.set(true);
                }else{
                    System.out.println("没找到："+parentPath+filename);
                }

            });
            return  exists.get();
        }else{

        String parentPath = sourceManager.getAimDic(this, artworkInfo);
        String fileName = artworkInfo.getFileName();
        return new File(parentPath + fileName).exists();
        }
//        return false;
    }

    @Override
    public Boolean fileNameExistsInDB(ArtworkInfo artworkInfo) {
        String fileName = artworkInfo.getFileName();
        boolean exists = sourceManager.fileNameExist(fileName);
        System.out.println("Downloader判断数据库中是否标记文件存在（状态不为3丢失）："+fileName + (exists?"存在":"不存在") );
        return exists;
    }

    public boolean sanCodeExist(String sanCode){
        return  1 == sourceManager.sanCodeExist(sanCode);
    }
    @Override
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo, boolean infoOnly,boolean storeOnly) {
        if(StringUtils.isEmpty(artworkInfo.getFileName())){
            System.out.println("XXXXXXXXXXXXXXXXXXXX");
            artworkInfo.setFileName(fileName);
        }else{
            System.out.println("file name: "+fileName );
            System.out.println("info file name:"+artworkInfo.getFileName());
        }
        if(this instanceof BookTaskController){
            System.out.println("判断出了当前要保存 带序号的文件");
            BookTaskController taskController = (BookTaskController)this;

            String seq = taskController.filenameSequence.get(fileName);
            int x = 4-seq.length();
            String prefix = seq;
            for (int i = 0; i < x; i++) {
                prefix = "0"+prefix;
            }
            fileName = prefix+"_"+fileName;
        }

        if(infoOnly){// 如果只保存信息
            if(!storeOnly)
            sourceManager.saveArtworkInfo(artworkInfo);
            return true;
        }else{// 如果
            String aimDic = sourceManager.getAimDic(this,artworkInfo);
            try {
                if(!new File(aimDic+fileName).exists()){

                    FileUtils.moveFile(tempFile,new File(aimDic+fileName));
                }
                System.out.println("文件存储成功 "+ aimDic+"/"+fileName);
                artworkInfo.relativePath = aimDic.substring(aimDic.indexOf(":")+1);
                System.out.println("保存前artworkInfo： "+artworkInfo);
                if(!storeOnly)
                sourceManager.saveArtworkInfo(artworkInfo);
                this.saveNum++;
                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return  false;
    }

    @Override
    public void saveBookInfo(BookParentInfo bookParentInfo) {
        int x = sourceManager.saveBookInfo(bookParentInfo);
        System.out.println("保存结果： "+x);
    }
}
