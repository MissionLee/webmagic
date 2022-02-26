package pers.missionlee.chan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.chan.pojo.BookInfo;
import pers.missionlee.webmagic.spider.newsankaku.dao.LevelInfo;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-10 18:02
 */
public class DataBaseService {
    static Logger logger = LoggerFactory.getLogger(DataBaseService.class);

    private static Long expire = 1000 * 60 * 30L;

//    static Map<String, Map<String, Object>> artistFullInfo;
    static Map<String,Integer> studioArtistLevel = new HashMap<>();
    static Map<String,Integer> copyrightArtistLevel = new HashMap<>();
    static Map<String, Integer> commonArtistLevel = new HashMap<>();

    static Map<String, Integer> commonArtistNeedUpdate = new HashMap<>();


    // 用于缓存 name
    public static ConcurrentHashMap<String, BigInteger> artistMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Long> artistMapExpire = new ConcurrentHashMap<>();
    // 缓存 copyright
    public static ConcurrentHashMap<String, BigInteger> characterMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Long> characterMapExpire = new ConcurrentHashMap<>();
    // studio
    public static ConcurrentHashMap<String, BigInteger> copyrightMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Long> copyrightMapExpire = new ConcurrentHashMap<>();

    static SqlSession sqlSession;
    static String resource = "mybatis/mybatis-config.xml";

