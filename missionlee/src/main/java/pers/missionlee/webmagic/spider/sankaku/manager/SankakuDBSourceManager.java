package pers.missionlee.webmagic.spider.sankaku.manager;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-25 21:29
 */
public class SankakuDBSourceManager {
    static String resource = "mybatis/mybatis-config.xml";
    static SqlSession sqlSession;
    static Map<String,Map<String, Object>> artists;
    Map<String, Integer> simpleArtist;

    static {
        try {
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            sqlSession = sqlSessionFactory.openSession();
            List<Map<String,Object>> tmpartists = sqlSession.selectList("san.getAllTargetArtist");
            artists = new HashMap<>();
            for (Map m :
                    tmpartists) {
                System.out.println(m.get("name"));
                artists.put(m.get("name").toString(),m);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取作者列表
     * */
    public Map<String, Integer> getArtists() {
        if (simpleArtist == null) {
            Set<String> names = artists.keySet();
            for (String name :
                    names) {
                simpleArtist.put(name,1);
            }
        }
        return simpleArtist;
    }
    /***
     * 判断是否需要更新
     */
    public boolean needUpdate(String name,Integer minPriority){
        Integer tmin = Math.min((Integer)artists.get(name).get("picLevel"),(Integer)artists.get(name).get("vidLevel"));
        // 优先级不够 表示 不需要更新
        if(tmin > minPriority)
            return false;
        // 优先级够了 如果没有目标更新时间 就更新
        if(artists.get(name).get("aimUpdateTime") == null)
            return true;
        if((Long)artists.get(name).get("aimUpdateTime")*1000<System.currentTimeMillis())
            return true;
        return false;
    }

    public static void main(String[] args) {
        SankakuDBSourceManager sankakuDBSourceManager = new SankakuDBSourceManager();
        sankakuDBSourceManager.needUpdate("starshadowmagician",0);

    }
}
