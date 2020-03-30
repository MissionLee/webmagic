package pers.missionlee.webmagic.spider.newsankaku;

import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.Task;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskFactory;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceSpiderRunner;

/**
 * @description: 特别下载条件下的控制器，例如DOA系列，官方作品系列
 * @author: Mission Lee
 * @create: 2020-03-30 19:10
 */
public class SpeicalSpiderManager {
    public NewSourceManager newSourceManager;
    public SpeicalSpiderManager(NewSourceManager newSourceManager) {
        this.newSourceManager = newSourceManager;
    }
    private Task startSpider(Task task){
        NewSpiderRunner runner = new NewSpiderRunner();
        runner.runTask(task);
        return task;
    }
    public void updateSpeical(){

    };
    public void updateCopyRight(String name,boolean official){

    }
    public void updateCharacter(String name,boolean official){

    }
    public void updateArtist(String name,boolean official){
        Task task = TaskFactory.getBean();

    }
}
