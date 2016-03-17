package com.java.base.distributedCache;

import com.taobao.at.common.base.Pair;
import com.taobao.ateye.annotation.Switch;
import com.taobao.trip.core.common.metrics.annotation.Metric;
import com.taobao.trip.core.common.metrics.lib.MutableRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 整个搜索的缓存,包括(atw,atew等结果)
 *
 * @author 黑伯
 * @version 1.0
 * @created 2016-03-14 13:55
 */
public abstract class AbstractSearchResultCache<OPT,V extends Serializable>{

    protected int cacheTimeoutSec = 30;

    protected int dkTimeoutSec = 5;

    protected int maxWaitMs = 1500;

    protected static final Logger log = LoggerFactory.getLogger(AbstractSearchResultCache.class);

    /**
     * 构造tair key
     *
     * @param option
     *
     * @return
     */
    protected abstract String buildInnerKey(OPT option, Object extra);


    /**
     * 从缓存获取数据
     *
     * @param option
     * @param extra  对option的补充 非必须
     *
     * @return <是否需要更新缓存，结果>
     */
    public Pair<Boolean, V> get(OPT option, Object extra) {

        long start = System.currentTimeMillis();

        try {
            String key = buildInnerKey(option, extra);
            Pair<Boolean, V> rs = new Pair<Boolean, V>(false, null);
            int used = 0;
            do {
                long begin = System.currentTimeMillis();
                V value = getValudDO(key);
                //哨兵去更新数据
                if (isExpire(value)) {
                    rs.setFirst(getDistributeLock(buildDistributeTairKey(option, extra)));
                    log.error("lxw-dbc sentinel start");
                }
                if (value != null) {
                    rs.setSecond(value);
                    getHitRate().incrNum();
                    return rs;
                } else {
                    // 遇到哨兵,赶紧放走去请求下层数据
                    if(rs.getFirst()){
                        return rs;
                    }
                    TimeUnit.MILLISECONDS.sleep(200);
                    used += (System.currentTimeMillis() - begin);
                }
            } while (used < maxWaitMs);
            log.error("lxw-dbc timeout, time used:{}ms", used);
            return rs;
        } catch (Exception e) {
            log.error("lxw-dbc get cache error", e);
            return new Pair<Boolean, V>(true, null);
        } finally {
            log.error("lxw-dbc time-used:{}ms", (System.currentTimeMillis() - start));
        }
    }

//    // 获取分布式锁 的value
//    public abstract Result<DataEntry> getLockKey(String key);

    public abstract boolean getDistributeLock(String key);

    protected abstract V getValudDO(String key);

    /**
     * 构造tair key
     *
     * @param option
     *
     * @return
     */
    protected String buildDistributeTairKey(OPT option, Object extra) {
        return "distribute-" + buildInnerKey(option, extra);
    }

//    // 抢(初始化)分布式锁的key
//    public abstract Result<DataEntry> initLockKey(String key);

    // 是否过期
    public abstract boolean isExpire(V value);

    public abstract void put(OPT option, V v);

    /**
     * 提前放哨兵时间，单位秒
     *
     * @return
     */
    protected int getTimeDelta() {
        return dkTimeoutSec;
    }

}
