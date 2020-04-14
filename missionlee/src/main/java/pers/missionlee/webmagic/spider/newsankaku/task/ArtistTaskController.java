package pers.missionlee.webmagic.spider.newsankaku.task;

import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import pers.missionlee.webmagic.spider.sankaku.manager.SourceManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-03-30 19:23
 */
public class ArtistTaskController extends AbstractTaskController {
    private String[] startUrls;
    private String artistName;
    public ArtistTaskController(NewSourceManager newSourceManager) {
        super(newSourceManager);
    }
    public ArtistTaskController(NewSourceManager newSourceManager, String artistName){
        this(newSourceManager);
        String[] keys = new String[1];
        keys[0] = artistName;
        setAimKeys(keys);
        this.artistName = artistName;
        init();
    }
    /**
     * 初始化作者已经存储的内容
     * */
    private void init(){
        // 获取作者名下 所有的sancode 存入 storedSanCode
        List<String> codes = sourceManager.getSanCodeOfArtist(artistName);
        System.out.println(codes);
        storedSanCode = codes;
        System.out.println(storedSanCode);
    }
    @Override
    public AimType getAimType(){
        return AimType.ARTIST;
    }

    @Override
    public String[] getStartUrls() {
        if(startUrls != null){
            return startUrls;
        }
        if(workMode == WorkMode.NEW){
            startUrls = SpiderUtils.getStartUrls(aimNum,aimKeys);
        }else if(workMode == WorkMode.UPDATE_ALL){
            startUrls = SpiderUtils.getStartUrls(aimNum,aimKeys);
        }else if(workMode == WorkMode.UPDATE){
            startUrls = new String[1];
            startUrls[0] = SpiderUtils.getUpdateStartUrl(getAimKeys());
        }
        return startUrls;
    }



    @Override
    public String getNumberCheckUrl() {
        return SpiderUtils.getNumberCheckUrl(aimKeys);
    }

    @Override
    public Boolean existOnDisk(String filename) {
        String parentPath = getArtistPath(artistName,filename);

        return new File(parentPath+filename).exists();
    }

    @Override
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo,boolean infoOnly) {
        if(infoOnly){
            sourceManager.saveArtworkInfo(artworkInfo);
            return true;
        }else{
            String aimDic = getArtistPath(artistName,fileName);
            try {
                if(!new File(aimDic+fileName).exists())
                    FileUtils.moveFile(tempFile,new File(aimDic+fileName));
                System.out.println("文件存储成功 "+ aimDic+"/"+fileName);
                artworkInfo.relativePath = aimDic.substring(aimDic.indexOf(":")+1);
                System.out.println("保存前artworkInfo： "+artworkInfo);
                sourceManager.saveArtworkInfo(artworkInfo);
                this.saveNum++;
                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return  false;

    }

    public String getArtistPath(String artistName,String filename){
        // TODO: 2020-03-31 这里需要对特别的作者名字进行改变

        String parentPath = sourceManager.getArtworkDicOfAimArtist(AimType.ARTIST,filename,artistName);
        System.out.println(parentPath);
        return parentPath;
    }
    public void finishUpdate(){
        int level = sourceManager.getArtistLevel(artistName);
        int times = 50 ; // 30 天
        boolean doUpdate = false; // 最终是否更新的标志位
        if (level == 5) {
            doUpdate = false;
//            times = 36000; // 5表示废弃 用不更新
        } else if (level == 4) {
            times = 360;
        } else if (level == 3) {
            times = 180;
        } else if (level == 2) {
            times = 90;
        } else if (level == 1 && getSaveNum() > 0) {
            times = 20; // 1级 有更新 20 天更新
        } else if (level == 0) {
            if (getSaveNum() == 0)
                times = 30; // 0 级本次没更新下次 30天更新
            else times = 15; // 0 级别 本次有个更新，下次 15天更新
        }
        if (doUpdate ) {
            long aimUpdateTime = System.currentTimeMillis() + times * 7 * 24 * 60 * 60 * 1000L;
            // 设定下次更新的时间
            // sankakuUpdateInfo.put(artistName, aimUpdateTime);
            // FileUtils.writeStringToFile(new File(sankakuDefaultInfoPath + UPDATE_INFO_FILE_NAME),
            //         JSON.toJSONString(sankakuUpdateInfo), "utf8", false);
            sourceManager.touchArtist(artistName, aimUpdateTime / 1000);
//            logger.info("更新完毕：" + artistName + "\t下次更新：" + times + "天 不再更新文本记录，只更新数据库记录");
        } else {
//            logger.warn("未做更近记录 ##########");
        }
    }
    public static void main(String[] args) {
        ArtistTaskController task = new ArtistTaskController( new NewSourceManager("H:\\ROOT","G:\\ROOT"),"combos & doodles");
//        task.getArtistPath("li chunfu","a.jpg");
    }
}