    public DataBaseService() {
        try {
            // 解析MyBatis配置文件
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            sqlSession = sqlSessionFactory.openSession(true);
            List<Map<String, Object>> tmpartists = sqlSession.selectList("chan.artist.getTargetCommonArtist");
            long nowTimeStamp = System.currentTimeMillis()/1000;
            for (Map m :
                    tmpartists) {
                int picLevel = 10;
                if (m.containsKey("picLevel")&&!StringUtils.isEmpty(m.get("picLevel").toString()))
                    picLevel = Integer.parseInt(m.get("picLevel").toString());
                int vidLevel = 10;
                if (m.containsKey("vidLevel")&&!StringUtils.isEmpty(m.get("vidLevel").toString()))
                    vidLevel = Integer.parseInt(m.get("vidLevel").toString());
                String name = m.get("name").toString();
                Integer minLevel = Math.min(picLevel, vidLevel);
                long updateTime = null == m.get("aimUpdateTime") ? 0 : (long) m.get("aimUpdateTime");
                commonArtistLevel.put(name, minLevel);
                if (updateTime < nowTimeStamp) {
                    commonArtistNeedUpdate.put(name, minLevel);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void makeSanCodeDeleted(String sanCode){
        sqlSession.update("chan.artwork.markDeleted",sanCode);
    }
    public void makeSanCodeVip(String sanCode,String artist){
            sqlSession.update("chan.artwork.markAsVip",sanCode);
//        if(sanCodeExist(sanCode)){
//        }else{
//            logger.info("注意：！！！ 原本设计 如果一个作品是vip 或者被删除，如果这个作品不存在，那么创建一个空作品，思考之后感觉没必要会造成其他麻烦");
//             ArtworkInfo artworkInfo = new ArtworkInfo();
//             List<String> artists = new ArrayList<>();
//             artists.add(artist);
//             artworkInfo.sanCode = sanCode;
//             artworkInfo.setTagArtist(artists);
//             saveArtworkInfo(artworkInfo);
//            sqlSession.update("chan.artwork.markAsVip",sanCode);
//
//        }
    }
    public void makeSanCodeSkip(String sanCode,String parentId){
        Map<String,String> map = new HashMap<>();
        map.put("sanCode",sanCode);
        map.put("parentId",parentId);
        sqlSession.update("chan.artwork.markAsSkip",map);
    }
    public boolean bookIdExists(int bookId){
        Object num = sqlSession.selectOne("chan.book.bookIdExists",bookId);
        if(null == num){
            return false;
        }else{
            Integer theNum = (Integer)num;
            return theNum>0;
        }
    }
    public List<String> getDelMarkFiles(String artistName){
        return sqlSession.selectList("chan.artwork.getMarkDel",artistName);
    }
    public boolean checkArtistIsTarget(String artistName){
        return 1==(Integer) (sqlSession.selectOne("chan.artist.isTarget",artistName));
    }
    public boolean needBook(String artistName){
        BigInteger x = sqlSession.selectOne("chan.artist.needBook",artistName);
        System.out.println(x.toString());
        System.out.println(x.getClass());
        return x.longValue()< (System.currentTimeMillis()/1000-30*24*60*60);
//        System.out.println(333);
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return  ()
//                >(System.currentTimeMillis()*1.0/1000+30*24*60*60);
    }
    public boolean needParent(String artistName){
        BigInteger y = sqlSession.selectOne("chan.artist.needParent",artistName);
        return  y.longValue() <(System.currentTimeMillis()/1000-30*24*60*60);
//        return ()>(System.currentTimeMillis()*1.0/1000+30*24*60*60);
    }
    public List<String> getArtistByFileName(String fileName){
        return sqlSession.selectList("chan.artwork.getArtistNameByFileName",fileName);
    }
    public void updateBookDone(String artistName){
        sqlSession.update("chan.artist.bookDone",artistName);
    }
    public void updateParentDone(String artistName){
        sqlSession.update("chan.artist.parentDone",artistName);
    }
    public void updateBookId(String sanCode,String bookId){
        int book = Integer.valueOf(bookId);
        Map<String,Object> params = new HashMap<>();
        params.put("sanCode",sanCode);
        params.put("bookId",book);
        sqlSession.update("chan.artwork.updateBookId",params);
    }
    public void updateParentId(String sanCode,int parentId){
        Map<String,Object> params = new HashMap<>();
        params.put("sanCode",sanCode);
        params.put("parentId",parentId);
        sqlSession.update("chan.artwork.updateParentId",params);
    }
    public List<String> getArtistParentIdStoredPart(String artistName){
        return sqlSession.selectList("chan.book.getArtistParentIdStoredPart",artistName);
    }
    public List<String> getArtistParentSanCodeToCheck(String artistName){
        return sqlSession.selectList("chan.artwork.getArtistParentSanCodeToCheck",artistName);
    }
    public void updateBookArtist(String bookId,String artistName){
        Map<String,String> params = new HashMap<>();
        params.put("bookId",bookId);
        params.put("artistName",artistName);
        sqlSession.update("chan.book.updateBookArtist",params);
    }
    public String getBookStoredArtistById(int bookId){
        return sqlSession.selectOne("chan.book.getStoreArtistById",bookId);
    }
    public List<String> getArtistBySanCode(String sanCode){
        return sqlSession.selectList("chan.artwork.getArtistNameBySanCode");
    }
    public List<LevelInfo> getLevelInfos(){
        return sqlSession.selectList("san.getLevelInfos");
    }
    public void makeArtistLost(String artistName){
        sqlSession.update("chan.artist.makeArtistLost",artistName);
    }
    public void touchArtist(String artistName,long nextUpdateTime){
        Map<String,Object> params = new HashMap<>();
        params.put("theName",artistName);
        params.put("time",nextUpdateTime/1000);
        sqlSession.update("san.refreshUpdateTime",params);
    }
    public void updateArtistPathLevel(LevelInfo info){
        Map<String,Object> params = new HashMap<>();
        params.put("name",info.name);
        params.put("pic_level",info.picLevel);
        params.put("vid_level",info.vidLevel);
        params.put("pic_path",info.picPath);
        params.put("vid_path",info.vidPath);
        sqlSession.update("san.updateArtistPathLevel",params);
    }
    public List<String> getArtistListByLevel(int level,boolean expired){
        List<String> artistList = new ArrayList<>();
        Map<String,Integer> aimMap = null;
        if(expired){
            aimMap = commonArtistNeedUpdate;
        }else{
            aimMap = commonArtistLevel;
        }
        aimMap.forEach((String iName,Integer iLevel )->{
            if(iLevel == level){
                artistList.add(iName);
            }
        });
        return artistList;
    }
    public String getArtistNeedUpdateByLevelRandom(int level){
        return sqlSession.selectOne("chan.artist.getRandomArtistNeedUpdate",level);
    }
    public void saveBookInfo(BookInfo bookInfo){
        sqlSession.insert("chan.book.saveBookInfo",bookInfo);
    }
    public boolean sanCodeExist(String sanCode){
        return 1==(Integer) sqlSession.selectOne("san.sanCodeExist",sanCode);
    }
    public void saveArtworkInfo(ArtworkInfo ar){
        try {


            Long createTime = ar.getTakeTime();
            Map<String, Object> artworkInfoMap = new HashMap<>();
            artworkInfoMap.put("fileName", ar.getFileName());
            artworkInfoMap.put("sanCode", ar.sanCode);
            artworkInfoMap.put("fileSize", ar.getFileSize());
            artworkInfoMap.put("fileFormat", ar.getFormat());
            artworkInfoMap.put("relativePath", ar.relativePath);
            artworkInfoMap.put("postDate", ar.getPostDate());
            artworkInfoMap.put("rating", ar.getRating());
            artworkInfoMap.put("resolutionRatio", ar.getResolutionRatio());
            artworkInfoMap.put("information", ar.toString());
            logger.warn("DataBaseService#239 为了节省数据库开销，将很多信息类字段设置为null");
            artworkInfoMap.put("fileSize", null);
            artworkInfoMap.put("fileFormat", null);
            artworkInfoMap.put("relativePath", null);
            artworkInfoMap.put("postDate", null);
            artworkInfoMap.put("rating", null);
            artworkInfoMap.put("resolutionRatio", null);
            artworkInfoMap.put("information", null);
            // show type 省略
            // store type  默认1 正常保存
            artworkInfoMap.put("createTime", createTime);
            artworkInfoMap.put("status", 1);
            artworkInfoMap.put("official",ar.official);
            artworkInfoMap.put("bookId",ar.bookId);
            artworkInfoMap.put("parentId",ar.parentId);
            artworkInfoMap.put("isSingle",ar.isSingle);
            artworkInfoMap.put("storePlace",ar.storePlace);

            Boolean stored = false;
            try {
                // todo 2.1 作品信息写入数据库
                sqlSession.insert("chan.artwork.save", artworkInfoMap);
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
                    System.out.println("添加角色："+character);
                    BigInteger cid;
                    if (characterMap.containsKey(character)) {
                        cid = characterMap.get(character);
                        params.put("character_id", cid);
                    } else {
                        cid = sqlSession.selectOne("san.getCharacterId", character);
                        if (cid == null) {
                            params.put("name", character);
                            sqlSession.insert("san.insertCharacter", params);// 拿到的就是 character_id 带着下划线的
                            System.out.println("-==================================-");
                            System.out.println();
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
                    } catch (Exception e) {
                        logger.warn("角色关系已经存在 " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            artistMapExpire.entrySet().removeIf(timeExpired);
            artistMap.entrySet().removeIf(entry -> (!artistMapExpire.containsKey(entry.getKey())));
            characterMapExpire.entrySet().removeIf(timeExpired);
            characterMap.entrySet().removeIf(entry -> !characterMapExpire.containsKey(entry.getKey()));
            copyrightMapExpire.entrySet().removeIf(timeExpired);
            copyrightMap.entrySet().removeIf(entry -> (!copyrightMapExpire.containsKey(entry.getKey())));
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

    public List<String> getSanCodeByArtistName(String artistName){
        return sqlSession.selectList("san.getSanCodesByArtist",artistName);
    }
}
