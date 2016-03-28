package com.java.base.waitNotify;

/**
 * Created by tangkun.tk on 2016/3/28.
 * 验证 wait 和 notify的一些特性
 */
public class WaitNotify {

    public static void main(String[] args) {
        final Object lock = new Object();
        try{
            new Thread(){
                public void run(){
                    synchronized (lock){
                        try{
                            lock.wait();
                            System.out.println("wait exit");
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            }.start();

            Thread.sleep(3*1000);

            new Thread(){
                public void run(){
                    synchronized (lock){
                        lock.notify();
                        try {
                            Thread.sleep(3*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("notify sleep 10 seconds and exit");
                    }
                }
            }.start();
            Thread.sleep(5*1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
