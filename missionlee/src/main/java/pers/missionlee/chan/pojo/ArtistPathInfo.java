package pers.missionlee.chan.pojo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtistPathInfo {
    public String name;
    public Set<String> rel;
    public Set<String> delFileMD5;
    public Set<String> delPool;
    public static ArtistPathInfo refreshInfo(String parentPath) throws IOException {
        // 加载 s.json文件
        String settingPath = PathUtils.buildPath(parentPath, "s.json");
        File sFile = new File(settingPath);
        String zdelPath = PathUtils.buildPath(parentPath,"zdel");
        File zdel = new File(zdelPath);
        ArtistPathInfo info = null;
        if (sFile.exists() && sFile.isFile()) { // 如果已经存在
            System.out.println("现有 s.json 加载");
            String sString = FileUtils.readFileToString(sFile, "UTF8");
            info = JSON.parseObject(sString, ArtistPathInfo.class);
            if(info.rel==null){
                info.rel = new HashSet<>();
            }
            if(info.delFileMD5 == null){
                info.delFileMD5 = new HashSet<>();
            }
            if(info.delPool == null){
                info.delPool = new HashSet<>();
            }
        } else { // 如果没有，那么创建一个新的
            info = new ArtistPathInfo();
            info.rel = new HashSet<>();
            info.delFileMD5 = new HashSet<>();
            info.delPool = new HashSet<>();
        }
        if(zdel.exists()){
            System.out.println("处理 zdel 文件夹");
            delFile(zdel,info);
        }
        String str = JSON.toJSONString(info);
        System.out.println("更新 s.json文件夹");
        FileUtils.writeStringToFile(sFile, str, "UTF8", false);
        return info;
    }
    // B[firolian][2171]Mercy_ The First Audition
    // P[firolian][6110840]
    public static String poolIDRegex = "\\]\\[(.*?)\\]";
    public static void delFile(File rootDir,ArtistPathInfo info){
        File[] files = rootDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File thisFile = files[i];
            if(thisFile.isDirectory()){
                if(thisFile.getName().contains("B[")||thisFile.getName().contains("P[")){
                    String fileName = thisFile.getName();
                    System.out.println("文件夹名称："+fileName);
                    Pattern r = Pattern.compile(poolIDRegex);
                    Matcher m = r.matcher(fileName);
                    if(m.find()){
                        String poolID = m.group(1);
                        System.out.println("删除POOL: "+poolID);
                        info.delPool.add(poolID);
                        System.out.println("发现目录，执行递归删除");
                        delFile(thisFile,info);
                        System.out.println("子文件删除完毕，删除当前目录："+thisFile.getName());
                        thisFile.delete();
                    }
                }

            }else{
                String delMd5 = thisFile.getName().substring(0,thisFile.getName().indexOf("."));
                if(delMd5.contains("_")){
                    delMd5 = delMd5.substring(delMd5.indexOf("_")+1);
                }
                System.out.println("删除："+delMd5);
                info.delFileMD5.add(delMd5);
                thisFile.delete();
            }
        }
    }

    public static void main(String[] args) {
        String reg = "\\]\\[(.*?)\\]";
        Pattern r = Pattern.compile(reg);
        Matcher m = r.matcher("P[firolian][5847114]");
        if(m.find()){
            System.out.println("删除POOL: "+m.group(1));
        }else{
            System.out.println("m没匹配到");
        }

//        String s = "Example_(xxxxx)_AND_(yyyyy)_2019-01-28";
//        Pattern p = Pattern.compile("\\][(\\[]([^()\\[\\]]*)[)\\]]");
//        Matcher m = p.matcher(s);
//        while (m.find()) {
//            System.out.println(m.group(1));
//        }
    }
}
