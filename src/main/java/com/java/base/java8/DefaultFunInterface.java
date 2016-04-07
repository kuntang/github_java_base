package com.java.base.java8;

/**
 * Created by tangkun.tk on 2016/4/7.
 * java8 接口
 */
public interface DefaultFunInterface {

    // 定义默认方法

    default int count(){
        return 1;
    }

    // 静态方法
    static String find(){
        return "interface found";
    }

}
