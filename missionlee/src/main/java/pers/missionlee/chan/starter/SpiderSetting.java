package pers.missionlee.chan.starter;

import pers.missionlee.chan.service.DiskService;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2021-04-10 17:10
 *  {
 *     "base":"I://ROOT",
 *     "bases":"I://ROOT,H://ROOT,I://ROOT-整理,I://ROOT-特别,H://ROOT-3D图片,H://ROOT-视频",
 *     "namePairs":"I://ROOT/name-change.txt",
 *     "chromePath":"C:\\\\Documents and Settings\\\\superli\\\\AppData\\\\Local\\\\Google\\\\Chrome\\\\User Data\\\\Default\\\\Bookmarks",
 *     "chromeNewDownloadLimit":"1000000",
 *     "works":"9-1,3-sirosoil,5-0,5-1,5-2,5-3",
 *     "说明":[
 *         "base  HttpClientGenerator 基础目录，bases用于扩充存储盘,9-1,5-0,5-1,5-2,5-3    bikini armor    genderswap",
 *         "namePairs 有些作者名字有特殊符号，不能做路径",
 *         "chromePath chrome 浏览器的bookmark文件目录",
 *         "chromeNewDownloadLimit 有些作者作品太多了，暂时跳过下载 仅仅用于 命令 1-xxx  downLoadChromeArtistDir",
 *         "san1-8有些有问题，已经人工完全处理完成了，不需要再管",
 *     "任务类型 在以下字符串里面选一个（多个任务   分隔）   zol   ",
 *     "1-san7 1表示下载chrome收藏夹，san7 表示收藏见目录名字 ",
 *     "2-league of legends 2表示下载official作品，league of legends 是系列名字 ",
 *     "3-sank：3表示全部下载某个特定作者，sank是作者名字 ",
 *     "4-doa 4表示 下载某个系列的作品，doa是系列名字，现在支持的系列有： ",
 *     "5-3 5表示=普通-按时间=更新现有内容，3表示更新最低等级 ",
 *     "6-3 6表示=强制-按时间=更新现有内容=10页（200）=，3表示更新最低等级 ",
 *     "7-3 7表示=普通-忽略时间=更新现有内容，3表示更新最低等级 ",
 *     "8-3 8表示=强制-忽略时间=更新现有内容=10页（200）=，3表示更新最低等级 ",
 *     "9-1 9表示=特殊功能：1.更新作者目录信息，查找重复文件夹 ",
 *                        "2.更新作品存在情况 ",
 *                        "3.将 delete目录下的作品标记为删除 taimanin (series)",
 *                        "4.将lost 文件目录下的作品标记为删除(用于发现损坏文件后，以后可以再次下载) ",
 *                        "2-final fantasy xiv,2-genshin impact,2-league of legends,2-dead or alive,2-auzr lane,2-overwatch",
 *                        "2-destiny child,2-arknights,2-girls frontline,2-epic7,",
 *                        "2-final fantasy vii remake,2-shadowverse,2-queen's blade,2-last origin,2-blade & soul,2-utawarerumono,2-soul worker,2-fire emblem,2-bayonetta,2-legend of the cryptids,2-chaos drive,2-chaos online,2-furyou michi ~gang road~,2-honkai impact 3,2-guns girlz (series) ,2-guns girl - honkai gakuen,2-houkai 3rd,2-granblue fantasy,2-dragalia lost,2-manaria friends,2-rage of bahamut,2-carole & tuesday,2-spice and wolf,2-fate/grand_order",
 *                        "grimhelm现有作品dark star (game)和alien quest eve xxxxx: 124renka | H:/ROOT-视频/sankaku/V-3-DLive2D与动漫/ | H:/ROOT/sankaku/V-3-0可以保留更新/ xxxxx: 3dimmanimations | H:/ROOT-视频/sankaku/V-3-DLive2D与动漫/ | H:/ROOT/sankaku/V-3-0可以保留更新/3dimmanimations "
 *
 *     ]
 * }
 */
public class SpiderSetting {
    public static int sleep429 = 180;
    public static int sleep502 = 30;

    public static int getSleep502() {
        return sleep502;
    }

