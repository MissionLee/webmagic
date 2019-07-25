package pers.missionlee.webmagic.movetomysql;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-25 08:27
 * <p>
 * 把存储的图片信息写入 数据库
 */
public class MoveToMySQL {
    /**
     * 1. 获取jsonline文件，遍历文件
     * 2. 获取 jsonline文件对应的 作者的所有文件
     * 3. jsonline + 存储文件 构建
     */
    public static void main(String[] args) throws Exception {

        String resource = "mybatis/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();

        SourceManager sourceManager = new SourceManager("E:\\ROOT");
        Map<String, Integer> jsonArtist = sourceManager.getSankakuArtistListByJson();
        Set<String> jsonArtists = jsonArtist.keySet();
        Set<String> a = sourceManager.getSankakuArtistsListByDir().keySet();
        System.out.println(jsonArtist.size() + " - " + a.size());
        Map<String, Object> allNames = new HashMap<>();
        for (String name :
                jsonArtists) {
            allNames.put(name, 0);
        }
        for (String name : a) {
            allNames.put(name, 0);
        }
        jsonArtist = null;
        a = null;
        Set<String> allNameSet = allNames.keySet();
        System.out.println(allNameSet);
//        for (String name :allNameSet
//        ) {
//            sqlSession.insert("san.insertArtist",name);
//        }
//        sqlSession.commit();

        int i = 0;
        for (String artist :
                allNameSet) { // todo  1. 遍历从 文件 和 本地获取的所有作者


            i++;
            if (true) {

                // 获取jsonline信息
                List<ArtworkInfo> artworkInfos = sourceManager.getArtworkOfArtist(SourceManager.SourceType.SANKAKU, artist);
                Map<String, Object> thisArtist = new HashMap<>();
                thisArtist.put("name", artist);
                // 获取作者 作品 两个等级
                thisArtist.putAll(sourceManager.getPriority(SourceManager.SourceType.SANKAKU, artist));
                // 获取作者作品 两个数量
                thisArtist.putAll(sourceManager.getRealAartworkNum(SourceManager.SourceType.SANKAKU, artist));
                thisArtist.put("infoed_num", artworkInfos.size());
                BigInteger artistId = sqlSession.selectOne("san.getArtistId", artist);
                if (artistId == null) {
                    System.out.println("新的作者"+artist);
                    sqlSession.insert("san.insertArtist", thisArtist);
                } else {
                    System.out.println("已有作者"+artist);
                    sqlSession.update("san.updateArtist", thisArtist);
                }
                for (ArtworkInfo ar :
                        artworkInfos) {// todo 2. 遍历 作者作品信息

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
                        e.printStackTrace();
                        System.out.println("写入作品信息失败");
                        // todo 2.1.1 如果作品的 名称 或者 sancode 重复了，都会报错
                        // 1. 作品可能重复（也就是 一个作品有多个作者，但是实际上 已经下载了多份） 重复就会报错
                        // 2. 这段代码跑过多次
                        Map<String, Object> map1 = sqlSession.selectOne("san.findArtworkWithSanCode", sanCode);
                        if (relativePath.equals(map1.get("relativePath"))) {
                            // 如果 相对路径一样，表示代码跑重复了，没啥问题
                            System.out.println("代码跑重复了没啥问题");
                        } else {
                            // 如果 当前路径和库里面的路径不一样，表示 真的重复了，删除后面出现的
                            String depFullPath = sourceManager.getArtistPath(SourceManager.SourceType.SANKAKU, ar.getName(), artist) + ar.getName();
                            System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
                            System.out.println("重复/删除" + depFullPath);
                            //  new File(depFullPath).delete();
                        }
                        stored = true;
                    }
                    if (!stored) { // 如果信息没有存储过： 可以相信只要存储过一次，所有的信息存储都是玩完整的
                        // 作者 ----------
                        Map<String, Object> params = new HashMap<>();
                        params.put("sanCode", sanCode);
                        System.out.println("添加作者关系" + ar.getTagArtist());
                        for (String artistName : ar.getTagArtist()) {
                            // 查询 名称对应的作者id
                            BigInteger id = sqlSession.selectOne("san.getArtistId", artistName);
                            params.put("artist_id", id);
                            if (id == null) { // 如果id 不存在
                                params.put("name", artistName);
                                sqlSession.insert("san.insertArtist", params);
                                System.out.println("生成新的作者：" + params.get("artist_id"));
                                sqlSession.commit();
//                                id = (BigInteger) params.get("artist_id");
//                                // System.out.println(params);
//                                if (id == null) throw new Exception("adfafdf");
//                                // System.out.println("这个作者还不存在 创建一个：id" + id + " 名字 " + artistName);
                            }
                            try {
                                sqlSession.insert("san.addArtistRel", params);
                                sqlSession.commit();
                            } catch (Exception e) {
                                System.out.println("作者关系已经存在");
                            }
                        }
                        System.out.println("添加角色关系" + ar.getTagCharacter());
                        // character 角色
                        for (String character : ar.getTagCharacter()) {
                            BigInteger cid = sqlSession.selectOne("san.getCharacterId", character);
                            params.put("character_id", cid);
                            if (cid == null) {
                                params.put("name", character);
                                sqlSession.insert("san.insertCharacter", params);
                                sqlSession.commit();

//                                System.out.println(params);
                                //cid = (BigInteger) params.get("character_id");
                            }
                            try {
                                sqlSession.insert("san.addCharacterRel", params);
                                sqlSession.commit();
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("角色关系已经存在");
                            }

                        }


                        // 版权
                        System.out.println("添加版权关系" + ar.getTagCopyright());
                        for (String copyright : ar.getTagCopyright()) {
                            BigInteger cid = sqlSession.selectOne("san.getCopyrightId", copyright);
                            params.put("copyright_id", cid);
                            if (cid == null) {
                                params.put("name", copyright);
                                sqlSession.insert("san.insertCopyright", params);
                                sqlSession.commit();

                                System.out.println("参数：" + params);
                                //cid = (BigInteger) params.get("character_id");
                            }
                            try {
                                sqlSession.insert("san.addCopyrightRel", params);
                                sqlSession.commit();
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("角色关系已经存在");
                            }
                        }
                        // tag
//                        insertTag(sqlSession, ar.getTagGeneral(), 1, params);
//                        insertTag(sqlSession, ar.getTagGenre(), 2, params);
//                        insertTag(sqlSession, ar.getTagMedium(), 3, params);
//                        insertTag(sqlSession, ar.getTagMeta(), 4, params);
//                        insertTag(sqlSession, ar.getTagStudio(), 5, params);
                    }
                    break;
                }

            } else if (i > 27) {
                break;
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
