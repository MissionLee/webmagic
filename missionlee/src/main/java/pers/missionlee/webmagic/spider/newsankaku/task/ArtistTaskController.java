package pers.missionlee.webmagic.spider.newsankaku.task;

import org.apache.commons.io.FileUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        }else if(workMode == WorkMode.ALL){
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

    public static void main(String[] args) {
        ArtistTaskController task = new ArtistTaskController( new NewSourceManager("H:\\ROOT","G:\\ROOT"),"combos & doodles");
//        task.getArtistPath("li chunfu","a.jpg");
    }
}
