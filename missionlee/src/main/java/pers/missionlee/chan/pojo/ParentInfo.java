package pers.missionlee.chan.pojo;

import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-23 22:01
 */
public class ParentInfo {
    public String id;
    public int poolNum;
    public int storedNum = 0;
    public int status;
    public boolean single;
    public String storedArtist;
    public int storedArtistId;
    public String information;
    public ArtworkInfo parentArtworkInfo;
}
