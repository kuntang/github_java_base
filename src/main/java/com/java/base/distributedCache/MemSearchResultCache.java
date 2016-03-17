package com.java.base.distributedCache;


import com.alibaba.fastjson.JSON;
import com.taobao.at.common.base.Pair;
import com.taobao.atsearch.commons.base.SearchOption;
import com.taobao.trip.atic.simpledo.SearchCacheDO;
import com.taobao.trip.core.common.metrics.annotation.Metrics;
import org.springframework.stereotype.Component;

/**
 * Created by tangkun.tk on 2016/3/17.
 * 基于内存的搜索结果缓存
 */

public class MemSearchResultCache extends DefaultSearchResultCache{

        // timeCacheMap , p1=缓存时间,p2=桶数目
        // 最好情况:  每个值缓存30秒
        // 最差情况:  部分边界值缓存20秒
        // 自动清理
        TimeCacheMap<String,Pair<Boolean,SearchCacheDO>> memCache = new TimeCacheMap<String, Pair<Boolean,SearchCacheDO>>(30,3);

        @Override
        public Pair<Boolean,SearchCacheDO> get(SearchOption searchOption, Object extra){
            String key = buildMemKey(searchOption);
            Pair<Boolean,SearchCacheDO> searchCacheDOPair = memCache.get(key);
            if( searchCacheDOPair == null || isExpire(searchCacheDOPair.getSecond()) ){
                memCache.put(key,super.get(searchOption,extra));
                searchCacheDOPair = memCache.get(key);
                log.error("option=[{}],load from tair", JSON.toJSONString(searchOption));
            }else {
                log.error("option=[{}],search result from mem", JSON.toJSONString(searchOption));
            }
            return searchCacheDOPair;
        }


        @Override
        public void put(SearchOption option, SearchCacheDO searchCacheDO) {
            if (!match(option)) {
                return;
            }
            try {
                // 优先放入本地内存里,后面的请求可直接取了
                searchCacheDO.setExpireTime(System.currentTimeMillis() + cacheTimeoutSec*1000 + getTimeDelta());
                memCache.put(buildMemKey(option),new Pair<Boolean, SearchCacheDO>(false,searchCacheDO));
                // 再放入tair
                super.put(option,searchCacheDO);
            } catch (Exception e) {
                log.error("lxw-dbc put cache error:", e);
            }
        }


        private String buildMemKey(SearchOption searchOption){
            return searchOption.getDepCityCode()+"_"+searchOption.getArrCityCode()+"_"+searchOption.getDepDate();
        }
}
