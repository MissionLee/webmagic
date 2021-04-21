package pers.missionlee.webmagic.spider.newsankaku.source.bookparentsingle;

import pers.missionlee.webmagic.spider.newsankaku.source.AbstractSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.BookTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.ParentTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.SingleTaskController;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-03-21 17:30
 */
public class BookParentSingleSourceManager extends AbstractSourceManager {
    String BASE_ROOT;

    String SINGLE_PATH = "sankaku_single";
    String SINGLE_BASE;
    String SINGLE_VID_BASE;
    String SINGLE_PIC_BASE;

    String PARENT_PATH = "sankaku_parent";
    String PARENT_BASE;

    String BOOK_PATH = "sankaku_book";
    String BOOK_BASE;



    public BookParentSingleSourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);

        this.BASE_ROOT = baseRoot;
        String[] bases;
        if(roots!=null){
             bases = new String[roots.length+1];
            for (int i = 0; i < roots.length; i++) {
                bases[i] = roots[i];
            }
            bases[bases.length-1] = baseRoot;
        }else{
             bases = new String[1];
            bases[0] = baseRoot;
        }
        for (int i = 0; i < bases.length; i++) {
            System.out.println("bases + "+i+" : "+bases[i]);
        }
        for (int i = 0; i < bases.length; i++) {
            if (new File(PathUtils.buildPath(bases[i] , SINGLE_PATH)).exists() && new File(PathUtils.buildPath(bases[i] ,SINGLE_PATH)).isDirectory()) {
                SINGLE_BASE = PathUtils.buildPath(bases[i], SINGLE_PATH);
                System.out.println("---------------");
                break;
            }
        }
        for (int i = 0; i < bases.length; i++) {
            if (new File(PathUtils.buildPath(bases[i] , PARENT_PATH)).exists() && new File(PathUtils.buildPath(bases[i] , PARENT_PATH)).isDirectory()) {
                PARENT_BASE = PathUtils.buildPath(bases[i] , PARENT_PATH);
                System.out.println("---------------");

                break;
            }
        }
        for (int i = 0; i < bases.length; i++) {
            if (new File(PathUtils.buildPath(bases[i] , BOOK_PATH)).exists() && new File(PathUtils.buildPath(bases[i] ,BOOK_PATH)).isDirectory()) {
                BOOK_BASE = PathUtils.buildPath(bases[i],BOOK_PATH);
                System.out.println("---------------");

                break;
            }
        }
        if (SINGLE_BASE == null || PARENT_BASE == null || BOOK_BASE == null) {
            throw new RuntimeException("未能识别 single book parent base");
        } else {
            SINGLE_VID_BASE = SINGLE_BASE + "/VID/";
            SINGLE_PIC_BASE = SINGLE_BASE + "/PIC/";
        }
    }

    public static final String CHARACTER_ZERO = "z_unknown";
    public static final String CHARACTER_MULTIPLE = "z_multiple";
    public static final String COPYRIGHT_ZERO = "z_non_copyright";
    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        BookTaskController controller1 = (BookTaskController) controller;
        List<String> characters = info.getTagCharacter();
        List<String> artists = info.getTagArtist();
        List<String> studios = info.getTagStudio();
        List<String> copyright = info.getTagCopyright();
        String aimDic;
        // ============================= 确定 character string
        String characterString =CHARACTER_ZERO;
        if (characters == null || characters.size() == 0) {

        } else if (characters.size() == 1) {
            characterString = characters.get(0);
        } else {
            characterString = CHARACTER_MULTIPLE;
        }
        // ============================= 确定 copyright string
        String copyrightString = COPYRIGHT_ZERO;
        if(copyright == null || copyright.isEmpty()){

        }else if(copyright.size()>1){
            copyrightString = getPreferCopyright(copyright);
        }else {
            copyrightString = copyright.get(0);
        }

        if (controller instanceof ParentTaskController) {
            //  /基础目录/种类-有作者/copyright/character/作者/parentId
            //  /基础目录/种类-无作者/copyright/character/parentId

            if (artists == null || artists.isEmpty()) { // 没有作者
                aimDic = PathUtils.buildPath(PARENT_BASE,"non-artist",copyrightString,characterString,String.valueOf(controller1.bookParentInfo.getId()));
            } else { // 有作者
                aimDic = PathUtils.buildPath(PARENT_BASE,"artist",artists.get(0),String.valueOf(controller1.bookParentInfo.getId()));
            }
        } else if (controller instanceof BookTaskController) {
            if (artists == null || artists.isEmpty()) {
                aimDic = PathUtils.buildPath(BOOK_BASE,"non_artist",copyrightString,characterString,controller1.bookParentInfo.getId()+"_"+controller1.bookParentInfo.getName().replaceAll("/","_"));
            } else {
                aimDic = PathUtils.buildPath(BOOK_BASE,"artist",artists.get(0),controller1.bookParentInfo.getId()+"_"+controller1.bookParentInfo.getName().replaceAll("/","_"));
            }
        } else if (controller instanceof SingleTaskController) {
            if (artists == null || artists.isEmpty()) {
                aimDic = PathUtils.buildPath(SINGLE_BASE,"non_artist",copyrightString,characterString);
            } else {
                aimDic = PathUtils.buildPath(SINGLE_BASE,"artist",artists.get(0));
            }
        } else {
            aimDic = PathUtils.buildPath(BASE_ROOT,"error");
        }

        return aimDic;
    }

    @Override
    public int getStoredNum(TaskController controller) {
        return 0;
    }

    @Override
    public Set<String> getStoredSanCode(TaskController controller) {
        return null;
    }

    @Override
    public void extractAllFileNames(Set<String> names) {

    }

    public String getPreferCopyright(List<String> copyrights) {
        // 如果包含 “source filmmaker” 舍弃
        // 如果包含 series 优先舍弃
        String selected ="";
        if (copyrights.contains("source filmmaker")) { // 如果包含copyright 优先去除
            copyrights.remove("source filmmaker");

        }
        if (copyrights.size() == 1) { // 去除copyright之后只剩一个，就直接返回
            selected = copyrights.get(0);
        } else { // 还剩下多个，排除带有 series 的字符串，然后返回其中最长的
            for (int i = 0; i < copyrights.size(); i++) {
                if(i == 0){// 先默认选中第一个
                    selected = copyrights.get(i);
                } else if(selected.contains("series") ){ // 如果 当前选中包含 series 并且还有其他待选，直接用待选
                    selected = copyrights.get(i);
                }else if(copyrights.get(i).contains("")) {// 如果 新的待选没有 series 那么选中当前和待选之中比较长的
                    if(copyrights.get(i).length()>selected.length()) selected = copyrights.get(i);
                }
            }
        }
        return selected;
    }

    public static void main(String[] args) {

    }
}
