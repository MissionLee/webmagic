package pers.missionlee.webmagic.spider.newsankaku.source.series;

import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.newsankaku.utlis.PathUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-22 11:05
 */
public class OverwatchSourceManager extends AbstractCopyRightAndCharacterSourceManager {
    public OverwatchSourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }

    @Override
    public void resetBaseRoot() {
        if(new File(baseRoot+PATH_OVERWATCH_SERIES).exists()
                && new File(baseRoot+PATH_OVERWATCH_SERIES).isDirectory()
        ){


        }else{
            for (int i = 0; i < addRoots.length; i++) {
                if(new File(addRoots[i]+PATH_OVERWATCH_SERIES).exists()
                        && new File(addRoots[i]+PATH_OVERWATCH_SERIES).isDirectory()
                ){
                    baseRoot = addRoots[i];
                    break;
                }
            }
        }
    }


    @Override
    public void moveDownloadedFromArtist(ArtistSourceManager artistSourceManager) {

    }

    public static final String PATH_OVERWATCH_SERIES = "sankaku_overwatch_series";

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return getAimDicOfCopyRight(controller,info,PATH_OVERWATCH_SERIES);
    }

    @Override
    public void extractAllFileNames(Set<String> names) {
        String pic_path = PathUtils.buildPath(baseRoot,PATH_OVERWATCH_SERIES,PATH_PIC);
        String vid_path = PathUtils.buildPath(baseRoot,PATH_OVERWATCH_SERIES,PATH_VID);
        File[] vids = new File(vid_path).listFiles();
        for (int i = 0; i < vids.length; i++) {
            String[] files = vids[i].list();
            names.addAll(Arrays.asList(files));
        }
        System.out.println(names.size());
        File[] pics = new File(pic_path).listFiles();
        for (int i = 0; i < pics.length; i++) {
            String[] files = pics[i].list();
            names.addAll(Arrays.asList(files));
        }
        System.out.println(names.size());
    }
}
