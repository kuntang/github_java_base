package com.java.base.concurrent;

/**
 * Created by tangkun.tk on 2016/4/8.
 * String 的不可变性.
 * 为什么String 是final的,不可变的?
 *  1,字符串常量池需要.
 *      String s1 = "1234";
 *      String s2 = "1234";
 *      s1 和 s2 引用指向同一个字符串对象. 如果,String是可变的,改变s1会影响s2的值.
 *  2,允许String对象缓存HashCode
 *      String对象内有个hash字段,缓存的就是String对象的哈希值,方便集合(Map,Set)等取,不用重复计算.
 *  3,安全性
 *      网络地址URL,文件路径path,反射机制所需要的参数等,可变会影响安全性.
 *
 */
public class StringDemo {
    public static void main(String[] args) {
        String s = "123";
    }
}
