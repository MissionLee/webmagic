package pers.missionlee.webmagic.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-02-27 19:05
 */
public class URLUtils {
    public static String getParameterFromURL(String URL,String key){
        String[] pairs = URL.split("\\?")[1].split("&");
        Map<String,String> mapPairs = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i++) {
            mapPairs.put(pairs[i].split("=")[0],pairs[i].split("=")[1]);
        }
        return mapPairs.get(key);
    }
}
