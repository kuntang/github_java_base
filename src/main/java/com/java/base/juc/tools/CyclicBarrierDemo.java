package com.java.base.juc.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tangkun.tk on 2015/12/6.
 * 多个关卡的问题
 * 旅游团从深圳出发，途径 广州->东莞->回到深圳
 * 三种出行方式,每种方式时间不一样:
 * 1, 徒步
 * 2，自驾游
 * 3，大巴
 * 必须所有人到达某个集合点后，再出发去下一个点。
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);
        Map<String,Integer> timeMap1 = new HashMap<String, Integer>();
        timeMap1.put("shenzhen",3);
        timeMap1.put("guangzhou",2);
        timeMap1.put("dongguan",5);
        timeMap1.put("shenzhen2",4);

        Map<String,Integer> timeMap2 = new HashMap<String, Integer>();
        timeMap2.put("shenzhen",5);
        timeMap2.put("guangzhou",6);
        timeMap2.put("dongguan",7);
        timeMap2.put("shenzhen2",8);

        Map<String,Integer> timeMap3 = new HashMap<String, Integer>();
        timeMap3.put("shenzhen",1);
        timeMap3.put("guangzhou",2);
        timeMap3.put("dongguan",3);
        timeMap3.put("shenzhen2",4);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(new TravelTask(timeMap1,cyclicBarrier,"徒步"));
        executorService.submit(new TravelTask(timeMap2,cyclicBarrier,"自驾游"));
        executorService.submit(new TravelTask(timeMap3,cyclicBarrier,"大巴"));
    }

}

class TravelTask implements Runnable{
    private Map<String,Integer> timeMap;
    private CyclicBarrier barrier;
    private String name;
    public TravelTask(Map<String,Integer> tiems,CyclicBarrier barrier,String name){
        this.timeMap = tiems;
        this.barrier = barrier;
        this.name = name;
    }
    public void run() {
        for(Map.Entry<String,Integer> entry : timeMap.entrySet()){
            try {
                Thread.sleep( entry.getValue() * 1000L);
                barrier.await();
                System.out.println(name+" arrived " + entry.getKey());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
