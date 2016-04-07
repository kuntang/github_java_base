package com.java.base.java8;

/**
 * Created by tangkun.tk on 2016/4/7.
 * java8 接口实现
 */
public class DefaultImpl implements DefaultFunInterface{

    @Override
    public int count(){
        return 2;
    }

    public static void main(String[] args) {

        DefaultImpl di = new DefaultImpl();
        System.out.println(","+di.count());
        System.out.println(DefaultFunInterface.find());

    }
}
