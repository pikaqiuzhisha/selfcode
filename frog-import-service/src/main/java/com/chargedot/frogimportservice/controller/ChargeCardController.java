package com.chargedot.frogimportservice.controller;

import com.alibaba.fastjson.JSON;
import com.chargedot.frogimportservice.controller.vo.CommonResult;
import com.chargedot.frogimportservice.model.ChargeCard;
import com.chargedot.frogimportservice.service.ChargeCardService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/card")
public class ChargeCardController {

    //创建日志对象
    private static final Logger log = LoggerFactory.getLogger(ChargeCardController.class);

    //创建卡管理业务对象
    @Autowired
    private ChargeCardService chargeCardService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @HystrixCommand(fallbackMethod = "defaultSendMessage")
    @RequestMapping(value = "/card_import", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> importData(@RequestBody String req) {
        // TODO 输入参数校验
        log.debug("req：{}", req);
        //获取卡号集合数据
        List<ChargeCard> chargeCardList = JSON.parseArray(req, ChargeCard.class);
        //存储重复的卡号
        List<ChargeCard> repeatCardList = new ArrayList<>();
        //存储唯一的卡号
        List<ChargeCard> onlyCardList = new ArrayList<>();
        log.debug("card条数：{}", chargeCardList.size());
        log.info("开始时间：{}", new Date());
        //定义变量,来判断是否成功
        int isRight = 0;
        //标识重复的个数
        int repeatCount = 0;
        //标识唯一的个数
        int onlyCount = 0;

        //判断集合里是否存在数据
        if (chargeCardList.size() > 0) {
            //循环检验是否存在重复的卡号
            for (ChargeCard chargeCard : chargeCardList) {
                if (chargeCardService.selectChargeCardNumberCount(chargeCard.getCardNumber())) {
                    repeatCardList.add(chargeCard);
                    repeatCount++;
                } else {
                    onlyCardList.add(chargeCard);
                    onlyCount++;
                }
            }
            //调用批量导入方法
            isRight = chargeCardService.importChargeCardData(onlyCardList);

            log.info("结束时间：{}", new Date());

            log.debug("重复个数：{}", repeatCount);
            log.debug("repeatCardList：{}", repeatCardList);
            log.debug("不重复个数：{}", onlyCount);
            log.debug("onlyCardList：{}", onlyCardList);

            //判断调用方法是否成功
            if (isRight > 0) {
                return new ResponseEntity<CommonResult>(CommonResult.buildSuccessResult("send success"), HttpStatus.OK);
            } else {
                return new ResponseEntity<CommonResult>(CommonResult.buildResult(500), HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
        }
    }

    public ResponseEntity<CommonResult> defaultSendMessage(@RequestBody String req) {
        log.info("[defaultSendMessage][req]{}", req);
        return new ResponseEntity<CommonResult>(CommonResult.buildResult(-1), HttpStatus.OK);
    }
}
