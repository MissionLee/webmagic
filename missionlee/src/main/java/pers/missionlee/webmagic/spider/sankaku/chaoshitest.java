package pers.missionlee.webmagic.spider.sankaku;

import java.util.concurrent.*;

/**
 * @description:
 * @author: Mission Lee
 * @create: 2019-03-07 20:09
 */
public class chaoshitest     {

    public static void main(String[] args) {
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                System.out.println("do something");
                Thread.sleep(10000);
                return 1;
            }
        } ;
        Future<Integer> future = executorService.submit(callable);
        try {
            Integer integer = future.get(5000, TimeUnit.MILLISECONDS);
            System.out.println(integer);
        } catch (InterruptedException e) {
            System.out.println("sinterrupted");
            e.printStackTrace();
        } catch (ExecutionException e) {
            System.out.println("error");
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("time out");
            e.printStackTrace();
        }
        executorService.shutdown();
    }
}
