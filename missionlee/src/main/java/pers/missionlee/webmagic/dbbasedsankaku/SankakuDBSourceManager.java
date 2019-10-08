package pers.missionlee.webmagic.dbbasedsankaku;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-25 21:29
 */
public class SankakuDBSourceManager {
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
    SourceManager sourceManager;

    static {
        try {
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            sqlSession = sqlSessionFactory.openSession();
            List<Map<String, Object>> tmpartists = sqlSession.selectList("san.getAllTargetArtist");
            artistFullInfo = new HashMap<>();
            long nowTimeStamp = System.currentTimeMillis()/1000;
            for (Map m :
                    tmpartists) {
//                System.out.println(m.get("name"));
                artistFullInfo.put(m.get("name").toString(), m);
                int picLevel = 10;
                if (!StringUtils.isEmpty(m.get("picLevel").toString()))
                    picLevel = Integer.parseInt(m.get("picLevel").toString());
                int vidLevel = 10;
                if (!StringUtils.isEmpty(m.get("vidLevel").toString()))
                    vidLevel = Integer.parseInt(m.get("vidLevel").toString());
                String name = m.get("name").toString();
                Integer minLevel = Math.min(picLevel, vidLevel);
                long updateTime = null == m.get("aimUpdateTime") ? 0 : (long) m.get("aimUpdateTime");
                artistLevel.put(name, minLevel);
                if (updateTime < nowTimeStamp) {
                    System.out.println("updateTIme: "+updateTime);
                    System.out.println("timeNow: "+nowTimeStamp);
                    artistNeedUpdate.put(name, minLevel);
                }
            }
            System.out.println("当前总收录作者：" + artistFullInfo.size());
            System.out.println("需要更新的作者：" + artistNeedUpdate.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SankakuDBSourceManager(SourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }

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

    public void addArtworkInfo(String artist, ArtworkInfo ar) {
        try {

            String sanCode = ar.getAddress().substring(ar.getAddress().lastIndexOf("/") + 1);
            String relativePath = sourceManager.getArtistPath(SourceManager.SourceType.SANKAKU, ar.getName(), artist).replace("E:/ROOT", "");
            String fileName = ar.getName();
            String fileSize = ar.getFileSize();
            String postDate = ar.getPostDate();
            String rating = ar.getRating();
            String resolutionRatio = ar.getResolutionRatio();
//                int status = 1;
            // 关闭了 配置文件 remove 机制，就算找不到文件，现在不改动信息文件
            int status = 2; // 1 正常 2 丢失 3 删除
            if (sourceManager.exists(SourceManager.SourceType.SANKAKU, artist, fileName)) {
                System.out.println("文件存在 " + artist + "  |  " + fileName);
                status = 1;
            }
            Long createTime = ar.getTakeTime();
            Map<String, Object> artworkInfoMap = new HashMap<>();
            artworkInfoMap.put("sanCode", sanCode);
            artworkInfoMap.put("relativePath", relativePath);
            artworkInfoMap.put("fileName", fileName);
            artworkInfoMap.put("fileSize", fileSize);
            artworkInfoMap.put("postDate", postDate);
            artworkInfoMap.put("rating", rating);
            artworkInfoMap.put("resolutionRatio", resolutionRatio);
            artworkInfoMap.put("status", status);
            artworkInfoMap.put("fileFormat", ar.getFormat());
            artworkInfoMap.put("createTime", createTime);
            artworkInfoMap.put("information", ar.toString());
            // System.out.println(map);

            Boolean stored = false;
            try {
                // todo 2.1 作品信息写入数据库
                sqlSession.insert("san.addArtwork", artworkInfoMap);
                sqlSession.commit();
            } catch (Exception e) {
                logger.info("已有作品");
                // System.out.println("写入作品信息失败");
                // todo 2.1.1 如果作品的 名称 或者 sancode 重复了，都会报错
                // 1. 作品可能重复（也就是 一个作品有多个作者，但是实际上 已经下载了多份） 重复就会报错
                // 2. 这段代码跑过多次
                Map<String, Object> map1 = sqlSession.selectOne("san.findArtworkWithSanCode", sanCode);
                if (relativePath.equals(map1.get("relativePath"))) {
                    // 如果 相对路径一样，表示代码跑重复了，没啥问题
                    logger.warn("已有作品在当前用户文件夹下");
                } else {
                    // 如果 当前路径和库里面的路径不一样，表示 真的重复了，删除后面出现的
                    logger.warn("这是个重复的作品-多个作者 删除当前这个");
                    String depFullPath = sourceManager.getArtistPath(SourceManager.SourceType.SANKAKU, ar.getName(), artist) + ar.getName();
                    System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
                    System.out.println("重复/删除" + depFullPath);
//                    FileUtils.writeStringToFile(depFile, depFullPath + "\n", "utf8", true);
//                    new File(depFullPath).delete();
                }
                stored = true;
            }
            if (true) { // 如果信息没有存储过： 可以相信只要存储过一次，所有的信息存储都是玩完整的
                //  ！！！ 事实证明 作品被存储过 也不一定 信息完整
                // 作者 ----------
                Map<String, Object> params = new HashMap<>();
                params.put("sanCode", sanCode);
                logger.info("添加作者关系" + ar.getTagArtist());
                if (ar.getTagArtist().size() == 0) {
                    logger.error("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
                }
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
        sqlSession.commit();
        return updated ==1;
    }
    public static void main(String[] args) {
        SankakuDBSourceManager db = new SankakuDBSourceManager(null);
//        boolean x =db.exists("0b2fd737759f76ed15e00c4fd7ba3865.jpg");
        boolean y = db.updateArtist("suzuki (artist)",System.currentTimeMillis()+1999999);
        System.out.println(y);
    }
}
