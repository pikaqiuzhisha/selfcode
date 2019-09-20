package com.chargedot.frogimportservice.controller.vo;

import com.chargedot.frogimportservice.config.ConstantConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author：caoj
 * @Description：
 * @Data：Created in 2018/6/8
 */
@Slf4j
@Data
public class CommonResult {

    @JsonProperty("Ret")
    private Integer ret;

    @JsonProperty("Msg")
    private String msg;

    @JsonProperty("Data")
    private Object data;

    public CommonResult(Integer ret, String msg) {
        this.ret = ret;
        this.msg = msg;
    }

    public CommonResult(Object data) {
        this.ret = ConstantConfig.RET_SUCCESS;
        this.msg = ConstantConfig.MSG_SUCCESS;
        this.data = data;
    }

    public CommonResult(Integer ret, String msg, Object data) {
        this.ret = ret;
        this.msg = msg;
        this.data = data;
    }

    public static CommonResult buildSuccessResult(Object data) {
        return new CommonResult(data);
    }

    public static CommonResult buildResult(Integer ret) {
        String msg = "unknown error";
        if (ret.equals(ConstantConfig.RET_SUCCESS)) {
            msg = ConstantConfig.MSG_SUCCESS;
        } else if (ret.equals(ConstantConfig.RET_POST_PARAM_ERROR)) {
            msg = ConstantConfig.MSG_POST_PARAM_ERROR;
        } else if (ret.equals(ConstantConfig.RET_SYSTEM_BUSY)) {
            msg = ConstantConfig.MSG_SYSTEM_BUSY;
        } else if (ret.equals(ConstantConfig.RET_SYSTEM_ERROR)) {
            msg = ConstantConfig.MSG_SYSTEM_ERROR;
        }
        return new CommonResult(ret, msg);
    }

    public static CommonResult buildResults(Integer ret, String msg,Object data) {
        return new CommonResult(ret, msg, data);
    }
}
