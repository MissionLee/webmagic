package pers.missionlee.webmagic.spider.newsankaku.task.copyright;

import pers.missionlee.webmagic.spider.newsankaku.source.SourceManager;

import java.util.List;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-23 10:54
 */
public class FinalFantasyTaskController extends AbstractCopyrightAndCharacterTaskController {
    public FinalFantasyTaskController(SourceManager artistSourceManager) {
        super(artistSourceManager);
    }

    public List<String> FINAL_FANTASY_I;
    public List<String> FINAL_FANTASY_II;
    public List<String> FINAL_FANTASY_III;
    public List<String> FINAL_FANTASY_IV;
    public List<String> FINAL_FANTASY_V;
    public List<String> FINAL_FANTASY_VI;
    public List<String> FINAL_FANTASY_VII;
    public List<String> FINAL_FANTASY_VIII;
    public List<String> FINAL_FANTASY_IX;
    public List<String> FINAL_FANTASY_X;
    public List<String> FINAL_FANTASY_XI;
    public List<String> FINAL_FANTASY_XII;
    public List<String> FINAL_FANTASY_XIV;
    public List<String> FINAL_FANTASY_XV;

    public void init() {
        // 最终幻想 6
        FINAL_FANTASY_VI.add("tina branford"); // ティナ･ブランフォード/ Tina Branford（日）Terra Branford（英）/ 蒂娜
        // celes chere セリス･シェール/ （General）Celes Chère / 莎莉丝
        // relm arrowny リルム･アローニィ/ Relm Arrowny / 丽姆

        // 最终幻想 7
        // https://myanimelist.net/anime/317/Final_Fantasy_VII__Advent_Children
        //
        // crisis_core_final_fantasy_vii 核心危机
        // before_crisis_final_fantasy_vii 危机之前
        // final_fantasy_vii 正传
        // final fantasy vii: advent children 再临之子/圣子降临
        // dirge_of_cerberus_final_fantasy_vii  地狱犬的挽歌 ps2 游戏
        // last_order:_final_fantasy_vii 最终密令 外传动画
        // final_fantasy_vii:_last_order 最终密令
        // final fantasy vii remake  重置

        // tifa lockhart 蒂法·洛克哈特
        // aerith gainsborough  艾莉丝·盖恩斯巴勒
        // yuffie kisaragi 尤菲·如月 尤菲·如月（ユフィ キサラギ/Yuffie Kisaragi）说明：忍着的末裔。来自世界最西端的乌泰城，活泼开朗，善耍小聪明。酷爱收集魔晶石。与克劳德等人相遇结伴起初是为了找机会偷走他们的魔晶石，后来觉得自己没有胜算，就以“受人委托”为由半强硬地加入了他们同伴的行列。在共同旅行的过程中，她的同伴意识开始渐渐地产生.
        // marlene wallace 玛琳 玛莲·华莱士（マリン ウォーレス/Marlene Wallace） 说明：巴雷特收养的义女，生父是Barret的好友戴恩。从小寄养在蒂法的第七天堂。聪明伶俐，性格中继承了一些巴雷特和蒂法的大胆、果断。
        // elena (ff7) 伊丽娜（イリーナ/Elena） 说明：Turks队员之一，喜欢Tseng。因为年纪和入队较晚的关系喜欢叫其他队员“前辈”。
        // jenova  杰诺瓦（Jenova） 在古代随着陨石而降临星球的外星生物。

        // 最终幻想8
        // rinoa heartilly 莉诺雅·哈蒂莉（リノア ・ハーティリー、Rinoa Heartilly）
        //  quistis trepe奇丝迪丝·萃普（キスティス・トゥリープ、Quistis Trepe）
        // selphie tilmitt  赛尔菲·提尔米多（セルフィ ティルミット、Selphie Tilmitt）
        //  edea kramer 伊迪雅·克莱默（イデア、EdeaKramer）
        // ultimecia 阿尔提米西亚（アルティミシア、Ultimecia） BOSS
        // fujin 日文：风神 　年龄：17
        // ellone 日文：エルオーネ Laguna和Raine的养女，拥有把他人的意识送往过去的能力。游戏中的梦中世界
        //没有 raine 玲 Laguna的妻子，由于照顾受伤的Laguna，与Laguna相识相爱。
        //没有 英文：Julia 日文：ジュリア Rinoa的母亲，歌星。早年曾在酒吧弹钢琴，与Laguna有过一段恋情。

        // 最终幻想9
        // garnet til alexandros xvii
        // eiko carol
        // freya crescent
        // vivi ornitier 人造小黑魔导
        // beatrix (ff9)  贝特丽克丝 （Beatrix） 年龄：28岁        说明：就任于亚历山德里亚王室的美丽女将军
        // beatrix (final fantasy ix)
        // ruby (ff9)  说明：坦特拉斯的当红明星。健康活泼，有一种大姐大的气质，感情起伏很大。和坦特拉斯的其他成员不同，似乎是从乡下进城来的，但她的出身和年龄都是个谜。不知道是不是受到她出生地风俗的影响，她使用的是别人模仿不了的独特的说话方式。
        // cinna  说明：坦特拉斯的一员，出生于林德布尔姆的飞空艇技师家庭，自身也有着当技师的本领。剧场艇的引擎是他的处女作，在坦特拉斯成为剧场艇的主人之时，自然而然也加入了坦特拉斯。
        // mikoto (ff9) 星命  种族：基因组    性别：女    年龄：15岁（从外观看）    说明：加兰德制造出来的基因组少女，作为那些违背加兰德意愿，并且遗弃在盖亚的基因组们的替代品，在盖亚制造战乱。为了留有成长的余地，她拥有和那些被遗弃的基因组们一样的孩童形态，作为继承过去的情感和意志的存在而诞生。
        //但是，由于在泰拉那样封闭的环境里长大，所以情感培育得很慢。因此要把她派往盖亚还需要一段时间。
        //对自身的使命不抱有任何怀疑，把自身的一切都要为泰拉奉献很自然地当成宿命并且服从。
        // lani 拉妮年龄：19岁
        //说明：和撒拉曼达一起被布拉奈雇佣，是一位女赏金猎人。长得很漂亮，大概她自己也知道这一点，所以给自己赐予了“美之猎人”、“爱之猎人”等称号。身材很苗条，但腕力却大得足以挥动巨大的斧子。
        //乍一看显得很高贵，说话也很有礼貌，但是一旦被击中要害，或者不能令她称心如意时，她马上就会大喊大叫，形象也顾不上了。一直在想尽办法攒钱，梦想着过上和贵族一样的生活。
        // eiko carol 艾可·卡萝尔 白魔导士 6岁

        // 最终幻想10
        //yuna  yuna (ff10)
        // rikku
        // lulu (final fantasy)
        // leblanc (ff10)
        // leblanc (final fantasy)
        // lenne 莲/莲因
        // 最终幻想10-2
        // paine  paine (ff10)

        // 最终幻想11
        //

        // 最终幻想12
        //ashelia b'nargin dalmasca 雅雪 アーシェ·バナルガン·ダルマスカ | Ashe | 阿西娅 身高 165CM 年龄：19投身抵抗运动的原王女。19岁。Dalmasca国王的8男1女9兄妹中的末子。国王49岁时所生。8位兄长因为战乱和疾病相继死去，Ashe成为王国唯一的继承者。
        // penelo パンネロ | Penelo | 潘妮罗  说明：是Vaan的像女朋友般的良好关系。对有着令人感到不安的生活方式的Vaan，引起了她的母性本能，事事都要照顾他。Penelo在帝国军的侵略引起的战乱中失去了亲人，现在一边在王都的名叫Migelo的店主经营的商店和义卖会帮忙，一边开朗积极的生活着。
        // fran (ffxii)   fran  フラン | Fran | 芙兰  说明：寿命比人类长的Viera族的女战士。拥有Viera族特有的敏锐听觉和视觉。冷静的判断周围的情况，引导同伴的可靠的存在。并且精通使用各种武器，有作为种类稀少的战士的能力。与巴尔佛雷亚一起驱使着飞空艇，作为空贼翱翔于全世界，因此也成为了高额通缉犯。
        // viera 种族

        // 最终幻想13
        //lightning farron 雷霆/Lightning（ライトニング） 雷霆全名：埃克莱尔·法隆/エクレール?ファロン/Eclair·Farron
        // lightning farron 小男孩
        //serah farron  塞拉·法隆/セラ·ファロン/Serah·Farron     说明：Lightning的妹妹，斯诺的未婚妻，曾在波达姆闲逛时“误入”异迹，被下界的法尔希·阿尼玛选中为路希并昏迷，后来被芳和香草二人发现并送到异迹外。
        //oerba yun fang 芳（ユン·ファング） 欧尔巴=云·芳/ヲルバ＝ユン·ファング/Oerba·Yun·Fang 说明：“想要活命的话，你就老实点！”第7名路希，黑发充满野性的女子。右手上臂有烙印，是对茧的居民充满威胁的露西，却与圣府军共同行动。个性强悍，豪爽而不拘小节，左臂上刺有獠牙般的刺青。值得注意的是她与香草同姓，着装也充满名族风，在几百年前曾与香草是古兰·下界的路希，在下界完成了使命，与香草一同成为水晶，一直沉睡在神殿的路希之间中，直到几百年后与香草再接到法尔希的使命并且苏醒，醒来后便发现自己已在茧中。
        // oerba dia vanille 全名：欧尔巴＝戴亚·香草/ヲルバ＝ダイア·ヴァニラ/Oerba· Dia·Vanille   ◇“下界的法尔希——我们就是在那里与命运相会了。” 红色双马尾，着装充满民族风，给人以独特感觉的少女。尽管在同伴面前总是表现出一幅活泼开朗的样子，可内心却暗藏着面对残酷命运的强烈决心。充满谜团的少女，在几百年前曾与芳是古兰·下界的路希，在下界完成了使命，与芳一同成为水晶，一直沉睡在神殿的路希之间中，直到几百年后与芳再接到法尔希的使命并且苏醒，醒来后便发现自己已在茧中，承担着下界法尔希赋予的某种非常重大的责任，因为与塞拉年龄相近所以比较合得来，同时似乎是因为她而导致塞拉萨兹的儿子遇难。在故事中担任重要角色和线索。
        //jihl nabaat 吉尔（ジル） 吉尔·娜巴特中校/ジル·ナバート中佐/Jihl·Nabaat    “要小心看管，这可是用来安定茧的贵重道具哦。”统率圣府军精锐部队“PSICOM”的管理者。迷人的外表下却影藏着难以想象的冷酷与残忍，不把露西当人看。只要能让茧的伤害最小化，甚至能将“流放政策”正当化，毫无顾忌的实行。

        // 最终幻想14
        // 种族
        // miqo'te 猫
        //au ra 龙
        // lalafell 拉拉肥
        // hyur 人
        //elezen 精灵
        // viera 兔
        // sin eater 食罪灵
        //roegadyn 鲁加
        // padjal 角尊
        // 重生之境
        //kan-e-senna 嘉恩艾神纳
        // raya-o-senna
        // minfilia 敏菲利亚
        //minfilia warde
        // yda hext  伊达
        //thancred waters  桑科瑞德
        // urianger augurelt 于里昂热
        //papalymo totolymo  帕帕力默
        //y'shtola rhul    Y'shtola（雅.修特拉）：Yang Menglu（杨梦露）
        // 苍天龙骑
        // alisaie leveilleur 阿莉塞
        //alphinaud leveilleur   Alphinaud 阿尔菲诺
        // 红莲解放者
        //lyse hext     lyse 莉瑟
        //  yotsuyu (final fantasy xiv)       Yotsuyu（夜露）：Hong Haitian（洪海天）
        //  yugiri mistwalker        Yugiri（夕雾）：Fan Churong（范楚绒）
        // fordola rem lupis        Fordola（芙朵拉）：Jiang Li
        //krile mayer baldesion        Krile（可露儿）：Xie Yuanzhen（谢元真）
        //cirina mol         Cirina（其日娜）：Tang Suling（唐夙凌）
        // sadu dotharl         Sadu（纱都）：Li Ye（李晔）
// m'naago        M'Naago（梅.娜格）：Qin Ziyi
        // 暗黑反叛者
        //gaia (ff14) 暗之女巫
        // ryne 琳【敏菲利亚】

// 同人角色
//        sabrith ebonclaw  猫
//        tayelle ebonclaw  猫
//        angelise reiter 好像是人
        // sahna 猫
        //ylva nighthawk 兔子

        // 蛮神
        //   garuda (final fantasy xiv) 迦楼罗 风神
        // lakshmi (final fantasy) 美神
        //titania (final fantasy xiv)  提坦尼亚
        // 十二神
        // nophica

        // 职业
        // red mage
        // white mage (final fantasy xiv)

    }

    @Override
    public String[] getStartUrls() {
        return new String[0];
    }
}
