package pers.missionlee.webmagic.mybatis;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-07-24 23:53
 */
public class MyBatisConfiguration {
    public static void main(String[] args) throws IOException {
        String resource = "mybatis/mybatis-config.xml";

        InputStream inputStream = Resources.getResourceAsStream(resource);

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        System.out.println(sqlSession.selectList("san.findArtist"));
    }
}
