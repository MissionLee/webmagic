package pers.missionlee.webmagic.spider.newsankaku;

import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:30
 */
public class NewSpiderRunner {
    public void runTask(TaskController task){
        AimType aimType = task.getAimType();
        if(aimType == AimType.ARTIST){
            doArtist(task);
        }else if(aimType == AimType.COPYRIGHTL){

        }else if(aimType == AimType.DOA){

        }
    }
    private void doArtist(TaskController task){
        // 1. 初始化基础 url

        //
    }

}
