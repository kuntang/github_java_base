package com.java.base.juc.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by tangkun.tk on 2015/12/6.
 * 范围导航map.
 */
public class ConcurrentSkipListMapDemo {

    public static void main(String[] args) {
        ConcurrentSkipListMap<Long,Long> map = new ConcurrentSkipListMap<Long,Long>();
        map.put(1L,2L);
        map.put(11L,3L);
        map.put(21L,3L);
        map.put(7L,3L);
        map.put(8L,3L);
//        for(Map.Entry<Long,Long> entry : map.entrySet()){
//            System.out.println(entry.getKey()+","+entry.getValue());
//        }
        ConcurrentNavigableMap<Long,Long> submap = map.subMap(7L,21L);
        for(Map.Entry<Long,Long> entry : submap.entrySet()){
            System.out.println(entry.getKey()+","+entry.getValue());
        }
    }
}
