package pers.missionlee.webmagic.spider.newsankaku.dao;

import com.alibaba.fastjson.JSON;
import com.sun.istack.internal.Nullable;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2020-04-19 18:03
 */
public class LevelInfo {
        public int artistId;
        public String name;
        public int picLevel = 10;
        public int vidLevel = 10;
        public String picPath = "no_path";
        public String vidPath = "no_path";

        public LevelInfo(String name) {
            this.name = name;
        }

        public LevelInfo(Integer artistId, String name, @Nullable Integer picLevel,@Nullable String picPath,@Nullable Integer vidLevel,@Nullable String vidPath) {
                this.artistId = artistId;
                this.name = name;
                if(picLevel !=null)
                this.picLevel = picLevel;
                if(vidLevel != null)
                this.vidLevel = vidLevel;
                if(picPath != null)
                this.picPath = picPath;
                if(vidPath != null)
                this.vidPath = vidPath;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this.name.equals(((LevelInfo) obj).name)
                    && this.picLevel == ((LevelInfo) obj).picLevel
                    && this.vidLevel == ((LevelInfo) obj).vidLevel;
//                    && this.picPath.equals(((LevelInfo) obj).picPath)
//                    &&  this.vidPath.equals(((LevelInfo) obj).vidPath);
        }

}
