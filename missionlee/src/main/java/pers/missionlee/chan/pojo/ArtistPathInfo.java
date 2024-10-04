package pers.missionlee.chan.pojo;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtistPathInfo {
    static Logger logger = LoggerFactory.getLogger(ArtistPathInfo.class);
    public String name;
    public Set<String> rel;
    public Set<String> delFileMD5;
    public Set<String> delPool;
    public Set<String> redownloadMD5;
    private static final String pathInfoFileName = "s.json";
    private static final String delFoldereName = "zdel";
    private static final String redownloadFolderName = "zrdo";

    public static ArtistPathInfo refreshInfo(String parentPath) throws IOException {
        logger.warn("IMPORTANT 删除文件夹[" + delFoldereName + "] 重新下载文件夹[" + redownloadFolderName + "]");
        logger.warn("指定文件夹将被清空，相关信息将被重新存储到 [" + pathInfoFileName + "]中");
        /**
         * Step-0 加载本地配置文件，如果没有配置文件，则新建一个
         * */
        logger.info("清理 Step-0 加载配置文件 [" + pathInfoFileName + "]");
        String settingPath = PathUtils.buildPath(parentPath, "s.json");
        File sFile = new File(settingPath);
        ArtistPathInfo info = null;
        if (sFile.exists() && sFile.isFile()) { // 如果已经存在
            String sString = FileUtils.readFileToString(sFile, "UTF8");
            info = JSON.parseObject(sString, ArtistPathInfo.class);
            if (info.rel == null) {
                info.rel = new HashSet<>();
            }
            if (info.delFileMD5 == null) {
                info.delFileMD5 = new HashSet<>();
            }
            if (info.delPool == null) {
                info.delPool = new HashSet<>();
            }
            if (info.redownloadMD5 == null) {
                info.redownloadMD5 = new HashSet<>();
            }
            logger.info("清理 Step-0 加载完成");
        } else { // 如果没有，那么创建一个新的
            info = new ArtistPathInfo();
            info.rel = new HashSet<>();
            info.delFileMD5 = new HashSet<>();
            info.delPool = new HashSet<>();
            info.redownloadMD5 = new HashSet<>();
            logger.info("清理 Step-0 加载完成 *当前为首次加载,系统已初始化数据*");
        }
        /**
         * Step-1  下载失败文件处理
         * Step-1.1 清理重新下载目录，并将信息传保存到配置文件中
         * Step-1.2 寻找普通作品中需要重新下载的作品，删除并处理信息
         * */
        logger.info("清理 Step-1 寻找标准失败页面 大小为 12.6K的画面");
        logger.info("清理 Step-1.1 清理 [" + redownloadFolderName + "] 并记录数据");
        String redownPath = PathUtils.buildPath(parentPath, redownloadFolderName);
        File redo = new File(redownPath);
        if (redo.exists()) {
            logger.info("清理--> 确认重新下载文件 写入重新下载信息");
            delReDoFile(redo, info);
        }
        logger.error("清理 Step-1.2 寻找超时替代文件并处理  不处理超时文件******* 注释掉了");
//        delExpiredFile(new File(parentPath), info);
        /**
         * Step-2 清理要删除的文件
         * */
        logger.info("清理 Step-2 清理要删除的作品并记录数据");
        String zdelPath = PathUtils.buildPath(parentPath, delFoldereName);
        File zdel = new File(zdelPath);
        if (zdel.exists()) {
            logger.info("清理--> 目标文件存在 开始清理工作");
            delFile(zdel, info);
        }
        /**
         * Step-2.puls
         * */
        logger.info("处理掉所有的del路径和内容");
        logger.warn("del 目录手写了两层删除，没有递归，所以可能报错");
        File delFile = new File(PathUtils.buildPath(parentPath,"del"));
        if(delFile.exists()){
            File[] files = delFile.listFiles();
            for (int i = 0; i < files.length; i++) {// del 这个目录最多就两层，所以直接写删除代码了
                if(files[i].isDirectory()){
                    File[] inFile = files[i].listFiles();
                    for (int j = 0; j < inFile.length; j++) {
                        inFile[j].delete();
                    }

                }
                files[i].delete();
            }
            delFile.delete();
        }
        /**
         * Step-3 保存配置文件
         * */
        logger.info("清理 开始写入 [" + pathInfoFileName + "] 标准文件");
        String str = JSON.toJSONString(info);
        FileUtils.writeStringToFile(sFile, str, "UTF8", false);
        logger.info("清理 [完成] 返回数据到上级程序，流程结束");
        /**
         *
         * */

        return info;
    }

    // B[firolian][2171]Mercy_ The First Audition
    // P[firolian][6110840]
    //
    public static String poolIDRegex = "\\]\\[(.*?)\\]";
    //String name1 = "[aoin]Kissing Dicks Association[298708]";
    //String name2 = "P[aoin][3254754]";
    //  poolIDRegexNew 的一点小注意事项,小技巧
    //  因为名字中会出现两组 [] [] 所以 新的正则,会匹配到第一个 [ 和第二个 ] 中间的内容
    //  正则中 匹配了两次 [  就不会出现上面的问题了
    public static String poolIDRegexNew  = "\\[.*\\[(.*)\\]$";

    /**
     * delFile 用于删除指定文件夹，并记录删除的信息
     */
    public static void delFile(File rootDir, ArtistPathInfo info) {
        File[] files = rootDir.listFiles();
        Pattern r1 = Pattern.compile(poolIDRegex);
        Pattern r2 =Pattern.compile(poolIDRegexNew);
        for (int i = 0; i < files.length; i++) {
            File thisFile = files[i];
            if (thisFile.isDirectory()) {
                String fileName = thisFile.getName();
                Matcher m1 = r1.matcher(fileName);
                Matcher m2 = r2.matcher(fileName);
                if(m2.find()){
                    String poolID = m2.group(1);
                    if(poolID.contains("[")){
                        poolID = poolID.substring(poolID.lastIndexOf("[")+1);
                    }
                    logger.warn("删除 -> POOL [" + poolID + "] 递归子文件");
                    info.delPool.add(poolID);
                    delFile(thisFile, info);
                    logger.warn("删除 -> POOL [" + poolID + "] 子文件完成&删除空白目录 " + thisFile.getName());
                    thisFile.delete();
                }else if(m1.find()){
                    String poolID = m1.group(1);
                    logger.warn("删除 -> POOL [" + poolID + "] 递归子文件");
                    info.delPool.add(poolID);
                    delFile(thisFile, info);
                    logger.warn("删除 -> POOL [" + poolID + "] 子文件完成&删除空白目录 " + thisFile.getName());
                    thisFile.delete();
                }
            } else {
                String delMd5 = thisFile.getName().substring(0, thisFile.getName().indexOf("."));
                if (delMd5.contains("_")) {
                    delMd5 = delMd5.substring(delMd5.indexOf("_") + 1);
                }
                logger.warn("删除 -> 文件 [" + delMd5 + "]");
                info.delFileMD5.add(delMd5);
                thisFile.delete();
            }
        }
    }

    /**
     * 专门用来清理 指定文件夹内下载失败的文件
     */
    public static void delReDoFile(File rootDir, ArtistPathInfo info) {
        File[] files = rootDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File thisFile = files[i];
            if (thisFile.isDirectory()) {
                logger.error("清理--> 递归清理");
                delReDoFile(thisFile, info);
                thisFile.delete();
            } else {
                String redelMd5 = thisFile.getName().substring(0, thisFile.getName().indexOf("."));
                if (redelMd5.contains("_")) {
                    redelMd5 = redelMd5.substring(redelMd5.indexOf("_") + 1);
                }
                logger.warn("清理--> 文件 [" + redelMd5 + "]");
                info.redownloadMD5.add(redelMd5);
                thisFile.delete();
            }
        }
    }

    /**
     * 用来找到
     */
    public static void delExpiredFile(File rootDir, ArtistPathInfo info) {
        if (rootDir.exists()) {
            File[] allFiles = rootDir.listFiles();

            for (int i = 0; i < allFiles.length; i++) {
                File thisFile = allFiles[i];
                String fileName = thisFile.getName();
                if (fileName.contains("low")
                        || fileName.contains(pathInfoFileName)
                        || fileName.contains(delFoldereName)
                        || fileName.contains(redownloadFolderName)
                ) {
                    logger.info("清理--> 检测到保留字文件 跳过 " + fileName);
                } else {
                    if (thisFile.isDirectory()) {
                        delExpiredFile(thisFile, info);
                    } else {
                        if (thisFile.length() > 12900 && thisFile.length() < 12950) {
                            logger.info("清理--> 大小符合“过期文件”的目标 " + fileName);
                            String redelMd5 = thisFile.getName().substring(0, thisFile.getName().indexOf("."));
                            if (redelMd5.contains("_")) {
                                redelMd5 = redelMd5.substring(redelMd5.indexOf("_") + 1);
                            }
                            info.redownloadMD5.add(redelMd5);
                            thisFile.delete();

                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        String name1 = "[aoin]Kissing Dicks Association[298708]";
        String name2 = "P[aoin][3254754]";
        String reg = "\\]\\[(.*)\\]";
        String reg2 = "\\[.*\\[(.*)\\]$";
        Pattern r = Pattern.compile(reg2);
        Matcher m = r.matcher(name1);
        if (m.find()) {
            System.out.println("删除POOL: " + m.group(1));
        } else {
            System.out.println("m没匹配到");
        }

//        String s = "Example_(xxxxx)_AND_(yyyyy)_2019-01-28";
//        Pattern p = Pattern.compile("\\][(\\[]([^()\\[\\]]*)[)\\]]");
//        Matcher m = p.matcher(s);
//        while (m.find()) {
//            System.out.println(m.group(1));
//        }
//        try {
//            refreshInfo("G:\\C-A-TOP-厚涂写实\\T-0-C纯爱\\dan-98");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        String x = "xxxxx[yy";
        System.out.println(x.substring(x.lastIndexOf("[")));
    }
}
