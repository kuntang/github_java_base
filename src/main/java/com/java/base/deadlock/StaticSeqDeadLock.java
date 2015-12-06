package com.java.base.deadlock;

/**
 * Created by tangkun.tk on 2015/12/5.
 * 静态顺序死锁
 */
public class StaticSeqDeadLock {

    final Object left = new Object();
    final Object right = new Object();

    public static void main(String[] args) {
        final StaticSeqDeadLock staticSeqDeadLock = new StaticSeqDeadLock();
        new Thread(new Runnable() {
            public void run() {
                try {
                    staticSeqDeadLock.getLeftRight();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


        new Thread(new Runnable() {
            public void run() {
                try {
                    staticSeqDeadLock.getRightLeft();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public void getLeftRight() throws InterruptedException {
        synchronized (left){
            System.out.println("left->right sleeping");
            Thread.sleep(10*1000L);
            System.out.println("get left,waiting right");
            synchronized (right){
                System.out.println("ok!");
            }
        }
    }

    public void getRightLeft() throws InterruptedException {
        synchronized (right){
            System.out.println("right->left sleeping");
            Thread.sleep(10*1000L);
            System.out.println("get right,waiting left");
            synchronized (left){
                System.out.println("ok!");
            }
        }
    }

    public void fix(){
        synchronized (left){
            System.out.println("must left -> right seq get lock");
            synchronized (right){
                System.out.println("ok");
            }
        }
    }

}
