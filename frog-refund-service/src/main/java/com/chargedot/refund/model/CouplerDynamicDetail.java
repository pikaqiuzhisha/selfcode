/**
 * 
 */
package com.chargedot.refund.model;

import com.chargedot.refund.config.ConstantConfig;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eric Gui
 * @date 2019/4/15
 */
@Data
public final class CouplerDynamicDetail {

    /**
     * order status
     */
    public final static String ORDER_NONE = "";
    public final static String ORDER_CHARGE = "充电";
    public final static String ORDER_TIME = "定时";
    public final static String ORDER_BOOK = "预约";

    private int userId;
    private volatile String orderType;
    private String orderNumber;
    private String sequenceNumber;
    private int startType;
    private int chargeTime;
    private String startDetail;
    private String startedAt;
    private String finishedAt;
    private int voltage;
    private int current;
    private int finishChargeReason;
    private int faultType;
    private Map<String, Integer> elecQuantityDetail = new HashMap<String, Integer>();
    private int elecQuantity;
    private int duration;

    public CouplerDynamicDetail() {
        init();
    }

    public void init() {
        userId = 0;
        orderType = ORDER_NONE;
        orderNumber = "";
        sequenceNumber = "";
        startType = 0;
        chargeTime = 0;
        startDetail = "";
        startedAt = "";
        finishedAt = "";
        voltage = 0;
        current = 0;
        finishChargeReason = 0;
        faultType = 0;
        elecQuantityDetail.clear();
        elecQuantity = 0;
        duration = 0;
    }

    public void setter(Integer userId, String sequenceNumber, String orderNumber, String cardNumber) {
        Date date=new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now =simpleDateFormat.format(date);
        this.userId =userId;
        this.orderType = ORDER_CHARGE;
        this.orderNumber = orderNumber;
        this.startType = ConstantConfig.START_BY_CARD;
        this.startDetail = cardNumber;
        this.startedAt = now;
    }
}