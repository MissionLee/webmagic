package pers.missionlee.webmagic.spider.sankaku.info;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-25 21:50
 */
public class UpdateInfo {
    private static final Long expireTime = 7*24*60*60*1000L;
//    // 作者总数
//    public int artistNum;
//    // 更新期内总数
//    public int updatedNum;

    public Map<String,Long> updateTime;

    public UpdateInfo() {
        this.updateTime = new HashMap<String, Long>();
    }

    public void update(String artist){
        if(updateTime==null){
            updateTime = new HashMap<String, Long>();
        }
        updateTime.put(artist,System.currentTimeMillis());
    }
    public Date getUpdateDate(String artist){
        if(updateTime!=null && updateTime.containsKey(artist)){
            return new Date(updateTime.get(artist));
        }else{
            return new Date(0L);
        }
    }
    public boolean needUpdate(String artist){
        if(!updateTime.containsKey(artist)){
            return true;
        }else{
            return (System.currentTimeMillis()-expireTime)>updateTime.get(artist);
        }
    }
    public static UpdateInfo getUpdateInfo(File updatInfoFile) throws IOException {
        String jsonInfo = FileUtils.readFileToString(updatInfoFile,"UTF8");
        UpdateInfo info =JSON.parseObject(jsonInfo,UpdateInfo.class);
        return info==null?(new UpdateInfo()):info;
    }
    public void writeUpdateInfo(File updatInfoFile) throws IOException {
        String json = JSON.toJSONString(this);
        FileUtils.writeStringToFile(updatInfoFile,json,"UTF8",false);
    }

    public static void main(String[] args) {
        System.out.println(new File("D:\\sankaku").listFiles().length);
    }
}
