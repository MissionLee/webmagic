package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-22 21:58
 */
public class BookParentInfo {
    int id;
    String name;
    String createdAt;
    String updatedAt;
    int artistId;
    List<String> artistName;
    int postCount;
    int visiblePostCount;
    String rating;
    String information;
    ArtworkInfo artworkInfo;

    public ArtworkInfo getArtworkInfo() {
        return artworkInfo;
    }

    public void setArtworkInfo(ArtworkInfo artworkInfo) {
        this.artworkInfo = artworkInfo;
    }

    public List<String> getArtistName() {
        return artistName;
    }

    public void setArtistName(List<String> artistName) {
        this.artistName = artistName;
    }

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }


    public void setInformation(String information) {
        this.information = information;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }


    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public int getVisiblePostCount() {
        return visiblePostCount;
    }

    public void setVisiblePostCount(int visiblePostCount) {
        this.visiblePostCount = visiblePostCount;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }


    public String getInformation() {
        return information;
    }

    public void setInformation(Object information) {
        this.information = JSON.toJSONString(information);
    }
}
