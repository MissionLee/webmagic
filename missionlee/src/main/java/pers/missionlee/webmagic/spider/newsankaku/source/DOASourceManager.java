package pers.missionlee.webmagic.spider.newsankaku.source;

import pers.missionlee.webmagic.spider.newsankaku.task.DOATaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;
import us.codecraft.webmagic.Spider;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-15 14:00
 */
public class DOASourceManager extends AbstractSourceManager {
    public DOASourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }


    @Override
    public int getStoredNum(TaskController controller) {

        return 0;
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        List<String> copyRights = DOATaskController.DEFAULT_COPYRIGHTS;
        List<String> characters = DOATaskController.DEFAULT_CHARACTERS;
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
     * ROOT/sankaku_doa_series/
     * - pic 与vid策略相同
     * - vid
     * - kasumi (dead or alive)      角色名 单个角色的作品
     * - z_unknown 没有标注角色的作品
     * - z_two_girl
     * - z_three_girl
     * - z_multiple_girl
     */
    public static final String PATH_DOA_SERIES = "sankaku_doa_series";
    public static final String PATH_PIC = "pic";
    public static final String PATH_VID = "vid";
    public static final String PATH_ZERO = "z_unknown";
    public static final String PATH_TWO = "z_two_girl";
    public static final String PATH_THREE = "z_three_girl";
    public static final String PATH_MULTIPLE = "z_multiple_girl";

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        List<String> characters = info.getTagCharacter();
        String fileName = info.getName();
        String typePath = PATH_PIC;
        if (PathUtils.isVideo(fileName))
            typePath = PATH_VID;
        if (characters == null || characters.size() == 0) {
            return PathUtils.buildPath(baseRoot, PATH_DOA_SERIES, typePath, PATH_ZERO);
        } else if (characters.size() > 3) {
            return PathUtils.buildPath(baseRoot, PATH_DOA_SERIES, typePath, PATH_THREE);
        } else if (characters.size() == 2) {
            return PathUtils.buildPath(baseRoot, PATH_DOA_SERIES, typePath, PATH_TWO);
        } else {
            return PathUtils.buildPath(baseRoot, PATH_DOA_SERIES, typePath, characters.get(0));
        }

    }

    public static void main(String[] args) {

    }
}
