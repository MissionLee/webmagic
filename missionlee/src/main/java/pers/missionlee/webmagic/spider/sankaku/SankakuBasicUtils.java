package pers.missionlee.webmagic.spider.sankaku;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-24 22:01
 */
public class SankakuBasicUtils {
    public static String UserName = "zuixue3000@163.com";
    public static String Password = "mingshun1993";

    private static final String chan_sankakucomplex_com_IP = "208.100.27.32";
    private static final String cs_sankakucomplex_com_IP = "208.100.24.254";


    protected static String urlFormater(String urlFragment) {
        // 空格 () ’
        return urlFragment.trim()
                .replaceAll(" ", "_")// !important 这里吧空格对应成了下划线，是sankaku的特别处理方法
                .replaceAll(" ", "%20")
                .replaceAll("!", "%21")
                .replaceAll("\"", "%22")
                .replaceAll("#", "%23")
                .replaceAll("\\$", "%24")
                //.replaceAll("%","%25")
                .replaceAll("&", "%26")
                .replaceAll("'", "%27")
                .replaceAll("\\(", "%28")
                .replaceAll("\\)", "%29")
                .replaceAll("\\*", "%2A")
                .replaceAll("\\+", "%2B")
                .replaceAll(",", "%2C")
                .replaceAll("-", "%2D")
                .replaceAll("\\.", "%2E")
                .replaceAll("/", "%2F")
                .replaceAll(":", "%3A")
                .replaceAll(";", "%3B")
                .replaceAll("<", "%3C")
                .replaceAll("=", "%3D")
                .replaceAll(">", "%3E")
                .replaceAll("\\?", "%3F")
                .replaceAll("@", "%40")
                .replaceAll("\\\\", "%5C")
                .replaceAll("\\|", "%7C");
    }
    protected static Map<String,Integer> sortNameList(Map<String,Integer> nameList){
        return sortNameList(nameList,false);
    }
    protected static Map<String, Integer> sortNameList(Map<String, Integer> namelist,boolean desc) {
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
                if(desc){
                    if (entries[j].getValue() > entries[k].getValue()) {
                        Map.Entry<String, Integer> tmp = entries[j];
                        entries[j] = entries[k];
                        entries[k] = tmp;
                    }
                }else{
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
}
