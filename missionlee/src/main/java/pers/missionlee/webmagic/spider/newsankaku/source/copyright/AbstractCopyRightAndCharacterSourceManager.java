package pers.missionlee.webmagic.spider.newsankaku.source.copyright;

import pers.missionlee.webmagic.spider.newsankaku.source.AbstractSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.copyright.AbstractCopyrightAndCharacterTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-22 11:07
 */
public abstract class AbstractCopyRightAndCharacterSourceManager extends AbstractSourceManager {
    public static final String PATH_PIC = "pic";
    public static final String PATH_VID = "vid";
    public static final String PATH_ZERO = "z_unknown";
    public static final String PATH_TWO = "z_two_girl";
    public static final String PATH_THREE = "z_three_girl";
    public static final String PATH_MULTIPLE = "z_multiple_girl";


    public AbstractCopyRightAndCharacterSourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }

    @Override
    public int getStoredNum(TaskController controller) {

        return 0;
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        System.out.println("=====================");
        System.out.println("=====================");
        System.out.println("=====================");
        System.out.println("=====================");
        System.out.println("=====================");
        System.out.println("=====================");
        System.out.println("=====================");
        List<String> copyRights = ((AbstractCopyrightAndCharacterTaskController)controller).DEFAULT_COPYRIGHTS;
        List<String> characters = ((AbstractCopyrightAndCharacterTaskController)controller).DEFAULT_CHARACTERS;
        Set<String> sanCodes = new HashSet<>();
        for (String copyRight :
                copyRights) {
            List<String> c = sourceService.getSanCodesByCopyRight(copyRight);
            if (c != null) {
                System.out.println(copyRight + " : " + c.size());

                sanCodes.addAll(c);
            }
        }
        for (String character :
                characters) {
            List<String> c = sourceService.getSanCodesByCharacter(character);
            if (c != null) {
                sanCodes.addAll(c);
                System.out.println(character + " : " + c.size());

            }
        }
        return sanCodes;
    }

    /**
     * 路径生成策略：
     * - baseRoot / 版权路径 / 类型路径 / 角色路径
     *
     * 版权路径：不同 SourceManager 自定义的路径，
     * 类型路径：（pic/vid）和 （pic_3d/vid_cd）几种可能，系统会根据文件和 作品信息，自动判断
     * 角色路径： 作品信息中包含的角色名称，没有名称会归为 unknown ，单个名称会使用 角色名，多个名称会分为：2/3/>3 三种情况
     * */
    public String getAimDicOfCopyRight(TaskController controller, ArtworkInfo info,String PATH_COPYRIGHT) {
        List<String> characters = info.getTagCharacter();
        String fileName = info.getName();
        String typePath = PATH_PIC;
        if (PathUtils.isVideo(fileName))
            typePath = PATH_VID;
        if(info.getTagMeta().contains("3d")){
            typePath = typePath+"_3d";
        }
        System.out.println("生成路径："+characters.size()+" | "+characters);
        if (characters == null || characters.size() == 0) {// 没标记角色的情况
            return PathUtils.buildPath(baseRoot, PATH_COPYRIGHT, typePath, PATH_ZERO);
        } else if (characters.size() > 3){// 多余三人
            return PathUtils.buildPath(baseRoot, PATH_COPYRIGHT, typePath, PATH_MULTIPLE);
        }else if(characters.size() ==3) {// 等于三人
            return PathUtils.buildPath(baseRoot, PATH_COPYRIGHT, typePath, PATH_THREE);
        } else if (characters.size() == 2) {// 等于两人
            return PathUtils.buildPath(baseRoot, PATH_COPYRIGHT, typePath, PATH_TWO);
        } else {// 等于一人
            return PathUtils.buildPath(baseRoot, PATH_COPYRIGHT, typePath, characters.get(0));
        }

    }
//    public String getAimDicOfCopyRightAndSubVersion(TaskController controller,ArtworkInfo info,String PATH_COPYRIGHT){
//
//    }
}
