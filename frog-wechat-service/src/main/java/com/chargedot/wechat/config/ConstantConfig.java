package com.chargedot.wechat.config;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/3/16
 */
public class ConstantConfig {

    /**
     * kafka topic
     */
    public static final String DWD_ERROR_REQ = "DWD-ERROR-REQ";//设备错误信息请求
    public static final String DWS_ERROR_RES = "DWS-ERROR-RES";//设备错误信息响应
    public static final String DWD_START_STOP_RESULT_REQ = "DWStartStopResultPush";//充电起停结果上报
    public static final String DWD_CHECK_IN_REQ = "DWD-CHECKIN-REQ";//设备登陆签到
    public static final String DW_CHARGE_REFUND_TOPIC = "DW-CHARGE-REFUND-TOPIC";// 充电退款
    public static final String S2D_REQ_TOPIC = "DWS-SERVER-REQ";
    public static final String S2D_RES_TOPIC = "DWS-SERVER-RES";
    public static final String D2S_REQ_TOPIC = "DWD-SERVER-REQ";
    public static final String D2S_RES_TOPIC = "DWD-SERVER-RES";


    /**
     * operation type
     */
    public static final String StartChargeRequest = "StartChargeRequest";//开始充电结果上报
    public static final String StopChargeRequest = "StopChargeRequest";//停止充电结果上报
    public static final String DCheckInRequest = "CheckInRequest";//设备登陆签到

    /**
     * redis config
     */
    public static final String REDIS_DEVICE_FAULT = "ELECFROGDEVICEFAULT";

    /**
     * interface path and name
     */
    public static final String REFUND_TO_BALANCE = "api/v1/finance/chargeorder/refundtobalance";
    public static final String NOTIFY_USER_START_RESULT = "";
    public static final String NOTIFY_USER_FINISHED = "api/v1/util/send/finishcharge";
    public static final String RECONNECT_REFUND = "api/v1/finance/chargeorder/batchrefund";

    /**
     * return value
     */
    public static final Integer RET_SYSTEM_BUSY = -1;// 系统繁忙，稍后重试
    public static final Integer RET_SUCCESS = 0;// 成功
    public static final Integer RET_POST_PARAM_ERROR = 4003;// post参数错误
    public static final Integer RET_SYSTEM_ERROR = 500;// 系统错误


    /**
     * return message
     */
    public static final String MSG_SYSTEM_BUSY = "system busy, please try again later.";
    public static final String MSG_POST_PARAM_ERROR = "post param is illegal.";
    public static final String MSG_SYSTEM_ERROR = "system error, please try again later.";
    public static final String MSG_SUCCESS = "request success.";

    public static final int DEV_RECONNECT_NET = 1;
    public static final int DEV_RESTART_MODE = 60;
    public static final int FINISH_CHARGE_NORMAL = 0;
    public static final int FINISH_CHARGE_USER = 4;
    public static final int FINISH_CHARGE_AUTO = 5;


    public static final int SUCCESS = 1;
    public static final int FAILURE = 2;

    /**
     * 订单状态 1为已创建,2为进行中,3为正常充电完成,4为过流停止充电,5未充电
     */
    public static final int CREATED = 1;
    public static final int ONGOING = 2;
    public static final int FINISH_SUCCESS = 3;
    public static final int FINISH_OUT_OF_AC = 4;
    public static final int UNCHARGEING = 5;


    /**
     * 凭证类型： 1包月次卡 2包月包时卡 3充值卡 4手机号
     */
    public static final int CERT_CART_OF_MONTH_COUNT = 1;
    public static final int CERT_CART_OF_MONTH_TIME = 2;
    public static final int CERT_CART_OF_BALANCE = 3;
    public static final int CERT_OF_PHONE = 4;

    /**
     * 启动方式 1为微信2为app3为卡
     */
    public static final int START_BY_WX = 1;
    public static final int START_BY_APP = 2;
    public static final int START_BY_CARD = 3;

    /**
     * 订单类型 1正常2异常3校正
     */
    public final static int ORDER_TYPE_NORMAL = 1;
    public final static int ORDER_TYPE_EXCEPTION = 2;
    public final static int ORDER_TYPE_ADJUST = 3;

    /**
     * 支付状态 1为待支付,2为支付成功,3为支付失败,4已退款,5已开票,6待退款，8退款中
     */
    public static final int UNPAID = 1;
    public static final int PAID = 2;
    public static final int PAID_FAILURE = 3;
    public static final int REFUND = 4;
    public static final int INVOICED = 5;
    public static final int UNREFUND = 6;
    public static final int REFUNDING = 8;


    /**
     * 扣款来源 1实账 2虚账 3虚实结合
     */
    public static final int PAY_SRC_REAL = 1;
    public static final int PAY_SRC_VIRTUAL = 2;
    public static final int PAY_SRC_REAL_AND_VIRTUAL = 3;

    /**
     * 支付方式 1为余额支付,2支付宝支付,3微信支付,4月卡支付,5平台扣款，6站点卡支付，7优惠卡（满几次送几次的卡）8包月包时卡9包月包次卡10充值卡
     */
    public static final int PAY_NULL = 0;
    public static final int PAY_BY_BALANCE = 1;
    public static final int PAY_BY_ALIPAY = 2;
    public static final int PAY_BY_WEIPAY = 3;
    public static final int PAY_BY_MONTH_CARD = 4;
    public static final int PAY_BY_PLAM = 5;
    public static final int PAY_BY_DISCOUNT_CARD = 6;
    public static final int PAY_BY_STATION_CARD = 7;
    public static final int PAY_BY_MONTHLY_TIME_CARD = 8;
    public static final int PAY_BY_MONTHLY_COUNT_CARD = 9;
    public static final int PAY_BY_PREPAID_CARD = 10;

    /**
     * card status 1可使用2使用中3不可使用
     */
    public final static int CARD_AVAILABLE = 1;
    public final static int CARD_OCCUPYING = 2;
    public final static int CARD_UNAVAILABLE = 3;

    /**
     * 卡片封禁状态（1已激活2封禁）
     */
    public final static int CARD_UNFORBIDDEN = 1;
    public final static int CARD_FORBIDDEN = 2;

    public final static int CARD_LOWEST_USE_COUNT = 0;


    /**
     * 流水记录类型 1扣费 2充值 3退款
     */
    public final static int STREAM_TYPE_PAY = 1;
    public final static int STREAM_TYPE_MINIPROGRAM = 2;
    public final static int STREAM_TYPE_REFOUND = 3;

    /**
     * 操作来源 1平台 2小程序 3刷卡
     */
    public final static int OPERATOR_SRC_PLATFORM = 1;
    public final static int OPERATOR_SRC_APP = 2;
    public final static int OPERATOR_SRC_CARD = 3;

    /**
     * port type
     */
    public final static String PORT_NONE = "";
    public final static String PORT_ALL = "FF";
    public final static String PORT_SINGLE = "00";


}
