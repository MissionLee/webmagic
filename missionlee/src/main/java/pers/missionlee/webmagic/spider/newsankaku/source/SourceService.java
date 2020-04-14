package pers.missionlee.webmagic.spider.newsankaku.source;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.dbbasedsankaku.SankakuDBSourceManager;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-29 18:17
 */
public class SourceService {

    Logger logger = LoggerFactory.getLogger(SankakuDBSourceManager.class);
    private static Long expire = 1000 * 60 * 30L;


    // 用于缓存 name
    public static ConcurrentHashMap<String, BigInteger> artistMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Long> artistMapExpire = new ConcurrentHashMap<>();
    // 缓存 copyright
    public static ConcurrentHashMap<String, BigInteger> characterMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Long> characterMapExpire = new ConcurrentHashMap<>();
    // studio
    public static ConcurrentHashMap<String, BigInteger> copyrightMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Long> copyrightMapExpire = new ConcurrentHashMap<>();

    static String resource = "mybatis/mybatis-config.xml";
    static SqlSession sqlSession;
    static Map<String, Map<String, Object>> artistFullInfo;
    static Map<String, Integer> artistLevel = new HashMap<>();
    static Map<String, Integer> artistNeedUpdate = new HashMap<>();
    Map<String, Integer> simpleArtist;

