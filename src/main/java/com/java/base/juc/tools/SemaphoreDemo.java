package com.java.base.juc.tools;

import java.util.concurrent.Semaphore;

/**
 * Created by tangkun.tk on 2015/12/6.
 * ĳ����Դ��౻5���߳�ͬʱ����
 */
public class SemaphoreDemo {

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(5);
        for(int i=0;i<20;i++){
            new Thread(new AccessTask(semaphore,"�߳�"+i)).start();
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
            System.out.println("�߳�"+name+" ��ȡ����Դ");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            semaphore.release();
        }
    }

}
