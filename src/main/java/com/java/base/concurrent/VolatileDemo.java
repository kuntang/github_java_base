package com.java.base.concurrent;

import java.util.concurrent.CountDownLatch;

/**
 * Created by tangkun.tk on 2016/4/8.
 *  volatile 关键字的使用
 *  java分主内存空间和线程内存空间,如下场景:
 *  主内存有变量 a = 1;
 *  线程空间保存变量a的一份副本,优化访问。但可能造成 线程空间 和 主内存空间 变量值不一致的情况.
 *  volatile 关键字修饰的变量,不再保存线程空间的变量副本,每次访问都直接访问主内存空间变量.
 *  volatile 关键字只保证 【可见性】，但却不能保证【原子性】.
 *  synchronized 关键字保证 【原子性】 和 【可见性】
 *
 *  soutVolatileAndSync()方法输出如下(每次输出结果不一致)：
 *  b= 97509
    c= 100001
    结论: volatile 只保证可见性,++操作会是: 读取主内存变量-> +1操作 ->写回主内存 三个步骤.多个线程间,读取到的值可能有相互覆盖,导致++最终不一致.
 *
 *
 *
 */
public class VolatileDemo {

    private static int a = 1;

    private static volatile int b = 1;

    private static int c = 1;

    private static CountDownLatch volatileCountDownLatch = new CountDownLatch(100);
    private static CountDownLatch syncCountDownLatch = new CountDownLatch(100);

    public static void main(String[] args) throws InterruptedException {
        read();
        write(2);

        soutVolatileAndSync();
    }

    public static void soutVolatileAndSync() throws InterruptedException{
        multWriteVolatileVar(100);
        volatileCountDownLatch.await();
        System.out.println("b= "+b);
        multWriteSyncVar(100);
        syncCountDownLatch.await();
        System.out.println("c= "+c);
    }


    public  static void read(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while( a == 1){
                    System.out.println(" a == 1");
                }
            }
        }).start();
    }

    public static void write(int a){
        VolatileDemo.a = 2;
    }

    public static void multWriteVolatileVar(int threads){
        for(int i=0;i<threads;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int j=0;j<1000;j++){
                        b++;
                    }
                    volatileCountDownLatch.countDown();
                }
            }).start();
        }
    }

    public static void multWriteSyncVar(int threads){
        for(int i=0;i<threads;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int j=0;j<1000;j++){
                        synchronized (VolatileDemo.class){
                            c++;
                        }
                    }
                    syncCountDownLatch.countDown();
                }
            }).start();
        }
    }

}
