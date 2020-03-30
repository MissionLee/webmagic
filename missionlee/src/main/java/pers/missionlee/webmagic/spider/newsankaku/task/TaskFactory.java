package pers.missionlee.webmagic.spider.newsankaku.task;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:24
 */
public class TaskFactory {
    public static Task getArtistTask(){
        return new ArtistTask();
    }
}
