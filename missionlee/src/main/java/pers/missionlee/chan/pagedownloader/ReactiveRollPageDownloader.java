package pers.missionlee.chan.pagedownloader;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;

import java.io.IOException;

public class ReactiveRollPageDownloader extends MixDownloader {
    public ReactiveRollPageDownloader(String chromePath, String chromeDriverPath, String debuggingPort) throws IOException {
        super(chromePath, chromeDriverPath, debuggingPort);
    }
    @Override
    public Page download(Request request, Task task){
        return null;
    }
}
