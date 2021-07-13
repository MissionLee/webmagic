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
    // =================== 以下参数 是数据库用的 =================
    // 文件名  数据库 file_name
    public String fileName;
    //
    public String sanCode;
    // 文件大小
    public String fileSize;
    public String format;
    public String postDate;
    // 评级
    public String rating;
    // 分辨率
    public String resolutionRatio;
    // status 通过默认值存储
    // 相对路径  现在没啥用了 这个
    public String relativePath;
    // create_time 自动生成
    // update_time 自动生成
    // information 通过 JSON.toString 整个类生成
    // 格式  show_type    img video object/embed 等等
    // official
    public boolean official=false;
    public int bookId=-1;
    public int parentId=-1;

    public boolean isSingle = false;
    // 存储位置
    public int storePlace;
    // ==================== 以下参数 是  程序用的
    // 目标作者名称 （作品可能是多作者，但是下载的时候，可能是为了下载某个单一作者的作品）
    public String aimName;
    public int artistType;
    public String bookName;
    public String fileSaveName;
    public String PBPrefix;
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    // 格式
    // sankaku 地址 : 展示页地址而非图片地址，因为sankaku加密原因，存储图片地址没有意义
    private String address;
    // 收录时间
    private Long takeTime;
    public String getAimName() {
        return aimName;
    }

    public void setAimName(String aimName) {
        this.aimName = aimName;
    }
    public enum ARTIST_TYPE{
        // '1 普通作者 2 copyright 3 studio 4 组合条件'
        ARTIST(1),
        COPYRIGHT(2),
        STUDIO(3),
        COMBINED(4),
        UNKNOWN(9)
        ;

        ARTIST_TYPE(int artistType) {
            this.artistType = artistType;
        }
        public int artistType;
    }
    public static enum  STORE_PLACE{
        //0 未确认 1 艺术家名下， 2 copyright 名下 3 studio 名下 4 single名下 5 ARTIST-parent/book 名下 6 single-book/parent名下 99 其他
        NOT_KNOWN(0),
        ARTIST(1),
        COPYRIGHT(2),
        STUDIO(3),
        SINGLE(4),
        ARTIST_PARENT_BOOK(5),
        SINGLE_PARENT_BOOK(6);


        STORE_PLACE(int storePlace) {
            this.storePlace = storePlace;
        }
        public int storePlace;
    }
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



    public static ArtworkInfo getArtistPicPathInfo(String artistName){
        ArtworkInfo artworkInfo = new ArtworkInfo();
        artworkInfo.storePlace = ArtworkInfo.STORE_PLACE.ARTIST.storePlace;
        artworkInfo.aimName = artistName;
        artworkInfo.fileName = "1.jpg";
        return  artworkInfo;
    }
    public static ArtworkInfo getArtistVidPathInfo(String artistName){
        ArtworkInfo artworkInfo = getArtistPicPathInfo(artistName);
        artworkInfo.fileName = "1.mp4";
        return artworkInfo;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
        return this.getFileName().equals(((ArtworkInfo) artworkInfo).getFileName());
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
                    if (!allWeHave.contains(((ArtworkInfo) iterator.next()).getFileName())) {
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
