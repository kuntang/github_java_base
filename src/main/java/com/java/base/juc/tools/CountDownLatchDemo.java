package com.java.base.juc.tools;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tangkun.tk on 2015/12/5.
 * 10��ѡ�����ܡ�
 * begin = ����ǹ
 * end = ���һ������յ�ĺ���.
 */
public class CountDownLatchDemo {

    static CountDownLatch begin = new CountDownLatch(1);
    static CountDownLatch end = new CountDownLatch(10);
    static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        for(int i=0;i<10;i++){
            final int no = i+1;
            executorService.submit(new Runnable() {
                public void run() {
                    try {
                        begin.await();
                        Thread.sleep((long)(Math.random() * 1000L));
                        System.out.println(no+" finished");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        end.countDown();
                    }
                }
            });
        }

        begin.countDown();
        try {
            end.await();
            System.out.println("ȫ���������");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();

    }

}
