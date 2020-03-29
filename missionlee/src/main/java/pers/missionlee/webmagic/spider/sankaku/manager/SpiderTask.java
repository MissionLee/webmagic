package pers.missionlee.webmagic.spider.sankaku.manager;

import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-12 15:10
 */
public class SpiderTask {
    private static String[] emptyStringArray = new String[0];
//    private static List<String> emptyStringList = new ArrayList<String>();
    public enum TaskType {
        UPDATE("更新"),
        NEW("遍历"),
        POPULAR("进步作者获取热门即可");
        String desc;
        TaskType(String desc) {
            this.desc=desc;
        }
    }
    // 创建时参数 这些值
    private SourceManager sourceManager;
    private SourceManager.SourceType sourceType;
    private int threadNum;
    private String artistName;//作者名
    private String dirName;
    private boolean official;
    private int downloadRetryTimes;
    private TaskType taskType;
    // 获取全部标志位 在NEW/UPDATE 情况下会根据不同的判断条件开启 二次重下载 以尝试获取全部内容（仅额外尝试一次）
    private boolean getAll;
    public int popularPageNum = 1;
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public boolean isGetAll() {
        return getAll;
    }

    public SourceManager getSourceManager() {
        return sourceManager;
    }

    public SourceManager.SourceType getSourceType() {
        return sourceType;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public String getArtistName() {
        return artistName;
    }

    public boolean isOfficial() {
        return official;
    }

    public int getDownloadRetryTimes() {
        return downloadRetryTimes;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    // 运行时参数
    public int total=0;// 爬虫获取到的作品总量
    public int stored=0;// 任务开启前已经存储的作品量
    public int added=0;// 列表分析过程中添加的目标
    public int downloaded=0;// 成功下载的数量
    public int failed=0;// 下载失败的数量
    public String[] startUrls=emptyStringArray;
    public List<String> artworkAddress = new ArrayList<String>();
    public List<String> targetUrl = new ArrayList<String>();

    public SpiderTask(SourceManager sourceManager, SourceManager.SourceType sourceType, int threadNum, String artistName,String dirName, boolean official, int downloadRetryTimes, TaskType taskType,boolean getAll) {
        this.sourceManager = sourceManager;
        this.sourceType = sourceType;
        this.threadNum = threadNum;
        this.artistName = artistName;
        this.official = official;
        this.downloadRetryTimes = downloadRetryTimes;
        this.taskType = taskType;
        this.getAll = getAll;
        this.dirName = dirName;
        // 检测本地是否有作者名称对应的文件
        try {
            // TODO: 2019-12-29  原本直接调用创建 json文本信息的方法 
            sourceManager.guaranteeArtistInfoFileExists(this.sourceType,this.dirName);
        } catch (IOException e) {
            System.out.println("= xx =");
        }
    }
    public void resetRunTimeParams(){
        this.total = 0;
        this.stored = 0;
        this.added=0;
        this.downloaded=0;
        this.failed =0;
        this.startUrls = emptyStringArray;
        this.artworkAddress = new ArrayList<String>();
        this.targetUrl = new ArrayList<String>();
    }
    public String getTmpPath(){
        return this.sourceManager.tmpPath;
    }
    public String getTaskProgress() {
        return String.format("[作者:%1$s 任务情况:%2$d/%3$d/%4$d 存储情况:%5$d/%6$d/%7$d]", artistName, downloaded, failed, added, (downloaded + stored), (added + stored), total);
    }
    public boolean existsInTmpPath(String filename){
        // 如果在临时路径中，挪到目标路径去
        File tmp =this.sourceManager.getFileInTmpPath(filename);
        if(tmp.exists()){
            return saveFile(tmp,filename);
        }else{
            return false;
        }

    }
    public boolean exists(String filename){
        return this.sourceManager.existInDB(this.sourceType,this.dirName,filename);
    }
    public Boolean saveFile(File tmpFile,String artworkName){
        // TODO: 2019-04-26
        return this.sourceManager.saveFile(this.sourceType,tmpFile,this.dirName,artworkName);
    }
    public void appendArtworkInfo(ArtworkInfo artworkInfo,String artistName) throws IOException {
        String address = artworkInfo.getAddress();
        this.artworkAddress.add(address);
        this.sourceManager.recordArtworkInfo(this.sourceType,this.dirName,artworkInfo,artistName);

    }
    public void confirmArtworkArtistRel(String fullUrl){
        this.sourceManager.confirmArtworkArtistRel(fullUrl,this.getArtistName());
    }
    @Override
    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append("-------------------");
        buffer.append("\n| 作者："+artistName);
        buffer.append("\n| 源类型："+sourceType.desc);
        buffer.append("\n| 任务类型："+taskType.desc);
        buffer.append("\n-------------------");
        return buffer.toString();
    }
    public int getArtworkNumOfDB(String artistName){
        return sourceManager.sankakuDBSourceManager.getArtistWorkNum(artistName);
    }
}
