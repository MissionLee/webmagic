package pers.missionlee.webmagic.spider.newsankaku.source.series;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
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
 * @create: 2020-04-15 14:00
 */
public class DOASourceManager extends AbstractCopyRightAndCharacterSourceManager {
    public DOASourceManager(String baseRoot, String... roots) {
        super(baseRoot, roots);
    }

    @Override
    public void resetBaseRoot() {
        // 判断 official 目录在拿个目录下面
        if(new File(baseRoot+PATH_DOA_SERIES).exists()
                && new File(baseRoot+PATH_DOA_SERIES).isDirectory()
        ){


        }else{
            for (int i = 0; i < addRoots.length; i++) {
                if(new File(addRoots[i]+PATH_DOA_SERIES).exists()
                        && new File(addRoots[i]+PATH_DOA_SERIES).isDirectory()
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

    @Override
    public String getAimDic(TaskController controller, ArtworkInfo info) {
        return getAimDicOfCopyRight(controller,info,PATH_DOA_SERIES);

    }

    @Override
    public void extractAllFileNames(Set<String> names) {
        // H:\ROOT\sankaku_doa_series\pic
        String pic_path = PathUtils.buildPath(baseRoot,PATH_DOA_SERIES,PATH_PIC);
        String vid_path = PathUtils.buildPath(baseRoot,PATH_DOA_SERIES,PATH_VID);
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
