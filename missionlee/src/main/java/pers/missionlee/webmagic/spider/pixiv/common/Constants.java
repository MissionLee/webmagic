package pers.missionlee.webmagic.spider.pixiv.common;

public class Constants {


    /**
     * P站预登陆url
     */
    public static final String PIXIV_BASE_URL = "https://accounts.pixiv.net/login?lang=en&source=pc&view_type=page&ref=wwwtop_accounts_index";

    /**
     * P站登录请求url
     */
    public static final String PIXIV_LOGIN_URL = "https://accounts.pixiv.net/api/login?lang=en";

    /**
     * P站搜索请求url
     */
    public static final String PIXIV_SEARCH_URL = "https://www.pixiv.net/search.php";

    /**
     * P站单图详情页url
     */
    public static final String PIXIV_ILLUST_MEDIUM_URL = "https://www.pixiv.net/member_illust.php?mode=medium&illust_id=";

    /**
     * P站多图详情页url
     */
    public static final String PIXIV_ILLUST_MANGA_URL = "https://www.pixiv.net/member_illust.php?mode=manga&illust_id=";
    /**
     * P站 获取作者作品列表 url
     * user-id 需要替换为 艺术家id
     */
    public static final String PIXIV_ARITST_ALL_AJAX = "https://www.pixiv.net/ajax/user/USER-ID/profile/all";

    /**
     * 图片本地保存根目录
     */
    public static final String IMG_DOWNLOAD_BASE_PATH = "C:\\Users\\MissionLee\\Desktop\\img";

    /**
     * 用户名
     */
    public static final String USERNAME = "zuixue3000";

    /**
     * 密码
     */
    public static final String PASSWORD = "shunzhiai";

    /**
     * 搜索关键词
     */
    public static final String KEY_WORD = "双子";

    /**
     * 是否只搜索r18结果
     */
    public static final boolean IS_R18 = true;

    /**
     * 点赞数（不低于）
     */
    public static final int STARS = 1000;
    /**
     * https 反向代理 证书 keystore格式文件地址
     * */
    public static final String KEY_SOTRE = "C:\\Program Files\\Pixiv-Nginx-master\\pixiv.keystore";
    /**
     * https 反向代理 keystore 密钥
     * */
    public static final String KEY_STORE_KEY = "589454";

}
