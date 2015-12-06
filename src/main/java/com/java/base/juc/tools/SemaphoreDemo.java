package com.java.base.juc.tools;

import java.util.concurrent.Semaphore;

/**
 * Created by tangkun.tk on 2015/12/6.
 * 某个资源最多被5个线程同时访问
 */
public class SemaphoreDemo {

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(5);
        for(int i=0;i<20;i++){
            new Thread(new AccessTask(semaphore,"线程"+i)).start();
        }
    }
}


class AccessTask implements Runnable{
    private Semaphore semaphore;
    private String name;
    public AccessTask(Semaphore semaphore,String name){
        this.semaphore = semaphore;
        this.name = name;
    }

    public void run() {
        try {
            semaphore.acquire();
            Thread.sleep(2*1000L);
            System.out.println("线程"+name+" 获取到资源");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            semaphore.release();
        }
    }

}
