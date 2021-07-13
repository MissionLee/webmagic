package pers.missionlee.chan.pojo;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-15 20:00
 */
public class BookInfo {
    // 以下几行，是数据库字段
    public int id;
    public String name;
    public String createdAt;
    public String updatedAt;
    public int artistId;
    public int postCount;
    public int visiblePostCount;
    public String rating;
    public String information;
    // 以下几行，是业务字段
    public String aimArtist;
    public List<String> artistName;
    public List<String> copyrights;
    public List<String> characters;
    public List<String> artistTags;
    public String storedArtistName;

    public String getStoredArtistName() {
        return storedArtistName;
    }

    public void setStoredArtistName(String storedArtistName) {
        this.storedArtistName = storedArtistName;
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

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
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

    public void setInformation(String information) {
        this.information = information;
    }

    public String getAimArtist() {
        return aimArtist;
    }

    public void setAimArtist(String aimArtist) {
        this.aimArtist = aimArtist;
    }

    public List<String> getArtistName() {
        return artistName;
    }

    public void setArtistName(List<String> artistName) {
        this.artistName = artistName;
    }

    public List<String> getCopyrights() {
        return copyrights;
    }

    public void setCopyrights(List<String> copyrights) {
        this.copyrights = copyrights;
    }

    public List<String> getCharacters() {
        return characters;
    }

    public void setCharacters(List<String> characters) {
        this.characters = characters;
    }

    public List<String> getArtistTags() {
        return artistTags;
    }

    public void setArtistTags(List<String> artistTags) {
        this.artistTags = artistTags;
    }
}
