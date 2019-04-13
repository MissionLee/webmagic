package pers.missionlee.webmagic.spider.update;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.sankaku.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.AutoDeduplicatedArrayList;
import pers.missionlee.webmagic.utils.ChromeBookmarksReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-04-12 14:56
 *
 *
 */
public class SpiderManager {

   public static void autoRun(String rootPath,String artistName) throws IOException {
      SourceManager sourceManager = new SourceManager(rootPath);
      Set<String> downloadedArtist = sourceManager.getSankakuArtists().keySet();
      System.out.println(downloadedArtist.size());
      ChromeBookmarksReader reader = new ChromeBookmarksReader("C:\\Documents and Settings\\Administrator\\Local Settings\\Application Data\\Google\\Chrome\\User Data\\Default\\Bookmarks");
      List<Map> chromeList =  reader.getBookMarkListByDirName("DownloadedBySpider");
      System.out.println(chromeList.size());
      List<String> needDownload = new AutoDeduplicatedArrayList<String>();
      StringBuffer buffer = new StringBuffer();
      for (Map bookmark :
              chromeList) {
         String tmpName = SpiderUtils.urlDeFormater(bookmark.get("url").toString().split("tags=")[1]);
         if(!downloadedArtist.contains(tmpName)){
            needDownload.add(tmpName);
            System.out.println("需要下载："+tmpName);
            buffer.append(tmpName+" 1\n");
         }
      }
      FileUtils.writeStringToFile(new File("C:\\Users\\Administrator\\Desktop\\ttt\\name.md"),buffer.toString(),"utf8",false);
//      SpiderTaskFactory factory = new SpiderTaskFactory(sourceManager);
//      SpiderTask task = factory.getSpiderTask(SourceManager.SourceType.SANKAKU,artistName,false, SpiderTask.TaskType.NEW);
//      SourceSpiderRunner runner = new SourceSpiderRunner();
//      runner.runTask(task);
   }
   public static void runWithNameList(String filePath) throws IOException {
      File nameListFile = new File(filePath);
      String nameListString = FileUtils.readFileToString(nameListFile, "UTF8");
      String[] nameListArray = nameListString.split("\n");
      int length = nameListArray.length;

      // 2. 获取排序后的 name list
      Map<String, Integer> nameListMap = new LinkedHashMap<String, Integer>();
      for (int i = 0; i < length; i++) {
         String str = nameListArray[i].trim();
         if (!StringUtils.isEmpty(str))
            if (!str.contains("run")) {
               while (str.startsWith("//"))
                  str = str.substring(2).trim();
               int lastIndex = str.lastIndexOf(" ");
               if (lastIndex != -1) {
                  String name = str.substring(0, str.lastIndexOf(" ")).trim();
                  String num = str.substring(str.lastIndexOf(" ")).trim().replaceAll("\\(", "").replaceAll("\\)", "").replaceAll(",", "");
                  if (Pattern.matches("\\d+", num)) {
                     nameListMap.put(name, Integer.valueOf(num));
                  }
               }
            }
      }
      Map<String, Integer> sortedMap = sortNameList(nameListMap, false);
      // 2+ 排序后的列表写回目录
      rewriteTodoList(nameListFile, sortedMap);
      Map<String, Integer> storageMap = new LinkedHashMap<String, Integer>(sortedMap);
      Set<String> set = sortedMap.keySet();
      Iterator iterator = set.iterator();
      while (iterator.hasNext()) {
         String key = (String) iterator.next();
         //task.resetDownloadTask(key);
         // 3 遍历下载
         autoRun("D:\\ROOT",key);
         // 原本会检测下载数量是否不足，但是update机制完善之后，可以通过update方式来补充下载缺漏
         storageMap.remove(key);
         rewriteTodoList(nameListFile, storageMap);
      }
   }
   protected static Map<String, Integer> sortNameList(Map<String, Integer> namelist, boolean desc) {
      Set<Map.Entry<String, Integer>> valueSet = namelist.entrySet();
      Map.Entry<String, Integer>[] entries = new Map.Entry[namelist.size()];
      Iterator iterator = valueSet.iterator();
      int i = 0;
      while (iterator.hasNext()) {
         entries[i++] = (Map.Entry<String, Integer>) iterator.next();
      }
      int length = namelist.size();
      for (int j = 0; j < length; j++) {
         for (int k = 0; k < length; k++) {
            if (desc) {
               if (entries[j].getValue() > entries[k].getValue()) {
                  Map.Entry<String, Integer> tmp = entries[j];
                  entries[j] = entries[k];
                  entries[k] = tmp;
               }
            } else {
               if (entries[j].getValue() < entries[k].getValue()) {
                  Map.Entry<String, Integer> tmp = entries[j];
                  entries[j] = entries[k];
                  entries[k] = tmp;
               }
            }

         }
      }
      Map<String, Integer> aimMap = new LinkedHashMap<String, Integer>();
      for (int j = 0; j < entries.length; j++) {
         aimMap.put(entries[j].getKey(), entries[j].getValue());
      }
      return aimMap;
   }
   protected static void rewriteTodoList(File file, Map<String, Integer> info) {
      try {
         StringBuffer stringBuffer = new StringBuffer();
         Set<String> set = info.keySet();
         for (String key :
                 set) {
            stringBuffer.append(key + " " + info.get(key) + "\n");
         }
         FileUtils.writeStringToFile(file, stringBuffer.toString(), "UTF8", false);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   public static void main(String[] args) throws IOException {
      //runWithNameList("D:\\sankaku\\name.md");
      autoRun("D:\\ROOT","");
   }
}
