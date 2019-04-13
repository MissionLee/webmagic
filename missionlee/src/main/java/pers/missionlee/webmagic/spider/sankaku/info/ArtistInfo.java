package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 13:40
 */
public class ArtistInfo {
    private static final List<String> picFormats = new ArrayList<String>() {{
        add("jpg");
        add("png");
        add("jpeg");
        add("webp");
        add("gif");
    }};
    private static String ARTIST_INFO_FILE_NAME = "info/artistInfo.json";
    private String name;
    private long updateTime;
    private int artworkNum;
    private Map<String, Integer> hotTags;
    private List<String> safePics;
    private List<String> questionablePics;
    private List<String> explicitPics;

    private List<String> safeVids;
    private List<String> questionableVids;
    private List<String> explicitVids;

    public ArtistInfo() {
        this.hotTags = new HashMap<String, Integer>();
        this.safePics = new ArrayList<String>();
        this.questionablePics = new ArrayList<String>();
        this.explicitPics = new ArrayList<String>();
        this.safeVids = new ArrayList<String>();
        this.questionableVids = new ArrayList<String>();
        this.explicitVids = new ArrayList<String>();
    }

    public Map<String, Integer> getHotTags() {
        return hotTags;
    }

    public void setHotTags(Map<String, Integer> hotTags) {
        this.hotTags = hotTags;
    }

    public List<String> getSafePics() {
        return safePics;
    }

    public void setSafePics(List<String> safePics) {
        this.safePics = safePics;
    }

    public List<String> getQuestionablePics() {
        return questionablePics;
    }

    public void setQuestionablePics(List<String> questionablePics) {
        this.questionablePics = questionablePics;
    }

    public List<String> getExplicitPics() {
        return explicitPics;
    }

    public void setExplicitPics(List<String> explicitPics) {
        this.explicitPics = explicitPics;
    }

    public List<String> getSafeVids() {
        return safeVids;
    }

    public void setSafeVids(List<String> safeVids) {
        this.safeVids = safeVids;
    }

    public List<String> getQuestionableVids() {
        return questionableVids;
    }

    public void setQuestionableVids(List<String> questionableVids) {
        this.questionableVids = questionableVids;
    }

    public List<String> getExplicitVids() {
        return explicitVids;
    }

    public void setExplicitVids(List<String> explicitVids) {
        this.explicitVids = explicitVids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public int getArtworkNum() {
        return artworkNum;
    }

    public void setArtworkNum(int artworkNum) {
        this.artworkNum = artworkNum;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public static ArtistInfo getArtistInfoFromArtwork(List<ArtworkInfo> artworkInfos, String name) {
        ArtistInfo artistInfo = new ArtistInfo();
        artistInfo.setArtworkNum((artworkInfos.size()));
        artistInfo.setName(name);
        artistInfo.setUpdateTime(System.currentTimeMillis());
        Map<String, Integer> tagCounts = new HashMap<String, Integer>();
        for (ArtworkInfo info : artworkInfos
        ) {
            List<String> generalTags = info.getTagGeneral();
            for (String tag :
                    generalTags) {
                if (tagCounts.containsKey(tag)) {
                    tagCounts.put(tag, tagCounts.get(tag) + 1);
                } else {
                    tagCounts.put(tag, 1);
                }
            }
            if (picFormats.contains(info.getFormat())) {

                if (info.getRating().equals("Questionable")) {
                    artistInfo.getQuestionablePics().add(info.getName());
                } else if (info.getRating().equals("Explicit")) {
                    artistInfo.getExplicitPics().add(info.getName());
                } else {
                    artistInfo.getSafePics().add(info.getName());
                }
            } else {
                if (info.getRating().equals("Questionable")) {
                    artistInfo.getQuestionableVids().add(info.getName());
                } else if (info.getRating().equals("Explicit")) {
                    artistInfo.getExplicitVids().add(info.getName());
                } else {
                    artistInfo.getSafeVids().add(info.getName());
                }
            }
        }
        return artistInfo;
    }

    public static ArtistInfo updateArtworkInfo(List<ArtworkInfo> artworkInfos, String artistPath, String artistName) throws IOException {
        ArtistInfo artistInfo = getArtistInfoFromArtwork(artworkInfos, artistName);
        FileUtils.writeStringToFile(new File(artistPath + ARTIST_INFO_FILE_NAME), artistInfo.toString(), "UTF8", false);
        return artistInfo;
    }
}
