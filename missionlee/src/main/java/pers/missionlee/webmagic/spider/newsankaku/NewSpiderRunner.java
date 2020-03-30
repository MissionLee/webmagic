package pers.missionlee.webmagic.spider.newsankaku;

import pers.missionlee.webmagic.spider.newsankaku.task.Task;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:30
 */
public class NewSpiderRunner {
    public void runTask(Task task){
        AimType aimType = task.getAimType();
        if(aimType == AimType.ARTIST){
            doArtist(task);
        }else if(aimType == AimType.COPYRIGHTL){

        }else if(aimType == AimType.DOA){

        }
    }
    private void doArtist(Task task){
        // 1. 初始化基础 url

        //
    }

}
