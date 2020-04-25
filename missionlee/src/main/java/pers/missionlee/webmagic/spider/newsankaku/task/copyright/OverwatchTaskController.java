package pers.missionlee.webmagic.spider.newsankaku.task.copyright;

import org.apache.commons.lang3.ArrayUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.task.copyright.AbstractCopyrightAndCharacterTaskController;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-22 11:38
 */
public class OverwatchTaskController extends AbstractCopyrightAndCharacterTaskController {
    public OverwatchTaskController(SourceManager artistSourceManager) {
        super(artistSourceManager);
        init();
    }

    @Override
    public String[] getStartUrls() {
        String[] video = getDefaultVideoStartUrls();
        String[] pic = getDefault3DStartUrls();
        return ArrayUtils.addAll(video,pic);
    }

    public void init(){
        DEFAULT_CHARACTERS.add("d.va (overwatch)");//dva
        DEFAULT_CHARACTERS.add("mercy (overwatch) ");// 天使
        DEFAULT_CHARACTERS.add("widowmaker (overwatch)");//黑百合
        DEFAULT_CHARACTERS.add("tracer (overwatch)");//莉娜 澳克斯顿 裂空
        DEFAULT_CHARACTERS.add("mei (overwatch)");//美
        DEFAULT_CHARACTERS.add("pharah (overwatch)");//法瑞尔 法老之鹰
        DEFAULT_CHARACTERS.add("sombra (overwatch)");// sombra
//        cyberspace sombra /   sombra
        DEFAULT_CHARACTERS.add("symmetra (overwatch)");// 秩序之光
        DEFAULT_CHARACTERS.add("brigitte (overwatch)");// 布里吉塔
        DEFAULT_CHARACTERS.add("ana (overwatch)");//安娜
        DEFAULT_CHARACTERS.add("zarya (overwatch)");//查里雅
        DEFAULT_CHARACTERS.add("meka (overwatch)");// 机甲
        DEFAULT_CHARACTERS.add("ashe (overwatch)");//艾什

//        DEFAULT_COPYRIGHTS.add("overwatch");

        storedSanCode = sourceManager.getStoredSanCode(this);

    }
}
