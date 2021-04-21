package pers.missionlee.webmagic.movetomysql;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-25 08:27
 * <p>
 * 把存储的图片信息写入 数据库
 * <p>
 *
 */
@Deprecated
public class MoveToMySQL {
    static Logger logger = LoggerFactory.getLogger(MoveToMySQL.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static List<String> errorNames = new ArrayList<String>(){{
        add("momochieri");
        add("eko");
        add("momo no kanzume");
}};
    public static class ArtistDown implements Callable {
        SourceManager sourceManager;
        SqlSession sqlSession;
        String artist;

        public ArtistDown(SourceManager sourceManager, SqlSession sqlSession, String artist) {
            this.sourceManager = sourceManager;
            this.sqlSession = sqlSession;
            this.artist = artist;
        }

        @Override
        public Object call() throws Exception {
            BigInteger targetId = sqlSession.selectOne("san.getTargetArtist", artist);
            if (targetId == null) {
                System.out.println(artist + " ID为null");
            } else {
                Integer dbArtworkNum = sqlSession.selectOne("san.getStoredArtworkNum", targetId);
                List artworkInfos = sourceManager.getArtworkOfArtist(SourceManager.SourceType.SANKAKU, artist);
                if (dbArtworkNum < artworkInfos.size()) {
                    System.out.println(artist + " 数量不够： 数据库 " + dbArtworkNum + " | json: " + artworkInfos.size());
                }
            }


            return null;
        }
    }

    public static class Extractor implements Callable {
        private static Long expire = 1000 * 60 * 3L;
        // 用于缓存 name
        public static ConcurrentHashMap<String, BigInteger> artistMap = new ConcurrentHashMap<>();
        public static ConcurrentHashMap<String, Long> artistMapExpire = new ConcurrentHashMap<>();
        // 缓存 copyright
        public static ConcurrentHashMap<String, BigInteger> characterMap = new ConcurrentHashMap<>();
        public static ConcurrentHashMap<String, Long> characterMapExpire = new ConcurrentHashMap<>();
        // studio
        public static ConcurrentHashMap<String, BigInteger> copyrightMap = new ConcurrentHashMap<>();
        public static ConcurrentHashMap<String, Long> copyrightMapExpire = new ConcurrentHashMap<>();

        Predicate timeExpired = new Predicate<Map.Entry<String, Long>>() {
            @Override
            public boolean test(Map.Entry<String, Long> entry) {
                boolean ret = System.currentTimeMillis() - entry.getValue() > expire;
                if (ret) {
                    logger.info("三分钟过期，清空掉" + entry.getKey() + " | " + entry.getValue());
                }
                return ret;
            }
        };

        CountDownLatch latch;
        ArtworkInfo ar;
        SourceManager sourceManager;
        SqlSession sqlSession;
        String artist;

        public Extractor(SourceManager sm, ArtworkInfo artworkInfo, SqlSession ss, String arname, CountDownLatch la) {
            ar = artworkInfo;
            sourceManager = sm;
            sqlSession = ss;
            artist = arname;
            latch = la;
        }

        @Override
        public Object call() throws Exception {
            try {
                String sanCode = ar.getAddress().substring(ar.getAddress().lastIndexOf("/") + 1);
                String relativePath = sourceManager.getArtistPath(SourceManager.SourceType.SANKAKU, ar.getFileName(), artist).replace("E:/ROOT", "");
                String fileName = ar.getFileName();
                String fileSize = ar.getFileSize();
                String postDate = ar.getPostDate();
                String rating = ar.getRating();
                String resolutionRatio = ar.getResolutionRatio();
//                int status = 1;
                // 关闭了 配置文件 remove 机制，就算找不到文件，现在不改动信息文件
                int status = 2; // 1 正常 2 丢失 3 删除
                if (sourceManager.existInDB(SourceManager.SourceType.SANKAKU, artist, fileName)) {
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
                        String depFullPath = sourceManager.getArtistPath(SourceManager.SourceType.SANKAKU, ar.getFileName(), artist) + ar.getFileName();
                        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
                        System.out.println("重复/删除" + depFullPath);
//                    FileUtils.writeStringToFile(depFile, depFullPath + "\n", "utf8", true);
                        new File(depFullPath).delete();
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
                latch.countDown();
                logger.info(Thread.currentThread() + " | " + latch.getCount());
                Long cur = System.currentTimeMillis();
                if (latch.getCount() == 1) {
                    artistMapExpire.entrySet().removeIf(timeExpired);
                    artistMap.entrySet().removeIf(entry -> (!artistMapExpire.containsKey(entry.getKey())));
                    characterMapExpire.entrySet().removeIf(timeExpired);
                    characterMap.entrySet().removeIf(entry -> !characterMapExpire.containsKey(entry.getKey()));
                    copyrightMapExpire.entrySet().removeIf(timeExpired);
                    copyrightMap.entrySet().removeIf(entry -> (!copyrightMapExpire.containsKey(entry.getKey())));
                }
            }
            return null;
        }
    }

    /**
     * 1. 获取jsonline文件，遍历文件
     * 2. 获取 jsonline文件对应的 作者的所有文件
     * 3. jsonline + 存储文件 构建
     */
    public static void main(String[] args) throws Exception {

        // 加载MyBatis 创建SQLSession
        String resource = "mybatis/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        // 创建SourceManager 获取用户列表 ！important 注意 刚开始没想这部分内容
        SourceManager sourceManager = new SourceManager("E:\\ROOT");
        Map<String, Integer> jsonArtist = sourceManager.getSankakuArtistListByJson();
        Set<String> jsonArtists = jsonArtist.keySet();
        Map<String, Object> allNames = new HashMap<>();
        for (String name :
                jsonArtists) {
            allNames.put(name, 0);
        }
        Set<String> allNameSet = allNames.keySet();


        // // TODO: 2019-07-26  并发任务分析： 如果不做限制，只是使用一个线程数为5的并发池子，直接开始循环，会造成创建了 很多“万”为单位的对象
        //                                   所以目标是把 同时等待的任务限制在 某个作者的总作品数量之下，当一个作者的作品全部处理完成，在处理下一个作者
        int i = 0;
        List<String> dbArtistList = sqlSession.selectList("san.getTargetArtistList");
        System.out.println("已经存储的作者数量： " + dbArtistList.size());
        for (String artist :
                MoveToMySQL.errorNames) { // todo  1. 遍历从 文件 和 本地获取的所有作者
//            ArtistDown artistDown = new ArtistDown(sourceManager,sqlSession,artist);
//            executorService.submit(artistDown);
            if (true) {
                List<ArtworkInfo> artworkInfos = null;
                boolean needCheck = true;

//            if (dbArtistList.contains(artist))
//                needCheck = false;

                artworkInfos = sourceManager.getArtworkOfArtist(SourceManager.SourceType.SANKAKU, artist);
                // TODO: 2019-07-28  下面这一句 临时注释掉，改在后面
                // 查询库里面有没有 这个人 并且 isTarget = 1 ：表示被收录的用户
                BigInteger targetId = sqlSession.selectOne("san.getTargetArtist", artist);

                if (targetId != null) {// 如果用户存在
                    //  1. 验证目标用户存在 2.验证已经收录作品数量
//                // 查询数据库里面有这个用户多少内容
                    Integer dbArtworkNum = sqlSession.selectOne("san.getStoredArtworkNum", targetId);
                    logger.info("用户已经存在：" + artist + " | 已经收录作品 " + dbArtworkNum + " | json信息作品 " + artworkInfos.size());
                    if (dbArtworkNum + 2 > artworkInfos.size())// 允许一点误差，不然不好用
                        needCheck = false;
                } else {  // 没有 target 用户： 可能没有这个用户，可能有一个 非target用户
                    // 获取jsonline信息
                    Map<String, Object> thisArtist = new HashMap<>();
                    thisArtist.put("name", artist);
                    // 获取作者 作品 两个等级
                    thisArtist.putAll(sourceManager.getPriority(SourceManager.SourceType.SANKAKU, artist));
                    // 获取作者作品 两个数量
                    thisArtist.putAll(sourceManager.getRealAartworkNum(SourceManager.SourceType.SANKAKU, artist));
                    thisArtist.put("infoed_num", artworkInfos.size());
                    // 判断是真的没有用户，还是用户非target
                    BigInteger artistId = sqlSession.selectOne("san.getArtistId", artist);
                    if (artistId == null) {
                        logger.info("新的作者" + artist);
                        sqlSession.insert("san.insertArtist", thisArtist);
                        sqlSession.commit();
                    } else {
                        logger.info("已有作者" + artist);
                        sqlSession.update("san.updateArtist", thisArtist);
                        sqlSession.commit();

                    }
                }


                if (needCheck) {
                    // 创建一个和作品数量相同的 latch
                    CountDownLatch latch = new CountDownLatch(artworkInfos.size());
                    for (ArtworkInfo ar :
                            artworkInfos) {// todo 2. 遍历 作者作品信息
                        // TODO: 2019-07-26 !important 对于一个作者的作品，计划通过多线程来加快处理速度
                        // // TODO: 2019-07-26  创建一个 工作队列， 允许 5个线程同时工作，把所有任务加载到队里里面，当所有任务完成
                        // 每个线程任务 都会获取 这个latch ，并且 当这个线程工作完成之后 会countDown
                        // 我们这里直接把 创建了作者所有记录对应的任务，交给 executorService，
                        // 而 executorService 会控制并发为5
                        Extractor extractor = new Extractor(sourceManager, ar, sqlSession, artist, latch);
                        Future<Object> future = executorService.submit(extractor);

                    }
                    latch.await();
                }
            }


        }
    }

    public static void insertTag(SqlSession sqlSession, List<String> tags, int type, Map params) {
        System.out.println("添加标签关系" + tags);
        if (tags != null)
            for (String tag : tags
            ) {
                params.put("name", tag);
                params.put("type", type);
                BigInteger tid = sqlSession.selectOne("san.getTagId", params);
                params.put("tag_id", tid);
                if (tid == null) {
                    params.put("name", tag);
                    sqlSession.insert("san.insertTag", params);
                    sqlSession.commit();

                }
                try {
                    sqlSession.insert("san.addTagRel", params);
                    sqlSession.commit();

                } catch (Exception e) {
                    System.out.println("标签关系已经存在");
                }
            }
    }
}
