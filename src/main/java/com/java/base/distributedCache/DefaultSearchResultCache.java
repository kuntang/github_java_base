package com.java.base.distributedCache;

import com.taobao.at.common.enumerate.TripCommonEnums;
import com.taobao.atsearch.commons.base.SearchOption;
import com.taobao.atsearch.constant.SearchConstant;
import com.taobao.atsearch.util.TairHelper;
import com.taobao.tair.DataEntry;
import com.taobao.tair.Result;
import com.taobao.tair.TairManager;
import com.taobao.trip.atic.simpledo.SearchCacheDO;
import com.taobao.trip.atic.util.AticSearchUtil;
import com.taobao.trip.core.common.metrics.annotation.Metrics;
import com.taobao.trip.core.common.metrics.lib.MutableRate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by tangkun.tk on 2016/3/16.
 * ����tair������cache
 */
public class DefaultSearchResultCache extends AbstractSearchResultCache<SearchOption, SearchCacheDO> {


    public static final int NS = TairHelper.getNameSpaceByKey(TairHelper.COMMON_NAMESPACE);

    @Resource(name = "tairManager")
    private TairManager TM;

    @Override
    protected String buildInnerKey(SearchOption option, Object extra) {
        return buildTairKey(option,extra);
    }

    @Override
    public boolean getDistributeLock(String key) {
        Result<DataEntry> current = TM.get(NS, key);
        Integer version = 0;
        if (current != null && current.isSuccess() && current.getValue() != null) {
            version = (Integer) current.getValue().getValue();
        }
        if (version == null || version <= 0) {
            //��������ǰ5sʧЧ��Ȼ�����ڱ�ȥ��������
            Result<Integer> rs = TM.incr(NS, key, 1, 0, cacheTimeoutSec);
            if (rs.isSuccess()) {
                if(rs.getValue() == 1){
                    TM.incr(NS, key, 1, 0, cacheTimeoutSec);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected SearchCacheDO getValudDO(String key) {
        Result<DataEntry> rs = TM.get(NS, key);
        if (rs.isSuccess() && rs.getValue() != null && rs.getValue().getValue() != null) {
            return (SearchCacheDO) rs.getValue().getValue();
        }
        return null;
    }

    /**
     * @param value �洢��valueֵ
     * @return true=����,false=δ����
     */
    @Override
    public boolean isExpire(SearchCacheDO value) {
        if(value == null){
            return true;
        }
        return (System.currentTimeMillis() + getTimeDelta()*1000) > value.getExpireTime();
    }


    @Override
    public void put(SearchOption option, SearchCacheDO searchCacheDO) {
        if (!match(option)) {
            return;
        }
        try {
            searchCacheDO.setExpireTime(System.currentTimeMillis() + cacheTimeoutSec*1000 + getTimeDelta());
            TM.put(NS, buildInnerKey(option, "all"), searchCacheDO, 0, cacheTimeoutSec + getTimeDelta());
        } catch (Exception e) {
            log.error("lxw-dbc put cache error:", e);
        }
    }

    protected boolean match(SearchOption option) {
        if (option == null) {
            return false;
        }
        if (option.getTripType() != TripCommonEnums.TripType.ONEWAY) {
            return false;
        }
        if (option.getMode() != SearchConstant.SearchMode.OW_1 && option.getMode() != SearchConstant.SearchMode.OW_2 &&
                option.getMode() != SearchConstant.SearchMode.OW_4) {
            return false;
        }
        //�ݺ��й�����
        if (option.getExtraSearchMode() != null) {
            return false;
        }
        //���ò���
        if (option.getB2g() == SearchConstant.B2G.ALIGROUP || option.getB2g() == SearchConstant.B2G.OTHER) {
            return false;
        }
        //�������Ĳ���
        if (!option.isSearchAll()) {
            return false;
        }
        //���̵Ĳ���
        if (AticSearchUtil.isFromShop(option)) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (!option.isNeedSpecialSale()) {
            return false;
        }
        return true;
    }

    protected String buildTairKey(SearchOption option, Object extra) {
        //Ŀǰkey���ܹ�֧�ֵ����̵�һ���͵ڶ���
        return "ATIC-" + extra + "," + option.getTripType() + "," + option.getDepCityCode() + "," +
                option.getArrCityCode() +
                "," + option.getDepDate() + "," + option.getDepFlightNO() + "," + option.getMode().name();
    }


}
