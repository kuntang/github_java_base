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
 * ���������Ļ���,����(atw,atew�Ƚ��)
 *
 * @author �ڲ�
 * @version 1.0
 * @created 2016-03-14 13:55
 */
public abstract class AbstractSearchResultCache<OPT,V extends Serializable>{

    protected int cacheTimeoutSec = 30;

    protected int dkTimeoutSec = 5;

    protected int maxWaitMs = 1500;

    protected static final Logger log = LoggerFactory.getLogger(AbstractSearchResultCache.class);

    /**
     * ����tair key
     *
     * @param option
     *
     * @return
     */
    protected abstract String buildInnerKey(OPT option, Object extra);


    /**
     * �ӻ����ȡ����
     *
     * @param option
     * @param extra  ��option�Ĳ��� �Ǳ���
     *
     * @return <�Ƿ���Ҫ���»��棬���>
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
                //�ڱ�ȥ��������
                if (isExpire(value)) {
                    rs.setFirst(getDistributeLock(buildDistributeTairKey(option, extra)));
                    log.error("lxw-dbc sentinel start");
                }
                if (value != null) {
                    rs.setSecond(value);
                    getHitRate().incrNum();
                    return rs;
                } else {
                    // �����ڱ�,�Ͻ�����ȥ�����²�����
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

//    // ��ȡ�ֲ�ʽ�� ��value
//    public abstract Result<DataEntry> getLockKey(String key);

    public abstract boolean getDistributeLock(String key);

    protected abstract V getValudDO(String key);

    /**
     * ����tair key
     *
     * @param option
     *
     * @return
     */
    protected String buildDistributeTairKey(OPT option, Object extra) {
        return "distribute-" + buildInnerKey(option, extra);
    }

//    // ��(��ʼ��)�ֲ�ʽ����key
//    public abstract Result<DataEntry> initLockKey(String key);

    // �Ƿ����
    public abstract boolean isExpire(V value);

    public abstract void put(OPT option, V v);

    /**
     * ��ǰ���ڱ�ʱ�䣬��λ��
     *
     * @return
     */
    protected int getTimeDelta() {
        return dkTimeoutSec;
    }

}
