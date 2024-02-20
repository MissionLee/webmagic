package pers.missionlee.chan.util;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public  class UseWebDriver {
    static final String PATTERN_1 = "";//作者首页
    static final String PATTERN_2 = "";
    static final String PATTERN_3 = "";
    static final String PATTERN_4 = "";
    static final String PATTERN_5 = "";
    static final Set<String> SANKAKU_WEB_DRIVER_PATTERN = new HashSet<>();
    static{
        SANKAKU_WEB_DRIVER_PATTERN.add(PATTERN_1);
    }

    public static boolean sankakuComplexUseWebDriver(String url){
//        for (String pattern : SANKAKU_WEB_DRIVER_PATTERN) {
//
//        }
        if(StringUtils.isEmpty(url)){
            return false;
        }
        if(url.contains("page")){
            return true;
        }else{
            return false;
        }
     }

    public static void main(String[] args) {

    }
}
