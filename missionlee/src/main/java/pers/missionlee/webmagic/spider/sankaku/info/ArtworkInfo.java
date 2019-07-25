package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 13:55
 */
public class ArtworkInfo {
    private static String FILE_PATH = "info/artworkInfo.jsonline";
    private static final String[] emptyStringArray = new String[0];

    // 文件名
    private String name;
    // 格式
    private String format;
    // sankaku 地址 : 展示页地址而非图片地址，因为sankaku加密原因，存储图片地址没有意义
    private String address;
    // 收录时间
    private Long takeTime;

    // 以下来自标签栏
    private List<String> tagArtist;
    private List<String> tagCharacter;
    private List<String> tagCopyright;

    private List<String> tagStudio;
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
    public String toString() {
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
    public boolean equals(Object artworkInfo) {
        return this.getName().equals(((ArtworkInfo) artworkInfo).getName());
    }
    @Deprecated
    private static FileFilter isPicOrVid = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().equals("pic") || pathname.getName().equals("vid");
        }
    };

    /**
     * @Description 清理下载失败的文件：下载过程中文件名称使用 UUID，下载成功转为实际名称
     * 此方法用于删除所有名称为UUID的文件
     * @Return: List 被删除文件列表
     * @Author: Mission Lee
     * @date: 2019/4/8
     */
    @Deprecated
    public static List<String> cleanErrorFiles(File artistFile) {
        List<String> deletedNames = new ArrayList<String>();
        File[] files = artistFile.listFiles(isPicOrVid);
        for (int j = 0; j < files.length; j++) { // pic 与 vid
            if (files[j].isDirectory()) {
                if (files[j].listFiles().length == 0) {
                    // 处理空文件夹（早期代码遗留问题）
                    files[j].delete();
                } else {
                    // 清空小文件
                    File[] pcvi = files[j].listFiles();
                    for (int k = 0; k < pcvi.length; k++) { // 具体文件
                        if (pcvi[k].length() < 10 || pcvi[k].getName().contains("-")) {
                            if (pcvi[k].delete()) {
                                deletedNames.add(pcvi[k].getName());
                            }

                        }
                    }
                }

            }
        }
        return deletedNames;
    }

    public static void convertArtworkStringToList(List<ArtworkInfo> list, String[] artworkInfoLines) {
        for (int i = 0; i < artworkInfoLines.length; i++) {
            if (!StringUtils.isEmpty(artworkInfoLines[i])) {
                ArtworkInfo artworkInfo = JSON.parseObject(artworkInfoLines[i], ArtworkInfo.class);
                if (!list.contains(artworkInfo))
                    list.add(artworkInfo);
            }
        }
    }

    /**
     * @Description: 获取当前已经成功下载的文件列表，获取过程会清理有问题的文件
     * 1. 下载错误的文件
     * 2. 文件记录中存在，但是文件不存在的记录
     * @Param: [artistPath]
     * @return: java.util.List<pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo>
     * @Author: Mission Lee
     * @date: 2019/4/8
     */
    @Deprecated
    public static List<ArtworkInfo> getCleanedArtworkInfoList(String artistPath) throws IOException {

        AutoDeduplicatedArrayList fileInfoJsonList = new AutoDeduplicatedArrayList();
        File artworkInfoFile = new File(artistPath + FILE_PATH);

        if (artworkInfoFile.exists()) {
            // 1. 清理垃圾文件 （小文件 与 临时文件）
            File artistFile = new File(artistPath);
            cleanErrorFiles(artistFile).size();
            // 2. 从文档中获取文件列表
            String artworkInfos = FileUtils.readFileToString(artworkInfoFile, "UTF8");
            String[] artworkInfoLines = artworkInfos.split("\n");
            int jsonLength = artworkInfoLines.length;

            convertArtworkStringToList(fileInfoJsonList, artworkInfoLines);

            // 3.处理文档与实际文件不符合的清空
            File tagFilePic = new File(artistPath + "/pic");
            File tagFileVid = new File(artistPath + "/vid");
            String[] pics = emptyStringArray;
            if (tagFilePic.exists())
                pics = tagFilePic.list();
            String[] vids = emptyStringArray;
            if (tagFileVid.exists())
                vids = tagFileVid.list();


            // fileInfoJsonList.size() != (pics.length + vids.length)
            if (true) {

                List<String> allWeHave = new ArrayList<String>();
                int remove = 0;
                for (int i = 0; i < pics.length; i++) {
                    allWeHave.add(pics[i]);
                }
                for (int i = 0; i < vids.length; i++) {
                    allWeHave.add(vids[i]);
                }
                Iterator iterator = fileInfoJsonList.iterator();
                while (iterator.hasNext()) {
                    if (!allWeHave.contains(((ArtworkInfo) iterator.next()).getName())) {
                        iterator.remove();
                        remove++;
                    }
                }
                if (jsonLength!=fileInfoJsonList.size() || remove>0) {
                    writeArtworkInfoFile(artworkInfoFile, fileInfoJsonList);
                }

            }
        }
        return fileInfoJsonList;
    }
    @Deprecated
    public static void writeArtworkInfoFile(File artworkInfoFile, List<ArtworkInfo> artworkInfos) throws IOException {
        //System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        //System.out.println(artworkInfoFile.getName()+FILE_PATH);
        //File artworkInfoFile = new File(artworkInfoFile.getName() + FILE_PATH);
        Iterator iterator = artworkInfos.iterator();
        FileUtils.writeStringToFile(artworkInfoFile, "", "UTF8", false);
        while (iterator.hasNext()) {
            FileUtils.writeStringToFile(artworkInfoFile, JSON.toJSONString(iterator.next()) + "\n", "UTF8", true);
        }
    }
    @Deprecated
    public static int getArtworkNumber(File artist) {
        int num = 0;
        cleanErrorFiles(artist);
        File[] files = artist.listFiles(isPicOrVid);
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                num += files[i].listFiles().length;
            }
        }
        return num;
    }

    /**
     * 添加一条artworkInfo 到 文件中
     */
    @Deprecated
    public static synchronized void appendArtworkInfo(ArtworkInfo info, String artistPath) throws IOException {
        FileUtils.writeStringToFile(new File(artistPath + FILE_PATH), JSON.toJSONString(info) + "\n", "UTF8", true);
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getCleanedArtworkInfoList("D:\\sankakuoffical\\league of legends\\").size());
    }
}
