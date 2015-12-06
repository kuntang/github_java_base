package com.java.base.deadlock;

/**
 * Created by tangkun.tk on 2015/12/5.
 * 协助对象间的死锁(相互调用)
 */
public class HelpDeadLock {

    private final Dispatch dispatch;
    private String name = "a";
    public HelpDeadLock(Dispatch dispatch){
        this.dispatch = dispatch;
    }

    public synchronized void syncSetName(String name){
        this.name = name;
    }

    public synchronized void callDispatch(){
        try {
            System.out.println("callDispatch waiting.....");
            Thread.sleep(2*1000L);
            dispatch.notifyHelpDeadLock(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        final Dispatch dispatch = new Dispatch();
        final HelpDeadLock deadLock = new HelpDeadLock(dispatch);
        dispatch.setDeadLock(deadLock);

        new Thread(new Runnable() {
            public void run() {
                dispatch.changeName();
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                deadLock.callDispatch();
            }
        }).start();


    }

    public void setName(String name) {
        this.name = name;
    }
}

class Dispatch{

    private HelpDeadLock deadLock;

    public synchronized void notifyHelpDeadLock(HelpDeadLock helpDeadLock){
        helpDeadLock.setName("b");
    }

    public synchronized void changeName(){
        System.out.println("changeName waiting.....");
        try {
            Thread.sleep(2 * 1000L);    // sleep 2 second,waiting sync
            deadLock.syncSetName("c");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setDeadLock(HelpDeadLock deadLock) {
        this.deadLock = deadLock;
    }
}


