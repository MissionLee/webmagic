package pers.missionlee.webmagic.spider.newsankaku.source.series;

import pers.missionlee.webmagic.spider.newsankaku.source.artist.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.TaskController;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.Set;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-23 10:44
 */
public class FinalFantasySourceManager extends AbstractCopyRightAndCharacterSourceManager {
    public FinalFantasySourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }

    @Override
    public void resetBaseRoot() {
        if(new File(baseRoot+PATH_FINAL_FANTASY).exists()
                && new File(baseRoot+PATH_FINAL_FANTASY).isDirectory()
        ){


        }else{
            for (int i = 0; i < addRoots.length; i++) {
                if(new File(addRoots[i]+PATH_FINAL_FANTASY).exists()
                        && new File(addRoots[i]+PATH_FINAL_FANTASY).isDirectory()
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

    public static final String PATH_FINAL_FANTASY = "sankaku_final_fantasy_series";
    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return null;
    }

    @Override
    public void extractAllFileNames(Set<String> names) {

    }
}
