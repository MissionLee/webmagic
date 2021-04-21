package pers.missionlee.webmagic.spider.newsankaku.source.official;

import pers.missionlee.webmagic.spider.newsankaku.source.AbstractSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.type.AimType;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-01-16 18:25
 */
public class CopyrightOfficialSourceManager extends AbstractSourceManager {

    String OFFICIAL_PATH="sankaku_copyright_official";
    String OFFICIAL_BASE;
    String OFFICIAL_VID_BASE ;
    String OFFICIAL_PIC_BASE ;
    public static Map<String,String> specialName = new HashMap<>();

    public CopyrightOfficialSourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
        // 判断 official 目录在拿个目录下面
        if(new File(baseRoot+OFFICIAL_PATH).exists()
                && new File(baseRoot+OFFICIAL_PATH).isDirectory()
        ){
            OFFICIAL_BASE = baseRoot+ OFFICIAL_BASE;


        }else{
            for (int i = 0; i < addRoots.length; i++) {
                if(new File(addRoots[i]+OFFICIAL_PATH).exists()
                        && new File(addRoots[i]+OFFICIAL_PATH).isDirectory()
                ){
                    OFFICIAL_BASE = addRoots[i]+OFFICIAL_PATH;
                    break;
                }
            }
        }
        if(OFFICIAL_BASE != null){
            OFFICIAL_VID_BASE = OFFICIAL_BASE +"/VID/";
            OFFICIAL_PIC_BASE = OFFICIAL_BASE+"/PIC/";
        }else{
            throw new RuntimeException("未能识别到 sankaku_copyright_official");
        }
    }

    @Override
    public int getStoredNum(TaskController controller) {
        AimType aimType = controller.getAimType();
        String officialName = controller.getAimKeys()[0];
        // 对应sql 查询的是 copyright 对应的 状态不为 丢失的作品数量
        return sourceService.getStoredNumOfCopyRight(officialName);
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        Set<String> codes = new HashSet<>();
        String officialName = controller.getAimKeys()[0];
        System.out.println("在OfficialSourceManager 的officialName:"+officialName);
        codes.addAll(sourceService.getSanCodesByCopyRightOfficial(officialName));
        return codes;
    }

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        String officialName = controller.getAimKeys()[0];
        if(officialName.contains("/")) officialName=officialName.replaceAll("/","_");
        String artworkName = info.getFileName();
        if(PathUtils.isVideo(artworkName)){
            return PathUtils.buildPath(OFFICIAL_VID_BASE,officialName);
        }else{
            return PathUtils.buildPath(OFFICIAL_PIC_BASE,officialName);
        }
    }

    @Override
    public void extractAllFileNames(Set<String> names) {
        File[] vidOfficials = new File(OFFICIAL_VID_BASE).listFiles();
        for (int i = 0; i < vidOfficials.length; i++) {
            String[] files = vidOfficials[i].list();
            names.addAll(Arrays.asList(files));
        }
        System.out.println(names.size());
        File[] picOfficials = new File(OFFICIAL_PIC_BASE).listFiles();
        for (int i = 0; i < picOfficials.length; i++) {
            String[] files = picOfficials[i].list();
            names.addAll(Arrays.asList(files));
        }
        System.out.println(names.size());
    }
}
