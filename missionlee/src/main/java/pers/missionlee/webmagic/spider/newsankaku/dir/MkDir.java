package pers.missionlee.webmagic.spider.newsankaku.dir;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-06-13 21:43
 */
public class MkDir {
    public static void main(String[] args) {
        String prefix = "\\T";
        int maxLevel = 5;
        String root = "G:\\ROOT\\sankaku_level";
        List<String> type = new ArrayList<String>(){{
            add("A文艺");
            add("B软色");
            add("C纯爱");
            add("D重口");
            add("E暗黑");
            add("F性癖");
            add("G猎奇");
        }};
        for (int i = 0; i < maxLevel; i++) {

            StringBuffer buffer = new StringBuffer();
            String fix= buffer.append(prefix).append("-").append(i).append("-").toString();
            for (String sub :
                    type) {
                new File(root+fix+sub).mkdir();
            }
        }
    }
}