    static {
        try {
            // 解析MyBatis配置文件
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            sqlSession = sqlSessionFactory.openSession(true);
            List<Map<String, Object>> tmpartists = sqlSession.selectList("san.getAllTargetArtist");
            artistFullInfo = new HashMap<>();
            long nowTimeStamp = System.currentTimeMillis()/1000;
            for (Map m :
                    tmpartists) {
                artistFullInfo.put(m.get("name").toString(), m);
                int picLevel = 10;
                if (m.containsKey("picLevel")&&!StringUtils.isEmpty(m.get("picLevel").toString()))
                    picLevel = Integer.parseInt(m.get("picLevel").toString());
                int vidLevel = 10;
                if (m.containsKey("vidLevel")&&!StringUtils.isEmpty(m.get("vidLevel").toString()))
                    vidLevel = Integer.parseInt(m.get("vidLevel").toString());
                String name = m.get("name").toString();
                Integer minLevel = Math.min(picLevel, vidLevel);
                long updateTime = null == m.get("aimUpdateTime") ? 0 : (long) m.get("aimUpdateTime");
                artistLevel.put(name, minLevel);
                if (updateTime < nowTimeStamp) {
                    artistNeedUpdate.put(name, minLevel);
                }
            }
            System.out.println("数据库-当前总收录作者：" + artistFullInfo.size());
            System.out.println("数据库-需要更新的作者：" + artistNeedUpdate.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public SourceService(){}

    public List<String> getArtists(boolean onlyNeedUpdate, int minLevel) {
        List<String> aimList = new ArrayList<>();
        Map aimMap = null;
        if (onlyNeedUpdate) {
            aimMap = artistNeedUpdate;
        } else {
            aimMap = artistFullInfo;

        }
        Iterator<String> iterator = aimMap.keySet().iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            int level = artistLevel.get(name);
//            System.out.println(name+" "+level);

            if (level <= minLevel) {
                aimList.add(name);
            }
        }
        return aimList;
    }
    public int getArtistWorkNum(String artworkName){
        return sqlSession.selectOne("san.getArtistWorkNum",artworkName);
    }
    /**
     * 获取作者列表
     */
    public Map<String, Integer> getArtists() {
        if (simpleArtist == null) {
            simpleArtist = new HashMap<>();
            Set<String> names = artistFullInfo.keySet();
            for (String name :
                    names) {
                simpleArtist.put(name, 1);
            }
        }
        return simpleArtist;
    }
    public List<Map<String,Object>> getArtistsOfArtworkSanCode(String sanCode){
        return sqlSession.selectList("san.getArtistListBySanCode",sanCode);
    }
    public Map<String, Object> getArtistInfo(String artistName) {
        return artistFullInfo.get(artistName);
    }

    /***
     * 判断是否需要更新
     */
    public boolean needUpdate(String name, Integer minPriority) {
        Integer tmin = Math.min((Integer) artistFullInfo.get(name).get("picLevel"), (Integer) artistFullInfo.get(name).get("vidLevel"));
        // 优先级不够 表示 不需要更新
        if (tmin > minPriority)
            return false;
        // 优先级够了 如果没有目标更新时间 就更新
        if (artistFullInfo.get(name).get("aimUpdateTime") == null)
            return true;
        if ((Long) artistFullInfo.get(name).get("aimUpdateTime") * 1000 < System.currentTimeMillis())
            return true;
        return false;
    }
    /**
     * 记录作者作品被更新的时间，保证作者 is_target  = 1
     * */
    public boolean touchArtist(String name){
        Map<String,Object> params = new HashMap<>();
        params.put("theName",name);
        params.put("time",System.currentTimeMillis()/1000);
        int updated = sqlSession.update("san.refreshUpdateTime",params);
        return updated == 1;

    }

    public void confirmArtworkArtistRel(String fullUrl,String artistName){
        String sanCode = fullUrl.substring(fullUrl.lastIndexOf("/")+1);
        Map<String,Object> map = new HashMap<>();
        map.put("sanCode",sanCode);
        map.put("name",artistName);
        List<Map<String,Object>> list = sqlSession.selectList("san.confirmArtworkOfSomeone",map);
        if(list != null
                && list.size()==1

        ){
            Map<String,Object> map1 = list.get(0);
            if(map1.containsKey("sanCode") && sanCode.equals(map.get("sanCode"))){
                System.out.println("作者："+artistName +"  与作品 "+sanCode+" 关系正常");
            }else{
                System.out.println("作者："+artistName +"  与作品 "+sanCode+" 未建立关系[后续自动补充关系]");
                map1.put("sanCode",sanCode);
                map1.put("artist_id",map1.get("artistId"));
                sqlSession.insert("san.addArtistRel",map1);
            }
        }else{
            System.out.println("验证作者作品关系时，未找到指定作者");
        }
    }
    public void addArtworkInfo(ArtworkInfo ar) {
        try {


            Long createTime = ar.getTakeTime();
            Map<String, Object> artworkInfoMap = new HashMap<>();
            artworkInfoMap.put("sanCode", ar.sanCode);
            artworkInfoMap.put("relativePath", ar.relativePath);
            artworkInfoMap.put("fileName", ar.getName());
            artworkInfoMap.put("fileSize", ar.getFileSize());
            artworkInfoMap.put("postDate", ar.getPostDate());
            artworkInfoMap.put("rating", ar.getRating());
            artworkInfoMap.put("resolutionRatio", ar.getResolutionRatio());
            artworkInfoMap.put("status", 1);
            artworkInfoMap.put("fileFormat", ar.getFormat());
            artworkInfoMap.put("createTime", createTime);
            artworkInfoMap.put("information", ar.toString());
            Boolean stored = false;
            try {
                // todo 2.1 作品信息写入数据库
                sqlSession.insert("san.addArtwork", artworkInfoMap);
                sqlSession.commit();
            } catch (Exception e) {
                logger.info("已有作品");
                stored = true;
            }
            if (true) { // 如果信息没有存储过： 可以相信只要存储过一次，所有的信息存储都是玩完整的
                //  ！！！ 事实证明 作品被存储过 也不一定 信息完整
                // 作者 ----------
                Map<String, Object> params = new HashMap<>();
                params.put("sanCode", ar.sanCode);
                logger.info("添加作者关系" + ar.getTagArtist());
                for (String artistName : ar.getTagArtist()) {
                    BigInteger id;
                    // 查询 名称对应的作者id
                    if (artistMap.containsKey(artistName)) {// 如果缓存了 作者名字，直接用缓存的
                        id = artistMap.get(artistName);
                        params.put("artist_id", id);
                    } else { //尝试从数据库获取id

                        id = sqlSession.selectOne("san.getArtistId", artistName);
                        if (id == null) { // 如果id 不存在 添加临时作者  这段sql 会把id 放到 params里面
                            params.put("name", artistName);
                            sqlSession.insert("san.insertArtistOnlyName", params);
                            sqlSession.commit();
                            artistMap.put(artistName, (BigInteger) params.get("artist_id"));
                            System.out.println("缓存作者：" + artistName + "  " + params.get("artist_id"));
                        } else {
                            params.put("artist_id", id);
                            //如果有这个id 缓存以下
                            artistMap.put(artistName, id);
                            System.out.println("缓存作者：" + artistName + "  " + id);

                        }
                        artistMapExpire.put(artistName, System.currentTimeMillis());

                    }
                    try {
                        sqlSession.insert("san.addArtistRel", params);
                        sqlSession.commit();
                    } catch (Exception e) {
                        logger.warn("作者关系已经存在 " + e.getMessage());
                    }
                }
                logger.info("添加角色关系" + ar.getTagCharacter());
                // character 角色
                for (String character : ar.getTagCharacter()) {
                    BigInteger cid;
                    if (characterMap.containsKey(character)) {
                        cid = characterMap.get(character);
                        params.put("character_id", cid);
                    } else {
                        cid = sqlSession.selectOne("san.getCharacterId", character);
                        if (cid == null) {
                            params.put("name", character);
                            sqlSession.insert("san.insertCharacter", params);
                            sqlSession.commit();
                            characterMap.put(character, (BigInteger) params.get("character_id"));
                        } else {
                            params.put("character_id", cid);
                            characterMap.put(character, cid);
                        }
                        logger.info("缓存角色：" + character);
                        characterMapExpire.put(character, System.currentTimeMillis());

                    }
                    try {
                        sqlSession.insert("san.addCharacterRel", params);
                        sqlSession.commit();
                    } catch (Exception e) {
                        logger.info("角色关系已经存在 " + e.getMessage());
                    }

                }


                // 版权
                logger.info("添加版权关系" + ar.getTagCopyright());
                for (String copyright : ar.getTagCopyright()) {
                    BigInteger cid;
                    if (copyrightMap.containsKey(copyright)) {
                        cid = copyrightMap.get(copyright);
                        params.put("copyright_id", cid);
                    } else {
                        cid = sqlSession.selectOne("san.getCopyrightId", copyright);

                        if (cid == null) {
                            // System.out.println("CopyRight : " + copyright);
                            params.put("name", copyright);
                            sqlSession.insert("san.insertCopyright", params);
                            sqlSession.commit();
                            copyrightMap.put(copyright, (BigInteger) params.get("copyright_id"));
                        } else {
                            params.put("copyright_id", cid);
                            copyrightMap.put(copyright, cid);
                        }
                        logger.info("缓存版权" + copyright);
                        copyrightMapExpire.put(copyright, System.currentTimeMillis());
                    }
                    try {
                        sqlSession.insert("san.addCopyrightRel", params);
                        sqlSession.commit();
                    } catch (Exception e) {
                        logger.warn("角色关系已经存在 " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            // 不管怎样 latch.countDown 一定要执行
//            latch.countDown();
//            logger.info(Thread.currentThread() + " | " + latch.getCount());
            Long cur = System.currentTimeMillis();
//            if (latch.getCount() == 1) {
            artistMapExpire.entrySet().removeIf(timeExpired);
            artistMap.entrySet().removeIf(entry -> (!artistMapExpire.containsKey(entry.getKey())));
            characterMapExpire.entrySet().removeIf(timeExpired);
            characterMap.entrySet().removeIf(entry -> !characterMapExpire.containsKey(entry.getKey()));
            copyrightMapExpire.entrySet().removeIf(timeExpired);
            copyrightMap.entrySet().removeIf(entry -> (!copyrightMapExpire.containsKey(entry.getKey())));
//            }
        }
    }
    Predicate timeExpired = new Predicate<Map.Entry<String, Long>>() {
        @Override
        public boolean test(Map.Entry<String, Long> entry) {
            boolean ret = System.currentTimeMillis() - entry.getValue() > expire;
            if (ret) {
                logger.info("三十分钟过期，清空掉" + entry.getKey() + " | " + entry.getValue());
            }
            return ret;
        }
    };
    public boolean exists(String fileName){
        System.out.println("数据库文件是否存在");
        System.out.println(fileName);
        return ((int)sqlSession.selectOne("san.fileExists",fileName))==1;
    }
    public boolean updateArtist(String artistName,long aimUpdateTimeStampSeconds){
        Map<String,Object> params = new HashMap<>();
        params.put("theName",artistName);
        params.put("time",aimUpdateTimeStampSeconds);
        int updated = sqlSession.update("san.refreshUpdateTime",params);
        return updated ==1;
    }
    synchronized  public  void updateArtistPathAndLevel(String name,int pic_level,String pic_path,int vid_level,String vid_path){
        Map<String,Object> params = new HashMap<>();
        params.put("name",name);
        params.put("pic_level",pic_level);
        params.put("vid_level",vid_level);
        params.put("pic_path",pic_path);
        params.put("vid_path",vid_path);
        System.out.println(params);
        Object x = sqlSession.update("san.updateArtistPathLevel",params);
        sqlSession.commit();
        System.out.println("----");
    }
    public static Map<String,Long> updateInfo;
    public Map<String,Long> getUpdateInfo(){
        if(updateInfo == null){
            updateInfo = new HashMap<>();
            List<Map<String,Object>> infos = sqlSession.selectList("san.getUpdateInfo");
            for (Map<String, Object> m :
                    infos) {
                updateInfo.put(m.get("name").toString(),(long)m.get("aimUpdateTime"));
            }
        }
        return updateInfo;
    }
    public List<String> getArtworkSanCodes(String name){
        return sqlSession.selectList("san.getArtworkSanCode",name);
    }
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        SankakuDBSourceManager db = new SankakuDBSourceManager(null);
////        boolean x =db.exists("0b2fd737759f76ed15e00c4fd7ba3865.jpg");
//        boolean y = db.updateArtist("suzuki (artist)",System.currentTimeMillis()+1999999);
//        System.out.println(y);
//        System.out.println(db.getUpdateInfo());
        System.out.println(db.getArtworkSanCodes("futs"));
    }
}
