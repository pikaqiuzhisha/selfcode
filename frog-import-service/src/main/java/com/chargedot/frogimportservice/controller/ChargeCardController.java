package com.chargedot.frogimportservice.controller;

import com.alibaba.fastjson.JSON;
import com.chargedot.frogimportservice.controller.vo.CommonResult;
import com.chargedot.frogimportservice.model.ChargeCard;
import com.chargedot.frogimportservice.model.DWUser;
import com.chargedot.frogimportservice.service.ChargeCardService;
import com.chargedot.frogimportservice.service.DWUserService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/card")
public class ChargeCardController {
    //创建卡管理业务对象
    @Autowired
    private ChargeCardService chargeCardService;

    @Autowired
    private DWUserService dwUserService;

    @HystrixCommand(fallbackMethod = "defaultSendMessage")
    @RequestMapping(value = "/card_import", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> importData(@RequestBody String req) {
        //校验参数
        if(req == null || req.equals("")){
            return new ResponseEntity<CommonResult>(CommonResult.buildResult(4003), HttpStatus.OK);
        }else{
            log.debug("req：{}", req);
            //获取卡号集合数据
            List<ChargeCard> chargeCardList = JSON.parseArray(req, ChargeCard.class);
            //存储重复的卡号
            List<ChargeCard> repeatCardList = new ArrayList<>();
            //存储唯一的卡号
            List<ChargeCard> noRepeatCardList = new ArrayList<>();
            log.debug("card条数：{}", chargeCardList.size());
            log.info("开始时间：{}", new Date());
            //定义变量，来校验新增用户是否成功
            int isCorrect = 0;
            //定义变量,来校验录卡是否成功
            int isRight = 0;
            //标识重复的个数
            int repeatCount = 0;
            //标识唯一的个数
            int noRepeatCount = 0;
            //表示卡的类型
            int type = 0;

            //校验集合里是否存在数据
            if (chargeCardList.size() > 0) {
                //循环校验是否存在重复的卡号
                for (ChargeCard chargeCard : chargeCardList) {
                    if (chargeCardService.selectChargeCardNumberCount(chargeCard.getCardNumber())) {
                        repeatCardList.add(chargeCard);
                        repeatCount++;
                    } else {
                        noRepeatCardList.add(chargeCard);
                        noRepeatCount++;
                        //根据卡的类型匹配用户的类型
                        switch (chargeCard.getType()){
                            case 1:
                                type = 3;
                                break;
                            case 2:
                                type = 4;
                                break;
                            case 3:
                                type = 5;
                                break;
                            default:
                                type = 1;
                                break;
                        }
                        //创建新用户
                        DWUser dwUser = new DWUser();
                        //录入用户信息
                        dwUser.setPhone(chargeCard.getCardNumber());
                        dwUser.setType(type);
                        dwUser.setStatus(2);
                        dwUser.setUserSrc(3);
                        dwUser.setMonthCard(3);
                        dwUser.setEnterpriseId(chargeCard.getEnterpriseId());
                        //dwUser.setEnterpriseId(0);
                        //调用新增用户方法
                        isCorrect = dwUserService.addDWUserInfo(dwUser);
                        //校验是否调用成功
                        if(isCorrect > 0){
                            //录入卡信息
                            chargeCard.setUserId(dwUser.getId());
                            chargeCard.setBeginedAt(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
                            //调用录入卡号方法
                            isRight = chargeCardService.addChargeCardInfo(chargeCard);

                            //校验是否调用成功
                            if(isRight < 0){
                                return new ResponseEntity<CommonResult>(CommonResult.buildErrorResult(1,"new cardNumber method call failed.",null), HttpStatus.OK);
                            }
                        }else{
                            return new ResponseEntity<CommonResult>(CommonResult.buildErrorResult(1,"new user method call failed.",null), HttpStatus.OK);
                        }
                    }
                }
                log.info("结束时间：{}", new Date());
                log.debug("重复个数：{}", repeatCount);
                log.debug("repeatCardList：{}", repeatCardList);
                log.debug("不重复个数：{}", noRepeatCount);
                log.debug("noRepeatCardList：{}", noRepeatCardList);
            } else {
                return new ResponseEntity<CommonResult>(CommonResult.buildErrorResult(1,"collection has no data.",null), HttpStatus.OK);
            }
            //定义一个数组，来接收卡号
            String [] resultCardNumber = null;
            //定义一个对象，来接收JSON转化的数组
            Object card = null;
            //校验卡号是否有重复的
            if(chargeCardList.size()==repeatCount){
                resultCardNumber = new String[repeatCardList.size()];
                for(int i = 0; i < repeatCardList.size(); i++){
                    resultCardNumber[i] = repeatCardList.get(i).getCardNumber();
                }
                card = JSON.toJSON(resultCardNumber);
                return new ResponseEntity<CommonResult>(CommonResult.buildErrorResult(1,"repeat collection has data and noRepeat collection has no data.",card), HttpStatus.OK);
            }else{
                resultCardNumber = new String[noRepeatCardList.size()];
                for(int i = 0; i < noRepeatCardList.size(); i++){
                    resultCardNumber[i] = noRepeatCardList.get(i).getCardNumber();
                }
                card = JSON.toJSON(resultCardNumber);
                return new ResponseEntity<CommonResult>(CommonResult.buildSuccessResult(card), HttpStatus.OK);
            }
        }

    }

    public ResponseEntity<CommonResult> defaultSendMessage(@RequestBody String req) {
        log.info("[defaultSendMessage][req]{}", req);
        return new ResponseEntity<CommonResult>(CommonResult.buildResult(-1), HttpStatus.OK);
    }
}