    public static void setSleep502(int sleep502) {
        SpiderSetting.sleep502 = sleep502;
    }

    public static int getSleep429() {
        return sleep429;
    }

    public static void setSleep429(int sleep429) {
        SpiderSetting.sleep429 = sleep429;
    }
    public boolean onlyClean;
    public boolean onlyTryTen;
    public boolean smartShutDown;
    public String __cfduid;
    public String cf_chl_2;
    public String cf_chl_prog;
    public String cf_clearance;
    public String userAgent;
    public String[] cookieString;
    public String artistBase; // 基础文件目录 （会放置临时文件，作为默认存放位置）
    public String[] normalAddArtistBases; //
    public String bookParentArtistBase;
    public String copyrightBase;//
    public String studioBase;
    public String normalSingleBase;
    public String singleBookParentBase;
    public String namePairs; // 文件名称映射文件路径
    public String chromePath; // chrome 书签文件路径
    public int downloadLimit; // 如果指定下载 ，多个这个数量限制会不进行下载
    public String[] works;
    public int threadNum;
    public int retryTime;
    public String[] removedTagForPath; // 用于single下载的时候，有一些特殊的 的tag ，不用于 创建路径 例如 2b 是个没有正常识别的简称
    public String[] tips;
    public boolean onlyArtist; // 只下载作者部分
    // book相关
    public boolean autoBook; // 用于更新作者后，如果作者在 BP类型文件夹下，是否自动以 book方式下载
    public double autoBookSkipPercent; // ！！！注意，和下面的参数逻辑相反！！！ book 的完整度高于这个数值，跳过下载
    public double bookSkipPercent;//！！！ 注意，和上面的参数逻辑相反！！！当以特定 book模式下载的时候，如果一个book缺失的作品数量 大于这个数字，那么下载这个book
    // parent相关
    public boolean autoParent;// 用于更新作者后，如果作者在 BP类型文件夹下，是否自动以 book方式下载
    //
    public boolean forceUpdate =false;// 按等级更新的时候，忽略更新时间，强制更新
    public boolean forceNew =false;// 按等级更新的时候，以新作的模式更新

    public String[] voiceActor;
    public String[] audioDesign;
//    public String[] animation;
    public boolean skipBookLostPage;
    public boolean downloadAllTryBest;
    public boolean bookOnlyDaiDing;
    public Map<String,String> siteCookie;
    public Map<String, List<String>> artistRelation;
    public List<String> skipParentPoolId;
    public boolean skipExistDBBook;
    public boolean skipNoneArtistBook = false;
    public boolean doGivenDirParent = false;
    public String[] giveParentDir;
    public String[] keepAlivePath;
    public boolean needKeepDiskAlive =  false;
    public boolean onlyUpdateChosenFolder = false;
    public String[] updateChosenFolder;
    public int blockSize ;
    public boolean updateByDateBest ;
    public int sleep =0;
    public String voiceActorPath;
    public boolean autoParentWhileUpdate = true;
    public boolean vipMode = false;
    public int allowedPageNum;
    public int siteRetryTimes ;
    public int siteCycleRetryTimes ;
    public int siteRetrySleepTime;
    public boolean nextMode;
    public String startName;
    public String[] singleBookIds;
    public String[] delPath;


    public boolean isUpdateStopUpdateArtist() {
        return updateStopUpdateArtist;
    }

    public void setUpdateStopUpdateArtist(boolean updateStopUpdateArtist) {
        this.updateStopUpdateArtist = updateStopUpdateArtist;
    }

    public boolean updateStopUpdateArtist;
    public String[] getDelPath() {
        return delPath;
    }

    public boolean isOnlyClean() {
        return onlyClean;
    }

    public void setOnlyClean(boolean onlyClean) {
        this.onlyClean = onlyClean;
    }

    public void setDelPath(String[] delPath) {
        this.delPath = delPath;
    }

    public String[] getSingleBookIds() {
        return singleBookIds;
    }

    public void setSingleBookIds(String[] singleBookIds) {
        this.singleBookIds = singleBookIds;
    }

    public String getStartName() {
        return startName;
    }

    public void setStartName(String startName) {
        this.startName = startName;
    }

    public boolean isNextMode() {
        return nextMode;
    }

