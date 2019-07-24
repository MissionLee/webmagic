package pers.missionlee.webmagic.spider.sankaku.manager;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-13 08:57
 */
public class SpiderTaskFactory {
    static Logger logger = LoggerFactory.getLogger(SpiderTaskFactory.class);
    private SourceManager sourceManager;
    private SourceManager.SourceType defaultSourceType = SourceManager.SourceType.SANKAKU;
    private int defaultThreadNum = 3;
    private boolean defaultOfficial = false;
    private int defaultDownloadRetryTimes = 3;
    private SpiderTask.TaskType defaultTaskType = SpiderTask.TaskType.NEW;

    public SpiderTaskFactory(SourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }

    public SpiderTaskFactory config(Map<String, Object> spiderTaskConfig) {
        if (spiderTaskConfig.containsKey("sourceType")) {
            if (spiderTaskConfig.get("sourceType").getClass() == SourceManager.SourceType.class)
                this.defaultSourceType = (SourceManager.SourceType) spiderTaskConfig.get("sourceType");
            else if (spiderTaskConfig.get("sourceType").getClass() == String.class) {
                if (spiderTaskConfig.get("sourceType").toString().toLowerCase().equals("idol")) {
                    this.defaultSourceType = SourceManager.SourceType.IDOL;
                } else if (spiderTaskConfig.get("sourceType").toString().toLowerCase().equals("sankaku")) {

                } else {
                    throw new RuntimeException("未知的SourceType：" + spiderTaskConfig.get("sourceType"));
                }
            } else {
                throw new RuntimeException("不支持的SourceType类型：" + spiderTaskConfig.get("sourceType").getClass());
            }
        }
        if (spiderTaskConfig.containsKey("threadNum")) {
            this.defaultThreadNum = Integer.parseInt(spiderTaskConfig.get("threadNum").toString());
        }
        if (spiderTaskConfig.containsKey("official")) {
            this.defaultOfficial = Boolean.parseBoolean(spiderTaskConfig.get("official").toString());
        }
        if (spiderTaskConfig.containsKey("downloadRetryTimes")) {
            this.defaultThreadNum = Integer.parseInt(spiderTaskConfig.get("downloadRetryTimes").toString());
        }
        if (spiderTaskConfig.containsKey("taskType")) {
            if (spiderTaskConfig.get("taskType").getClass() == SpiderTask.TaskType.class)
                this.defaultTaskType = (SpiderTask.TaskType) spiderTaskConfig.get("taskType");
            else if (spiderTaskConfig.get("taskType").getClass() == String.class) {
                String taskType = spiderTaskConfig.get("taskType").toString().toLowerCase();
                if (taskType.equals("new"))
                    this.defaultTaskType = SpiderTask.TaskType.NEW;
                else if (taskType.equals("manager"))
                    this.defaultTaskType = SpiderTask.TaskType.UPDATE;
                else
                    throw new RuntimeException("未知的TaskType：" + taskType);
            } else {
                throw new RuntimeException("不支持的TaskType类型：" + spiderTaskConfig.get("taskType").getClass());

            }
        }
        return this;
    }

    public SpiderTaskFactory config(String jsonConfigFilePath) {
        File file = new File(jsonConfigFilePath);
        if (file.exists()) {
            try {
                String json = FileUtils.readFileToString(file, "utf8");
                Map<String, Object> spiderTaskConfig = (Map<String, Object>) JSON.parse(json);
                config(spiderTaskConfig);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("无法读取被指文件，将使用默认配置");
            }
        } else {
            System.out.println("未找到正确的配置文件，将使用默认配置");
        }
        return this;
    }

    public SpiderTask getSpiderTask(SourceManager.SourceType sourceType, String artistName,String dirName ,boolean offical, SpiderTask.TaskType taskType, boolean getAll, SourceManager sourceManager) throws IOException {
        SpiderTask spiderTask = new SpiderTask(this.sourceManager, sourceType, defaultThreadNum, artistName, dirName,offical, defaultDownloadRetryTimes, taskType, getAll);
        List<ArtworkInfo> artworkInfos = sourceManager.getArtworkOfArtist(sourceType, dirName);
        for (ArtworkInfo a :
                artworkInfos) {
            spiderTask.artworkAddress.add(a.getAddress());

        }

        spiderTask.stored = sourceManager.getArtworkNum(sourceType, dirName);

        return spiderTask;
    }

}
