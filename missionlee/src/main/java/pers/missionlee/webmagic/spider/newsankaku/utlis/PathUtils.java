package pers.missionlee.webmagic.spider.newsankaku.utlis;

import java.io.File;
import java.io.FileFilter;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-29 17:20
 */
public class PathUtils {
    public static String formatPath(String path) {
        if (!path.endsWith("/"))
            path = path + "/";
        return path;
    }
    public static String[] formatPaths(String[] paths){
        for (int i = 0; i < paths.length; i++) {
            paths[i] = formatPath(paths[i]);
        }
        return paths;
    }
    public static String buildPath(String... paths) {
        String aimPath = "";
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].startsWith("/"))
                paths[i] = paths[i].substring(1);
            aimPath += formatPath(paths[i]);
        }
        return aimPath.replaceAll("\\\\", "/");
    }
    public static FileFilter aimFileFilter(){
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith("pic-")
                        || pathname.getName().startsWith("vid-")
                        || pathname.getName().startsWith("图-")
                        || pathname.getName().startsWith("视-")
                        || pathname.getName().startsWith("T-")
                        || pathname.getName().startsWith("V-")
                        || pathname.getName().startsWith("pic")
                        || pathname.getName().startsWith("vid");
            }
        };
    }
    public static boolean isPicture(String fileName) {
        String nameLow = fileName.toLowerCase();
        if (nameLow.endsWith("jpg")
                || nameLow.endsWith("jpeg")
                || nameLow.endsWith("png")
                || nameLow.endsWith("fig")
                || nameLow.endsWith("webp")
                || nameLow.endsWith("fpx")
                || nameLow.endsWith("svg")
                || nameLow.endsWith("bmp")
        )
            return true;
        return false;
    }

    public static boolean isVideo(String fileName) {
        String nameLow = fileName.toLowerCase();
        if (nameLow.endsWith(".mp4")
                || nameLow.endsWith(".webm")
                || nameLow.endsWith(".avi")
                || nameLow.endsWith(".rmvb")
                || nameLow.endsWith(".flv")
                || nameLow.endsWith(".3gp")
                || nameLow.endsWith(".mov")
                || nameLow.endsWith(".swf")) {
            return true;
        }
        return false;
    }

}