    public void setNextMode(boolean nextMode) {
        this.nextMode = nextMode;
    }

    public int getSiteRetryTimes() {
        return siteRetryTimes;
    }

    public void setSiteRetryTimes(int siteRetryTimes) {
        this.siteRetryTimes = siteRetryTimes;
    }

    public int getSiteCycleRetryTimes() {
        return siteCycleRetryTimes;
    }

    public void setSiteCycleRetryTimes(int siteCycleRetryTimes) {
        this.siteCycleRetryTimes = siteCycleRetryTimes;
    }

    public int getSiteRetrySleepTime() {
        return siteRetrySleepTime;
    }

    public void setSiteRetrySleepTime(int siteRetrySleepTime) {
        this.siteRetrySleepTime = siteRetrySleepTime;
    }

    public int getAllowedPageNum() {
        return allowedPageNum;
    }

    public void setAllowedPageNum(int allowedPageNum) {
        this.allowedPageNum = allowedPageNum;
    }

    public boolean isVipMode() {
        return vipMode;
    }

    public void setVipMode(boolean vipMode) {
        this.vipMode = vipMode;
    }

    public boolean isAutoParentWhileUpdate() {
        return autoParentWhileUpdate;
    }

    public void setAutoParentWhileUpdate(boolean autoParentWhileUpdate) {
        this.autoParentWhileUpdate = autoParentWhileUpdate;
    }

    public int getSleep() {
        return sleep;
    }

    public String getVoiceActorPath() {
        return voiceActorPath;
    }

