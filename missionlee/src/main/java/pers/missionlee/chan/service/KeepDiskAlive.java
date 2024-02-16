package pers.missionlee.chan.service;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class KeepDiskAlive implements Runnable {
    public static void keepAlive(String[] paths,Logger logger){
        new Thread(new KeepDiskAlive(paths,logger)).start();
    }
    public static void keepAlive(String[] paths){
        new Thread(new KeepDiskAlive(paths)).start();
    }
    Logger logger ;
    String[] diskFilePath;
    String diskString="";
    public KeepDiskAlive(String[] diskFilePath, Logger logger) {
        this.diskFilePath = diskFilePath;
        this.logger = logger;

    }
    public KeepDiskAlive(String[] diskFilePath) {
        this.diskFilePath = diskFilePath;
//        this.logger = logger;

    }
    @Override
    public void run() {
        try {
            keepDiskAlive();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public  void keepDiskAlive() throws InterruptedException, IOException {
        for (int i = 0; i < diskFilePath.length; i++) {
            FileUtils.writeStringToFile(new File(diskFilePath[i]),"1","utf-8",false);
            diskString+= diskFilePath[i]+" ";
        }
        while (true){
//            logger.info("正在通过代码，写入"+diskString+"保持磁盘活跃");
            Thread.sleep(1000*60);
            for (int i = 0; i < diskFilePath.length; i++) {
                String path = diskFilePath[i];
                FileUtils.writeStringToFile(new File(path),"1","utf-8",true);
            }
        }
    }

    public static void main(String[] args) {
        String[] path = new String[1];
        path[0]="F://keepAlive.txt";
        KeepDiskAlive.keepAlive(path);
    }
}
