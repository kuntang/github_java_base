package com.java.base.java8;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by tangkun.tk on 2016/4/7.
 * 新的集合操作,排序,遍历,过滤,匹配等
 */
public class NewListOp {

    public static void main(String[] args) {
        seqSort();
        parallelSort();
        streamOut();
    }

    public static void seqSort(){
        List<String> list = new ArrayList<String>();
        for(int i=0;i<1000000;i++){
            double d = Math.random()*1000;
            list.add(d+"");
        }
        long start = System.nanoTime();//获取系统开始排序的时间点
        Stream s=  ((Stream) list.stream().sequential()).sorted();
        long end = System.nanoTime();//获取系统结束排序的时间点
        long ms = TimeUnit.NANOSECONDS.toMillis(end-start);//得到串行排序所用的时间
        System.out.println(ms+"ms,count=");

        // 遍历输出
//        s.forEach(System.out::println);


    }

    public static void parallelSort(){
        List<String> list = new ArrayList<String>();
        for(int i=0;i<1000000;i++){
            double d = Math.random()*1000;
            list.add(d+"");
        }
        long start = System.nanoTime();//获取系统开始排序的时间点
        int count = (int)((Stream) list.stream().parallel()).sorted().count();
        long end = System.nanoTime();//获取系统结束排序的时间点
        long ms = TimeUnit.NANOSECONDS.toMillis(end-start);//得到并行排序所用的时间
        System.out.println(ms + "ms,count="+count);
    }


    public static void streamOut(){
        List<Double> list = new ArrayList<>();
        for(int i=0;i<100;i++){
            double d = Math.random()*1000;
            list.add(d);
        }
        Stream<Double> s =  (list.stream().sequential()).sorted();
        // 遍历输出
        List<Double> vv = new ArrayList<>();
        // forEach操作
        s.filter( (v)-> v > 100 ).forEach( (v)-> vv.add(v+1));
    }

}
