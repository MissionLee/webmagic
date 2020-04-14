package pers.missionlee.webmagic.spider.newsankaku.task;

import pers.missionlee.webmagic.spider.newsankaku.source.NewSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-14 10:54
 * 文件层级（存储策略）
 * - sankaku_doa
 * - 角色名称/组合
 * 下载策略：
 * 通过特定组合大概筛选下载目标
 * 每次以 update 模式运行
 * 排除重复策略
 * - 对于已经下载到 ARTIST 文件夹里面的，采取
 * <p>
 * // TODO: 2020-04-14
 * king of fighters - 拳皇  不知火舞 麻宫雅典娜
 * fatal fury  - 饿狼传说
 * street fighter - 街头霸王
 * art of fighting/ryuko no ken - 龙虎之拳
 * samurai spirits - 侍魂
 * <p>
 * 主系列 (3D格鬥遊戲系列)
 * <p>
 * ==========死或生5
 * Dead or Alive（1996）(Sega Saturn 1997) (PlayStation 1998)
 * Dead or Alive 2 （1999）
 * Dead or Alive 3 （2002）
 * Dead or Alive 4 （2006）
 * Dead or Alive 5 （2012）
 * Dead or Alive 6（2019）
 * ==========外傳和相關作品
 * Dead or Alive++ （1998）
 * DOA2: Hardcore （2000）
 * Dead or Alive Ultimate （2004）
 * Dead or Alive Online (2007年7月3日~2011年2月16日)(與盛大網絡合作，由盛大在大陸運營，現已停服)
 * Girls of DOA BlackJack
 * Dead or Alive: Dimensions （2011）
 * Dead or Alive 5+ （2013）
 * Dead or Alive 5 Ultimate （2013）
 * Dead or Alive 5 Last Round （2015）
 * ==========Dead or Alive Xtreme 系列(死或生:沙灘排球系列)
 * Dead or Alive Xtreme Beach Volleyball (2003)
 * Dead or Alive Xtreme 2 - Dead or Alive Paradise (2006)
 * Dead or Alive Xtreme 3 Fortune(PS4版)、Venus(PSV版) (2016)
 * Dead or Alive Xtreme Venus Vacation (2017)（死或生沙灘排球:維納斯假期）（PC端網遊）
 * Dead or Alive Xtreme 3: Scarlet (2019)
 * <p>
 * 阅读更多：死或生系列（https://zh.moegirl.org/%E6%AD%BB%E6%88%96%E7%94%9F%E7%B3%BB%E5%88%97）
 * 本文引自萌娘百科(https://zh.moegirl.org)，文字内容默认使用《知识共享 署名-非商业性使用-相同方式共享 3.0》协议。
 */
public class DOATaskController extends AbstractTaskController {
    private static List<String> characters = new ArrayList<>();
    private static List<String> coryRights = new ArrayList<>();
    private static List<String> searchTag = new ArrayList<>();
    static {
        // === DOA 1
        characters.add("kasumi (dead or alive)");// Kasumi（かすみ，霞），初次登场：DOA，配音：丹下樱（2代之前），桑岛法子（3代开始）
        characters.add("tina armstrong");// Tina Armstrong（ティナ・アームストロング，蒂娜），初次登场：DOA，配音：小山末美（1代），永岛由子（2代开始）。
        characters.add("lei fang");// Lei-Fang（レイ・ファン，雷芳/雷芳），初次登场：DOA，配音：冬马由美。
        // === DOA ++
        characters.add("ayane (dead or alive)");// Ayane（あやね，绫音），初次登场：DOA++，配音：山崎和佳奈。 忍龙主角
        // === DOA 2 Dead or Alive Ultimate(1/2的重置)
        characters.add("helena douglas");// Helena Douglas（エレナ，海莲娜），初次登场：DOA2，配音：小山裕香。
        // === DOA 3
        characters.add("hitomi (dead or alive)");//　Hitomi（ヒトミ，瞳），初次登场：DOA3，配音：堀江由衣。
        characters.add("christie (dead or alive)");// Christie Allen（クリスティ，克丽丝蒂），初次登场：DOA3，配音：三石琴乃。
        // === DOA 4
        characters.add("kokoro (dead or alive)");// Kokoro（こころ，心），初次登场：DOA4，配音：川澄绫子。
        characters.add("la mariposa (dead or alive) ");//DOA XTREME中的Lisa // 　La-Mariposa（蝴蝶），初次登场：DOA4，配音：坂本真绫 。
        characters.add("lisa hamilton");//莉莎·汉密尔顿
        characters.add("alpha-152");// alpha-152 霞的克隆人 4代boss
        // === DOA 5 5U 5L
        characters.add("nyotengu");//女天狗
        characters.add("honoka (dead or alive)"); // 穗乃果(ほのか) 声优:野中蓝
        characters.add("mila (dead or alive)");// Mila（米拉），初次登场：DOA5，配音：白石凉子 。
        characters.add("marie rose");//Marie Rose（マリー・ローズ，玛莉・萝丝），初次登场：DOA5U Arcade，配音：相泽舞 。
        characters.add("sarah bryant");// Sarah Bryant（サラ・ブライアント，莎菈・布莱恩），初次登场：DOA5
        characters.add("phase-4");// PHASE-4（フェーズ4） 声优：桑岛法子 霞的克隆人 Kasumi Alpha Phase 4
        characters.add("pai chan");//[VR战士] Pai Chan（パイ・チェン，陈佩），初次登场：DOA5，配音：高山みなみ
        characters.add("sarah bryant");//[VR战士]　Sarah Bryant（サラ・ブライアント，莎菈・布莱恩），初次登场：DOA5，配音：Lisle Wilkerson 。
        characters.add("rachel (ninja gaiden)"); //[忍者龙剑传] Rachel（瑞秋），初次登场：DOA5U，配音：富沢美智恵 。
        characters.add("momiji (ninja gaiden)"); //[忍者龙剑传] Momiji（红叶），初次登场：DOA5U，配音：皆口裕子 。
        characters.add("ii naotora (sengoku musou)");//[战国无双] 井伊直虎
        // === DOA 6
        characters.add("nico (dead or alive)");// NICO 死或生6
        characters.add("shiranui mai"); //[街头霸王] 不知火舞 联动
        characters.add("kula diamond");// [街头霸王] 库拉·戴雅萌多（库拉·戴尔蒙多）联动
        // === 沙滩排球/女神假期
        characters.add("tamaki (dead or alive)");// 环 游戏《死或生沙滩排球：女神假期》 《死或生：沙滩排球3 》系列中登场的女角色；游戏《死或生6》中登场的女角色。
        characters.add("tamaki (doa)");// 环 游戏《死或生沙滩排球：女神假期》 《死或生：沙滩排球3 》系列中登场的女角色；游戏《死或生6》中登场的女角色。
        characters.add("misaki (dead or alive)");//海咲（Misaki）
        characters.add("fiona (dead or alive)");//菲奥娜 游戏《死或生：沙滩排球》系列中女角色
        characters.add("luna (dead or alive)");// Luna  死或生：沙滩排球 女神假期
        characters.add("sayuri (dead or alive)");// 小百合（Sayuri） 死或生：沙滩排球 女神假期
        characters.add("kanna (dead or alive)");// 死或生：沙滩排球 女神假期
        characters.add("nagisa (dead or alive)");//渚(Nagisa)。她是美咲的姐姐
        characters.add("miyako (dead or alive)");//Kokoro的老媽 美夜子(Miyako)是TECMO所创作的游戏《死或生》及其衍生作品的登场角色。对战不可操作人物
        // ==========================================
        coryRights.add("dead or alive");
        coryRights.add("dead or alive xtreme venus vacation");
        coryRights.add("dead or alive xtreme");
//        coryRights.add("dead or alive 4");
//        coryRights.add("dead or alive 5");
//        coryRights.add("dead or alive 5 last round");
//        coryRights.add("dead or alive 6");
//        coryRights.add("dead or alive xtreme beach volleyball");
//        coryRights.add("dead or alive xtreme 2");
//        coryRights.add("dead or alive xtreme 3 fortune");
//        coryRights.add("ninja gaiden");//忍者龙剑传
//        coryRights.add("ninja gaiden sigma");//忍者龙剑传Σ
//        coryRights.add("ninja gaiden sigma 2 ");//忍者龙剑传Σ 2
//        coryRights.add("ninja gaiden dragon sword");//忍者龙剑传:龙剑

        searchTag.add("3d");
        searchTag.add("");
    }

    public DOATaskController(NewSourceManager newSourceManager) {
        super(newSourceManager);
    }

    /**
     * 1.
     */
    @Override
    public String[] getStartUrls() {
        if (startUrls != null) { // 如果通过 setStartUrls 配置了，就下载指定的

        } else if (workMode == WorkMode.UPDATE) {

        } else if (workMode == WorkMode.UPDATE_ALL || workMode == WorkMode.NEW) {

        }
        return new String[0];
    }

    @Override
    public boolean storeFile(File tempFile, String fileName, ArtworkInfo artworkInfo, boolean infoOnly) {
        return false;
    }

    @Override
    public Boolean existOnDisk(String filename) {
        return null;
    }
}
