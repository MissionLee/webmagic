package pers.missionlee.webmagic.spider.sankaku.task;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-10 11:07
 */
@Deprecated
public class SankakuSpiderTask {
    public class DownloadTask {
        public String artistName;//作者名
        public int total=0;// 爬虫获取到的作品总量
        public int stored=0;// 任务开启前已经存储的作品量
        public int added=0;// 列表分析过程中添加的目标
        public int downloaded=0;// 成功下载的数量
        public int failed=0;// 下载失败的数量
        public long startTime;// 任务开始时间戳

        public String getTaskProgress() {
            return String.format("[作者:%1$s 任务情况:%2$d/%3$d/%4$d 存储情况:%5$d/%6$d/%7$d]", artistName, downloaded, failed, added, (downloaded + stored), (added + stored), total);
        }

        public DownloadTask(String artistName) {
            this.artistName = artistName;
            this.startTime=System.currentTimeMillis();
        }
    }

    public String rootPath;//根路径
    public int threadNum;// 线程数
    public boolean offical;// 是否有官方作品筛选需求
    public String taskType;
    public DownloadTask currentDownloadTask;
    public SourceManager sourceManager;
    public SourceManager.SourceType sourceType;

    public String getCurrentTaskProgress() {
        return currentDownloadTask.getTaskProgress();
    }
    public void resetDownloadTask(String artistName){
        this.currentDownloadTask = new DownloadTask(artistName);
    }
    public static SankakuSpiderTask buildTask(String configPath) throws IOException {
        System.out.println("使用文件配置:"+configPath);
        String rootPath;
        boolean official;
        int threadNum;
        String taskType;
        String config = FileUtils.readFileToString(new File(configPath), "utf8");
        Map<String, Object> configMap = (Map<String, Object>) JSON.parse(config);
        if (!configMap.containsKey("rootPath") || !configMap.containsKey("taskType")) {
            System.out.println("配置文件至少包含\n- rootPath :配置文件绝对路径，如果为\"relative\"表示rootPath为配置文件所在目录\n- taskType : \"new\"表示根据rootPath下name.md文件启动爬虫 \"manager\"表示进行更新操作");
            throw new RuntimeException("请检查配置文件");
        } else {
            if(configMap.get("rootPath").equals("relative")){

                int index = configPath.lastIndexOf("/");
                if(index==-1){
                    index = configPath.lastIndexOf("\\");
                }
                rootPath = configPath.substring(0,index);
            }else{
                rootPath=configMap.get("rootPath").toString();
            }
            if(!rootPath.endsWith("/"))
                rootPath+="/";
            if(!new File(rootPath).isDirectory()){
                throw new RuntimeException("rootPath错误:"+rootPath);
            }
            if(configMap.containsKey("official")){
                official = (Boolean) configMap.get("official");
            }else{
                official=false;
            }
            if(configMap.containsKey("threadNum")){
                threadNum = Integer.parseInt(configMap.get("threadNum").toString());

            }else{
                threadNum=4;
            }
            if(configMap.get("taskType").equals("manager")){
                taskType = "manager";
            }else{
                taskType = "new";
                // 检测 name.md 是否存在
                if(!new File(rootPath+"name.md").exists()){
                    throw new RuntimeException("未找到name.md文件");
                }
            }
        }
        SankakuSpiderTask task = new SankakuSpiderTask(rootPath,threadNum,official,taskType);
        return task;
    }

    public SankakuSpiderTask(String rootPath, int threadNum, boolean offical, String taskType) {
        this.rootPath = rootPath;
        this.threadNum = threadNum;
        this.offical = offical;
        this.taskType = taskType;
    }
}