    public void setVoiceActorPath(String voiceActorPath) {
        this.voiceActorPath = voiceActorPath;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public boolean isUpdateByDateBest() {
        return updateByDateBest;
    }

    public void setUpdateByDateBest(boolean updateByDateBest) {
        this.updateByDateBest = updateByDateBest;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public String[] getUpdateChosenFolder() {
        return updateChosenFolder;
    }

    public void setUpdateChosenFolder(String[] updateChosenFolder) {
        this.updateChosenFolder = updateChosenFolder;
    }

    public boolean isOnlyUpdateChosenFolder() {
        return onlyUpdateChosenFolder;
    }

    public void setOnlyUpdateChosenFolder(boolean onlyUpdateChosenFolder) {
        this.onlyUpdateChosenFolder = onlyUpdateChosenFolder;
    }

    public boolean isNeedKeepDiskAlive() {
        return needKeepDiskAlive;
    }

    public void setNeedKeepDiskAlive(boolean needKeepDiskAlive) {
        this.needKeepDiskAlive = needKeepDiskAlive;
    }

    public String[] getKeepAlivePath() {
        return keepAlivePath;
    }

    public void setKeepAlivePath(String[] keepAlivePath) {
        this.keepAlivePath = keepAlivePath;
    }

    public boolean isDoGivenDirParent() {
        return doGivenDirParent;
    }

    public void setDoGivenDirParent(boolean doGivenDirParent) {
        this.doGivenDirParent = doGivenDirParent;
    }

    public String[] getGiveParentDir() {
        return giveParentDir;
    }

    public void setGiveParentDir(String[] giveParentDir) {
        this.giveParentDir = giveParentDir;
    }

    public boolean isSkipExistDBBook() {
        return skipExistDBBook;
    }

    public void setSkipExistDBBook(boolean skipExistDBBook) {
        this.skipExistDBBook = skipExistDBBook;
    }

    public boolean isSkipNoneArtistBook() {
        return skipNoneArtistBook;
    }

    public void setSkipNoneArtistBook(boolean skipNoneArtistBook) {
        this.skipNoneArtistBook = skipNoneArtistBook;
    }

    public Map<String, String> getSiteCookie() {
        return siteCookie;
    }

    public List<String> getSkipParentPoolId() {
        return skipParentPoolId;
    }

    public void setSkipParentPoolId(List<String> skipParentPoolId) {
        this.skipParentPoolId = skipParentPoolId;
    }

    public void setSiteCookie(Map<String, String> siteCookie) {
        this.siteCookie = siteCookie;
    }

    public boolean isBookOnlyDaiDing() {
        return bookOnlyDaiDing;
    }

    public void setBookOnlyDaiDing(boolean bookOnlyDaiDing) {
        this.bookOnlyDaiDing = bookOnlyDaiDing;
    }

    public String getRelationName(String name){
        if(artistRelation.containsKey(name)){
            return name;
        }else{
            AtomicReference<String> relName =new AtomicReference<>();
            relName.set(name);
            artistRelation.forEach((reName,namelist)->{
                if(namelist.contains(name)){
                    relName.set(reName);
                }
            });
            return relName.get().toLowerCase();
        }
    }
    public Map<String,String> initAllRelatedStoredFilesMd5(DiskService diskService,String name){

        Set<String> names = new HashSet<>();
        String realName = getRelationName(name); // 真名
        names.add(realName);
        if(artistRelation.containsKey(realName)){
            List<String> nameList = artistRelation.get(realName);
            if(nameList != null ){
                names.addAll(nameList);
            }
        }
        Map<String,String> md5s = new HashMap<>();
       names.forEach( (theName)->{
           md5s.putAll(diskService.getArtistFileMd5Path(name));
       });
        return md5s;
    } //         this.filePath = diskService.getArtistFilePath(saveArtistName);
    public Map<String,String> initAllRelatedStoredFiles(DiskService diskService,String name){
        System.out.println("目标名称："+name);
        Set<String> names = new HashSet<>();
        String realName = getRelationName(name); // 真名
        names.add(realName);
        if(artistRelation.containsKey(realName)){
            List<String> nameList = artistRelation.get(realName);
            if(nameList != null ){
                names.addAll(nameList);
            }
        }
        Map<String,String> md5s = new HashMap<>();
        names.forEach( (theName)->{
            System.out.println("MD5访问名称："+theName);
            md5s.putAll(diskService.getArtistFilePath(theName));
        });
        return md5s;
    } //         this.filePath = ;
    public int getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, List<String>> getArtistRelation() {
        return artistRelation;
    }

    public void setArtistRelation(Map<String, List<String>> artistRelation) {
        this.artistRelation = artistRelation;
    }

    public boolean isDownloadAllTryBest() {
        return downloadAllTryBest;
    }

    public void setDownloadAllTryBest(boolean downloadAllTryBest) {
        this.downloadAllTryBest = downloadAllTryBest;
    }

    public boolean isSkipBookLostPage() {
        return skipBookLostPage;
    }

    public void setSkipBookLostPage(boolean skipBookLostPage) {
        this.skipBookLostPage = skipBookLostPage;
    }

    public double getBookSkipPercent() {
        return bookSkipPercent;
    }

    public void setBookSkipPercent(double bookSkipPercent) {
        this.bookSkipPercent = bookSkipPercent;
    }

    public String[] getVoiceActor() {
        return voiceActor;
    }

    public void setVoiceActor(String[] voiceActor) {
        this.voiceActor = voiceActor;
    }

    public String[] getAudioDesign() {
        return audioDesign;
    }

    public void setAudioDesign(String[] audioDesign) {
        this.audioDesign = audioDesign;
    }

    public boolean isOnlyTryTen() {
        return onlyTryTen;
    }

    public void setOnlyTryTen(boolean onlyTryTen) {
        this.onlyTryTen = onlyTryTen;
    }

    public boolean isSmartShutDown() {
        return smartShutDown;
    }

    public void setSmartShutDown(boolean smartShutDown) {
        this.smartShutDown = smartShutDown;
    }

    public String[] getCookieString() {
        return cookieString;
    }

    public void setCookieString(String[] cookieString) {
        this.cookieString = cookieString;
    }

    public String get__cfduid() {
        return __cfduid;
    }

    public void set__cfduid(String __cfduid) {
        this.__cfduid = __cfduid;
    }

    public String getCf_chl_2() {
        return cf_chl_2;
    }

    public void setCf_chl_2(String cf_chl_2) {
        this.cf_chl_2 = cf_chl_2;
    }

    public String getCf_chl_prog() {
        return cf_chl_prog;
    }

    public void setCf_chl_prog(String cf_chl_prog) {
        this.cf_chl_prog = cf_chl_prog;
    }

    public String getCf_clearance() {
        return cf_clearance;
    }

    public void setCf_clearance(String cf_clearance) {
        this.cf_clearance = cf_clearance;
    }

    public double getAutoBookSkipPercent() {
        return autoBookSkipPercent;
    }

    public double getBookSkipNum() {
        return bookSkipPercent;
    }

    public void setBookSkipNum(double bookSkipNum) {
        this.bookSkipPercent = bookSkipNum;
    }

    public void setAutoBookSkipPercent(double autoBookSkipPercent) {
        this.autoBookSkipPercent = autoBookSkipPercent;
    }
    public boolean isAutoParent() {
        return autoParent;
    }

    public void setAutoParent(boolean autoParent) {
        this.autoParent = autoParent;
    }

    public boolean isAutoBook() {
        return autoBook;
    }

    public void setAutoBook(boolean autoBook) {
        this.autoBook = autoBook;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public boolean isForceNew() {
        return forceNew;
    }

    public void setForceNew(boolean forceNew) {
        this.forceNew = forceNew;
    }

    public boolean isOnlyArtist() {
        return onlyArtist;
    }

    public void setOnlyArtist(boolean onlyArtist) {
        this.onlyArtist = onlyArtist;
    }

    // 数据库相关参数

    public static SpiderSetting buildSetting(){
        SpiderSetting spiderSetting = new SpiderSetting();
        spiderSetting.artistBase = "F://CHAN_BASE";
        spiderSetting.normalAddArtistBases = new String[0];
        spiderSetting.bookParentArtistBase = "F://CHAN_ARTIST_BP";
        spiderSetting.copyrightBase = "F://CHAN_COPYRIGHT";
        spiderSetting.studioBase = "F://CHAN_STUDIO";
        spiderSetting.normalSingleBase ="F://CHAN_SINGLE";
        spiderSetting.singleBookParentBase = "F://CHAN_SINGLE_BP";
        spiderSetting.threadNum = 3;
        spiderSetting.removedTagForPath = new String[0];
        spiderSetting.chromePath = "";
        spiderSetting.onlyArtist = false;
        return spiderSetting;
    }
    public String[] getRemovedTagForPath() {
        return removedTagForPath;
    }

    public void setRemovedTagForPath(String[] removedTagForPath) {
        this.removedTagForPath = removedTagForPath;
    }

    public String getArtistBase() {
        return artistBase;
    }

    public void setArtistBase(String artistBase) {
        this.artistBase = artistBase;
    }

    public String[] getNormalAddArtistBases() {
        return normalAddArtistBases;
    }

    public void setNormalAddArtistBases(String[] normalAddArtistBases) {
        this.normalAddArtistBases = normalAddArtistBases;
    }

    public String getBookParentArtistBase() {
        return bookParentArtistBase;
    }

    public void setBookParentArtistBase(String bookParentArtistBase) {
        this.bookParentArtistBase = bookParentArtistBase;
    }

    public String getCopyrightBase() {
        return copyrightBase;
    }

    public void setCopyrightBase(String copyrightBase) {
        this.copyrightBase = copyrightBase;
    }

    public String getStudioBase() {
        return studioBase;
    }

    public void setStudioBase(String studioBase) {
        this.studioBase = studioBase;
    }

    public String getNormalSingleBase() {
        return normalSingleBase;
    }

    public void setNormalSingleBase(String normalSingleBase) {
        this.normalSingleBase = normalSingleBase;
    }

    public String getSingleBookParentBase() {
        return singleBookParentBase;
    }

    public void setSingleBookParentBase(String singleBookParentBase) {
        this.singleBookParentBase = singleBookParentBase;
    }

    public String getNamePairs() {
        return namePairs;
    }

    public void setNamePairs(String namePairs) {
        this.namePairs = namePairs;
    }

    public String getChromePath() {
        return chromePath;
    }

    public void setChromePath(String chromePath) {
        this.chromePath = chromePath;
    }

    public int getDownloadLimit() {
        return downloadLimit;
    }

    public void setDownloadLimit(int downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    public String[] getWorks() {
        return works;
    }

    public void setWorks(String[] works) {
        this.works = works;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public String[] getTips() {
        return tips;
    }

    public void setTips(String[] tips) {
        this.tips = tips;
    }
}
