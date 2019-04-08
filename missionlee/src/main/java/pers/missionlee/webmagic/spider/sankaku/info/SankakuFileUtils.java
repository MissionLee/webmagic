package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-02 16:24
 */
public class SankakuFileUtils {
    private static Logger logger = LoggerFactory.getLogger(SankakuFileUtils.class);
    /**
     * 两个工具方法
     * */
    private static String formatPath(String rootPath) {
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        return rootPath;
    }
    private static String formatArtistPath(String rootPath,String artistName){
        return formatPath(formatPath(rootPath)+artistName);
    }
    public static String buildPath(String... paths){
        String aimPath = "";
        for (int i = 0; i < paths.length; i++) {
            if(paths[i].startsWith("/"))
                paths[i]=paths[i].substring(1);
            aimPath+=formatPath(paths[i]);
        }
        return aimPath;
    }
    /**
     * 创建作者目录
     **/
    public static boolean makeArtistDir(String rootPath,String artistName){
        String artistPath = formatArtistPath(rootPath,artistName);
        File artist = new File(artistPath);
        if(!artist.exists()){
            return artist.mkdir();
        }
        return true;
    }
    /**
     * ArtworkInfo 相关内容
     */
    // 获取 作品信息列表
    public static List<ArtworkInfo> getArtworkInfoList(String rootPath,String artistName) throws IOException {
        String artistPath = formatArtistPath(rootPath,artistName);
        return ArtworkInfo.getCleanedArtworkInfoList(artistPath);
    }
    // 新增一条作品信息
    public static synchronized void appendArtworkInfo(ArtworkInfo info, String rootPath,String artistName) throws IOException {
        String artistPath = formatArtistPath(rootPath,artistName);
        ArtworkInfo.appendArtworkInfo(info,artistPath);
    }
    // 获取作品数量
    public static int getArtworkNumber(File artist) {
        return ArtworkInfo.getArtworkNumber(artist);
    }
    // 删除错误文件
    public static int cleanErrorFilesForArtist(String rootPath,String artistName) {
        String artistPath = formatArtistPath(rootPath,artistName);
        File artist = new File(artistPath);
        return cleanErrorFilesForArtist(artist);
    }
    public static int cleanErrorFilesForArtist(File artistFile){
        List<String> deleted = ArtworkInfo.cleanErrorFiles(artistFile);
        logger.info("DELETE: " + JSON.toJSONString(deleted));
        return deleted.size();
    }
    /**
     * ArtistInfo 相关内容
     * */
    // 提供作品信息，重构作者信息
    public static ArtistInfo freshArtistInfo(List<ArtworkInfo> artworkInfos,String rootPath,  String artistName) {
        String artistPath= formatArtistPath(rootPath,artistName);
        try {
            ArtistInfo artistInfo = ArtistInfo.updateArtworkInfo(artworkInfos, artistPath, artistName);
            return artistInfo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    @Deprecated()
    public static int deleteEmptyFilesTag(String rootPath, String tag) {
        return cleanErrorFilesForArtist(rootPath,tag);
    }

    /**
     * @Description: 下载出错的内容可能是个空文件，这里可以把空文件删除
     * @Param: [rootPath]
     * @return: int
     * @Author: Mission Lee
     * @date: 2019/3/4
     */
    public static int deleteEmptyFilesRoot(String rootPath) {
        int deleted = 0;
        File root = new File(rootPath);
        File[] tags = root.listFiles();
        for (int i = 0; i < tags.length; i++) {// tag 级别
            deleted += cleanErrorFilesForArtist(tags[i]);
        }
        return deleted;
    }

    /**
     * @Description:
     * @Param: [tag]
     * @return: int 被删除的文件数量
     * @Author: Mission Lee
     * @date: 2019/3/4
     */



    private static FilenameFilter sample = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("sample");
        }
    };
    /**
     * 指定根目录，为所有没有sample图片的作品集选sample图片
     * */
    public static void extractSamplePic(File rootFile) throws IOException {
        File[] files = rootFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {

                File[] theFile = files[i].listFiles(sample);
                System.out.println(files[i].getPath());
                File pic = new File(files[i].getPath() + "/pic");
                if (theFile.length == 0 && pic.exists()) {
                    cleanErrorFilesForArtist(files[i]);
                    File[] pics = pic.listFiles();
                    if (pics!=null && pics.length > 0) {
                        File srcFile = pics[0];
                        String subFix = "." + srcFile.getName().split("\\.")[1];
                        File destFile = new File(files[i].getPath() + "/sample" + subFix);
                        FileUtils.copyFile(srcFile, destFile);
                    } else {
                        pic.delete();
                    }
                }

            }
        }
    }




    private static FileFilter infoFileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile()&&(pathname.getName().equals("artistInfo.json")||pathname.getName().equals("artworkInfo.jsonline"));
        }
    };
    /**
     * 用于把作者目录下的两个信息文件转移到 作者目录下的 info文件夹下面
     *
     * 处理历史遗留问题
     * */
    public static void removeInfoFiles(String rootPath) throws IOException {
        rootPath = formatPath(rootPath);
        File root = new File(rootPath);
        File[] files = root.listFiles();
        for (int i = 0; i < files.length; i++) {
            File artistFile = files[i];
            File artistInfoFile = new File(artistFile.getPath()+"/info");
            if(artistFile.isDirectory()){
                File[] aimFiles = artistFile.listFiles(infoFileFilter);
                for (int j = 0; j < aimFiles.length; j++) {
                    FileUtils.moveFileToDirectory(aimFiles[j],artistInfoFile,true);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
//        extractSamplePic(new File("D:\\sankaku"));
//        removeInfoFiles("D:\\sankaku");
        System.out.println(buildPath("D:/abc","de/","fg","hij"));
    }
}
