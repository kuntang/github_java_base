package com.java.base.deadlock;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by tangkun.tk on 2015/12/5.
 * 线程饥饿死锁
 * 线程池里只有1个线程,但提交的第一个任务依赖于第二个和第三个任务执行完成。
 * 线程池中线程永远执行不到第二个第三个任务,因而造成死锁.
 */
public class HungerDeadLock {
    ExecutorService exec = Executors.newSingleThreadExecutor();
    public static void main(String[] args) {
        HungerDeadLock deadLock = new HungerDeadLock();
        RenderPageTask renderPageTask = deadLock.new RenderPageTask();
        deadLock.exec.submit(renderPageTask);
    }

    public  class RenderPageTask implements Callable<String>{
        public String call() throws Exception {
            System.out.println("RenderPageTask.call()");
            Future header = exec.submit(new LoadTask("header"));
            Future footer = exec.submit(new LoadTask("header"));
            return (String)header.get()+footer.get();
        }
    }

    static class LoadTask implements Runnable{
        private String s;
        public LoadTask(String s){
            this.s = s;
        }

        public void run() {
            System.out.println(s + " ok ");
        }
    }


}
