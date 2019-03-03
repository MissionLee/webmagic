package pers.missionlee.webmagic.spider.sankaku.info;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 14:22
 */
public class SpiderDiray {
    // 入口标签
    public String artistTag;
    // 开始时间
    public long startTime;
    // 完成时间
    public long endTime;
    // 开启线程数
    public int threadNum;
    // 已有文件数量
    public int picNumOld;
    public int vidNumOld;
    // 新增文件数量
    public int picNumAdd;
    public int vidNumAdd;
    // 最终状态文件数量
    public int picNumNow;
    public int vidNumNow;

}
