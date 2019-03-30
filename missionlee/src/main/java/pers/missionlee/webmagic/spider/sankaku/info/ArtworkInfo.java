package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 13:55
 */
public class ArtworkInfo {
    // 以下内容为爬取时记录
    // 文件名
    private String name;
    // 格式
    private String format;
    // sankaku 地址 : 展示页地址而非图片地址，因为sankaku加密原因，存储图片地址没有意义
    private String address;
    // 收录时间
    private Long takeTime;

    // 以下来自标签栏
    private List<String> tagCopyright;
    private List<String> tagStudio;
    private List<String> tagCharacter;
    private List<String> tagArtist;
    private List<String> tagMedium;
    private List<String> tagGeneral;
    private List<String> tagGenre;
    private List<String> tagMeta;

    public List<String> getTagMeta() {
        return tagMeta;
    }

    public void setTagMeta(List<String> tagMeta) {
        this.tagMeta = tagMeta;
    }

    public List<String> getTagGenre() {
        return tagGenre;
    }

    public void setTagGenre(List<String> tagGenre) {
        this.tagGenre = tagGenre;
    }

    // 以下来自标签栏下方的信息栏
    // 上传到sankaku日志
    private String postDate;
    // 分辨率
    private String resolutionRatio;
    // 文件大小
    private String fileSize;
    // 评级
    private String rating;

    @Override
    public String toString(){
            return JSON.toJSONString(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getTakeTime() {
        return takeTime;
    }

    public void setTakeTime(Long takeTime) {
        this.takeTime = takeTime;
    }

    public List<String> getTagCopyright() {
        return tagCopyright;
    }

    public void setTagCopyright(List<String> tagCopyright) {
        this.tagCopyright = tagCopyright;
    }

    public List<String> getTagStudio() {
        return tagStudio;
    }

    public void setTagStudio(List<String> tagStudio) {
        this.tagStudio = tagStudio;
    }

    public List<String> getTagCharacter() {
        return tagCharacter;
    }

    public void setTagCharacter(List<String> tagCharacter) {
        this.tagCharacter = tagCharacter;
    }

    public List<String> getTagArtist() {
        return tagArtist;
    }

    public void setTagArtist(List<String> tagArtist) {
        this.tagArtist = tagArtist;
    }

    public List<String> getTagMedium() {
        return tagMedium;
    }

    public void setTagMedium(List<String> tagMedium) {
        this.tagMedium = tagMedium;
    }

    public List<String> getTagGeneral() {
        return tagGeneral;
    }

    public void setTagGeneral(List<String> tagGeneral) {
        this.tagGeneral = tagGeneral;
    }

    public String getPostDate() {
        return postDate;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public String getResolutionRatio() {
        return resolutionRatio;
    }

    public void setResolutionRatio(String resolutionRatio) {
        this.resolutionRatio = resolutionRatio;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    @Override
    public boolean equals(Object artworkInfo){
        return this.getName().equals(((ArtworkInfo)artworkInfo).getName());
    }
}
