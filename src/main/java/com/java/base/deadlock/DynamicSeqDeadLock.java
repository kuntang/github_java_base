package com.java.base.deadlock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tangkun.tk on 2015/12/5.
 * 动态顺序死锁(银行转账)
 */
public class DynamicSeqDeadLock {
    // c账户
    static Account cAccount = new Account("c账户",1000);
    // d账户
    static Account dAccount = new Account("d账户",2000);

    // 加时锁
    static Object xLock = new Object();

    public static void main(String[] args) {
        final DynamicSeqDeadLock deadLock = new DynamicSeqDeadLock();
//        deadLock.runDeadlock(deadLock);
        deadLock.runFixDeadlock(deadLock);

    }

    public void runDeadlock(final DynamicSeqDeadLock deadLock){
        new Thread(new Runnable() {
            public void run() {
                try {
                    deadLock.transforMoney(cAccount,dAccount,1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    deadLock.transforMoney(dAccount,cAccount,1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void runFixDeadlock(final DynamicSeqDeadLock deadLock){
        new Thread(new Runnable() {
            public void run() {
                try {
//                    deadLock.fix(cAccount,dAccount,10);
                    deadLock.fixByTryLock(cAccount,dAccount,10);
                    System.out.println("cAccount="+cAccount.getMoney()+",dAccount="+dAccount.getMoney());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                try{
//                    deadLock.fix(dAccount,cAccount,20);
                    deadLock.fixByTryLock(dAccount,cAccount,20);
                    System.out.println("cAccount="+cAccount.getMoney()+",dAccount="+dAccount.getMoney());
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void transforMoney(Account from,Account to,int money) throws InterruptedException {
        synchronized (from){
            Thread.sleep(2*1000L);
            System.out.println("get from"+from.getName()+",waiting "+to.getName());
            synchronized (to){
                decreaseAccount(from,to,money);
            }
        }
    }

    public void fix(Account from,Account to,int money) throws InterruptedException {
        if(from.hashCode() > to.hashCode()){
            synchronized (from){
                Thread.sleep(2*1000L);
                System.out.println("get from"+from.getName()+",waiting "+to.getName());
                synchronized (to){
                    decreaseAccount(from,to,money);
                }
            }
        }else if(to.hashCode() > from.hashCode() ){
            synchronized (to){
                Thread.sleep(2*1000L);
                System.out.println("get from"+from.getName()+",waiting "+to.getName());
                synchronized (from){
                    decreaseAccount(from,to,money);
                }
            }
        }else{
            synchronized (xLock){
                synchronized (to){
                    synchronized (from){
                        decreaseAccount(from,to,money);
                    }
                }
            }
        }
    }

    public void fixByTryLock(Account from,Account to,int money) throws InterruptedException {
        while (true){
            if (from.lock.tryLock()){
                System.out.println("account"+from.getName()+" sleeping");
                try{
                    if(to.lock.tryLock()){
                        try{
                            decreaseAccount(from,to,money); // success , return
                            return;
                        }finally {
                            to.lock.unlock();
                        }
                    }
                }finally {
                    from.lock.unlock();
                }
            }
        }
    }


    public void decreaseAccount(Account from,Account to,int money){
        if(from.getMoney() < money){
            throw new IllegalArgumentException("your account has not enough money");
        }
        from.setMoney(from.getMoney()-money);
        to.setMoney(to.getMoney()+money);
        System.out.println("finished!");
    }

    static class Account{
        Lock lock = new ReentrantLock();
        private String name;
        private int money;
        public Account(String name,int money){
            this.name = name;
            this.money = money;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getMoney() {
            return money;
        }

        public void setMoney(int money) {
            this.money = money;
        }
    }

}
