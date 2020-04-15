package pers.missionlee.webmagic.spider.newsankaku.task;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import pers.missionlee.webmagic.spider.newsankaku.source.ArtistSourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.DOASourceManager;
import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;
import pers.missionlee.webmagic.spider.newsankaku.type.WorkMode;
import pers.missionlee.webmagic.spider.newsankaku.utlis.SpiderUtils;
import pers.missionlee.webmagic.spider.sankaku.info.ArtworkInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public static List<String> DEFAULT_CHARACTERS = new ArrayList<>();
    public static List<String> DEFAULT_COPYRIGHTS = new ArrayList<>();
    public static List<String> DEFAULT_TAGS = new ArrayList<>();
    private String copyRight;
    private String character;
    private String[] searchTags;

    static {
        // === DOA 1
        DEFAULT_CHARACTERS.add("kasumi (dead or alive)");// Kasumi（かすみ，霞），初次登场：DOA，配音：丹下樱（2代之前），桑岛法子（3代开始）
        DEFAULT_CHARACTERS.add("tina armstrong");// Tina Armstrong（ティナ・アームストロング，蒂娜），初次登场：DOA，配音：小山末美（1代），永岛由子（2代开始）。
        DEFAULT_CHARACTERS.add("lei fang");// Lei-Fang（レイ・ファン，雷芳/雷芳），初次登场：DOA，配音：冬马由美。
        // === DOA ++
        DEFAULT_CHARACTERS.add("ayane (dead or alive)");// Ayane（あやね，绫音），初次登场：DOA++，配音：山崎和佳奈。 忍龙主角
        // === DOA 2 Dead or Alive Ultimate(1/2的重置)
        DEFAULT_CHARACTERS.add("helena douglas");// Helena Douglas（エレナ，海莲娜），初次登场：DOA2，配音：小山裕香。
        // === DOA 3
        DEFAULT_CHARACTERS.add("hitomi (dead or alive)");//　Hitomi（ヒトミ，瞳），初次登场：DOA3，配音：堀江由衣。
        DEFAULT_CHARACTERS.add("christie (dead or alive)");// Christie Allen（クリスティ，克丽丝蒂），初次登场：DOA3，配音：三石琴乃。
        // === DOA 4
        DEFAULT_CHARACTERS.add("kokoro (dead or alive)");// Kokoro（こころ，心），初次登场：DOA4，配音：川澄绫子。
        DEFAULT_CHARACTERS.add("la mariposa (dead or alive) ");//DOA XTREME中的Lisa // 　La-Mariposa（蝴蝶），初次登场：DOA4，配音：坂本真绫 。
        DEFAULT_CHARACTERS.add("lisa hamilton");//莉莎·汉密尔顿
        DEFAULT_CHARACTERS.add("alpha-152");// alpha-152 霞的克隆人 4代boss
        // === DOA 5 5U 5L
        DEFAULT_CHARACTERS.add("nyotengu");//女天狗
        DEFAULT_CHARACTERS.add("honoka (dead or alive)"); // 穗乃果(ほのか) 声优:野中蓝
        DEFAULT_CHARACTERS.add("mila (dead or alive)");// Mila（米拉），初次登场：DOA5，配音：白石凉子 。
        DEFAULT_CHARACTERS.add("marie rose");//Marie Rose（マリー・ローズ，玛莉・萝丝），初次登场：DOA5U Arcade，配音：相泽舞 。
        DEFAULT_CHARACTERS.add("sarah bryant");// Sarah Bryant（サラ・ブライアント，莎菈・布莱恩），初次登场：DOA5
        DEFAULT_CHARACTERS.add("phase-4");// PHASE-4（フェーズ4） 声优：桑岛法子 霞的克隆人 Kasumi Alpha Phase 4
        DEFAULT_CHARACTERS.add("pai chan");//[VR战士] Pai Chan（パイ・チェン，陈佩），初次登场：DOA5，配音：高山みなみ
        DEFAULT_CHARACTERS.add("sarah bryant");//[VR战士]　Sarah Bryant（サラ・ブライアント，莎菈・布莱恩），初次登场：DOA5，配音：Lisle Wilkerson 。
        DEFAULT_CHARACTERS.add("rachel (ninja gaiden)"); //[忍者龙剑传] Rachel（瑞秋），初次登场：DOA5U，配音：富沢美智恵 。
        DEFAULT_CHARACTERS.add("momiji (ninja gaiden)"); //[忍者龙剑传] Momiji（红叶），初次登场：DOA5U，配音：皆口裕子 。
        DEFAULT_CHARACTERS.add("ii naotora (sengoku musou)");//[战国无双] 井伊直虎
        // === DOA 6
        DEFAULT_CHARACTERS.add("nico (dead or alive)");// NICO 死或生6
        DEFAULT_CHARACTERS.add("shiranui mai"); //[街头霸王] 不知火舞 联动
        DEFAULT_CHARACTERS.add("kula diamond");// [街头霸王] 库拉·戴雅萌多（库拉·戴尔蒙多）联动
        // === 沙滩排球/女神假期
        DEFAULT_CHARACTERS.add("tamaki (dead or alive)");// 环 游戏《死或生沙滩排球：女神假期》 《死或生：沙滩排球3 》系列中登场的女角色；游戏《死或生6》中登场的女角色。
        DEFAULT_CHARACTERS.add("tamaki (doa)");// 环 游戏《死或生沙滩排球：女神假期》 《死或生：沙滩排球3 》系列中登场的女角色；游戏《死或生6》中登场的女角色。
        DEFAULT_CHARACTERS.add("misaki (dead or alive)");//海咲（Misaki）
        DEFAULT_CHARACTERS.add("fiona (dead or alive)");//菲奥娜 游戏《死或生：沙滩排球》系列中女角色
        DEFAULT_CHARACTERS.add("luna (dead or alive)");// Luna  死或生：沙滩排球 女神假期
        DEFAULT_CHARACTERS.add("sayuri (dead or alive)");// 小百合（Sayuri） 死或生：沙滩排球 女神假期
        DEFAULT_CHARACTERS.add("kanna (dead or alive)");// 死或生：沙滩排球 女神假期
        DEFAULT_CHARACTERS.add("nagisa (dead or alive)");//渚(Nagisa)。她是美咲的姐姐
        DEFAULT_CHARACTERS.add("miyako (dead or alive)");//Kokoro的老媽 美夜子(Miyako)是TECMO所创作的游戏《死或生》及其衍生作品的登场角色。对战不可操作人物
        // ==========================================
//        DEFAULT_COPYRIGHTS.add("dead or alive");
//        DEFAULT_COPYRIGHTS.add("dead or alive xtreme venus vacation");
//        DEFAULT_COPYRIGHTS.add("dead or alive xtreme");

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

        DEFAULT_TAGS.add("3d");//
//        searchTag.add("animated");
//        searchTag.add("16:9 aspect ratio");
//        searchTag.add("large filesize");
//        searchTag.add("video");
//        searchTag.add("has audio");
//        searchTag.add("mp4");
//        searchTag.add("video with sound");
//        searchTag.add("ugoira");// pixiv 的一种文件格式
//        searchTag.add("blender (software)");//三维动画制作软件
//        searchTag.add("uncensored");//无修
//        searchTag.add("high resolution");
//        searchTag.add("extremely large filesize");
//        searchTag.add("multiple views");
//        searchTag.add("high frame rate");
//        searchTag.add("webm");
//        searchTag.add("high resolution");
//        searchTag.add("very high resolution");
//        searchTag.add("screen capture");
//        searchTag.add("video with sound");
//        searchTag.add("vertical video");
//        searchTag.add("no audio");
//        searchTag.add("realistic");
    }

    public DOATaskController(SourceManager sourceManager) {
        super(sourceManager);
        init();
    }

    public DOATaskController(SourceManager sourceManager, String copyRight, String character, String... tags) {
        this(sourceManager);
        this.copyRight = copyRight;
        this.character = character;
        this.searchTags = tags;
    }
    /**
     * 初始化本地参数
     * 1.初始话已经存储的所有sanCode
     * */
    public void init(){
        storedSanCode = sourceManager.getStoredSanCode(this);
    }
    /**
     * 1.
     */
    @Override
    public String[] getStartUrls() {
        if (startUrls != null) { // 如果通过 setStartUrls 配置了，就下载指定的
            return startUrls;
        } else {
            startUrls = new String[0];
            if (StringUtils.isEmpty(copyRight) && StringUtils.isEmpty(copyRight)) { // 如果没有给定特定的 作品和人物，就遍历默认内容
                if (workMode == WorkMode.UPDATE) {
                    for (String copyRight : DEFAULT_COPYRIGHTS
                    ) {
                        ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, copyRight, "3d"));
                        ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, copyRight, "video"));
                    }
                    for (String character : DEFAULT_CHARACTERS) {
                        ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, character, "3d"));
                        ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, character, "video"));
                    }
                } else if (workMode == WorkMode.UPDATE_ALL || workMode == WorkMode.NEW) {
                    // 主要作品标签+3d 起始页
                    for (String copyRight1 :
                            DEFAULT_COPYRIGHTS) {
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_DEC, copyRight1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_ASC, copyRight1, "3d"));

                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_DEC, copyRight1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_ASC, copyRight1, "video"));
                    }
                    // 主要人物标签+3d 起始页
                    // 多数非热门标签页面很少，所以下面代码应该不会引起数据爆炸
                    for (String character1 : DEFAULT_CHARACTERS) {
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_DEC, character1, "3d"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_ASC, character1, "3d"));

                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_DEC, character1, "video"));
                        startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_ASC, character1, "video"));
                    }
                }
            } else {
                String[] keys = new String[0];
                if(!StringUtils.isEmpty(character))keys = ArrayUtils.add(keys,character);
                if(!StringUtils.isEmpty(copyRight))keys = ArrayUtils.add(keys,copyRight);
                keys = ArrayUtils.add(keys,"3d");
                if(workMode == WorkMode.UPDATE){
                    ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, keys));
                }else if(workMode == WorkMode.UPDATE_ALL || workMode == WorkMode.NEW){
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_DEC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_ASC, keys));
                }
                keys[keys.length-1] = "video";
                if(workMode == WorkMode.UPDATE){
                    ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, keys));
                }else if(workMode == WorkMode.UPDATE_ALL || workMode == WorkMode.NEW){
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.DATE, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.POPULAR, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.QUALITY, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_ASC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.TAG_COUNT_DEC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_DEC, keys));
                    startUrls = ArrayUtils.add(startUrls, SpiderUtils.getSearchUrlPageOne(SpiderUtils.OrderType.FILESIZE_ASC, keys));
                }
            }


        }
        for (int i = 0; i < startUrls.length; i++) {
            System.out.println(startUrls[i]);
        }
        return startUrls;
    }

    @Override
    public boolean confirmRel(String fullUrl) {
        return false;
    }


    @Override
    public String getNumberCheckUrl() {
        return null;
    }

    public static void main(String[] args) {
        DOASourceManager sourceManager = new DOASourceManager("H:\\ROOT", "G:\\ROOT");
        DOATaskController doaTaskController = new DOATaskController(sourceManager);
        doaTaskController.setWorkMode(WorkMode.NEW);
        System.out.println("=========================================");
//        String[] urls = doaTaskController.getStartUrls();
//        for (int i = 0; i < urls.length; i++) {
//            System.out.println(urls[i]);
//        }
        Set<String> codes = doaTaskController.storedSanCode;
        System.out.println(codes);
        System.out.println(codes.size());
    }
}
