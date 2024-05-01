package pers.missionlee.chan.pagedownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class WebDriverUtils {

    public static void killChrome() throws IOException {
        Runtime.getRuntime().exec("taskkill /IM " + "chrome.exe" + " /F");
//        int killed = 0;
//        Process process = Runtime.getRuntime().exec("tasklist");
//        String line;
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            while ((line=reader.readLine())!=null){
//                System.out.println(line);
//                if(line.contains("chrome.exe")){
//
//
//                }
//            }
//        }
    }

    public static int getFreePort(int start) throws IOException {
        String command = "netstat -noa"; // | findstr "+start;  Runtime.exec不支持管道指令，要用的话需要 ProcessorBuilder 比较麻烦
        Process process = Runtime.getRuntime().exec(command);
        String aim = ""+start;
        String line;
        boolean find = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            System.out.println(reader.readLine());
            while ((line = reader.readLine()) != null) {
                if(line.contains(aim)){
                    find = true;
                    break;
                }
            }
        }
        if(find){
            return getFreePort(start+1);
        }else{
            return start;
        }
    }
}
