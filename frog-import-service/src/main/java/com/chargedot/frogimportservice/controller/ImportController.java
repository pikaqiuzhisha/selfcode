package com.chargedot.frogimportservice.controller;

import com.chargedot.frogimportservice.controller.vo.CommonResult;
import com.chargedot.frogimportservice.model.DWCert;
import com.chargedot.frogimportservice.model.vo.DataParam;
import com.chargedot.frogimportservice.service.DWCertService;
import com.chargedot.frogimportservice.util.JacksonUtil;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("/cert")
public class ImportController {

    //创建凭证管理业务对象
    @Autowired
    private DWCertService dwCertService;

    @HystrixCommand(fallbackMethod = "defaultSendMessage")
    @RequestMapping(value = "/cert_import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CommonResult> importData(@RequestBody String data) {

        //校验参数
        if (StringUtils.isBlank(data)) {
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "参数错误.", null), HttpStatus.OK);
        }

        //JSONObject转成JAVA对象
        DataParam dataParam = JacksonUtil.json2Bean(data, DataParam.class);

        //校验参数的解析是否正确
        if (Objects.isNull(dataParam)) {
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "参数解析错误.", null), HttpStatus.OK);
        }

        //获取凭证号集合
        List<DWCert> dwCertList = dataParam.getDwCertList();
        //获取公司id
        int enterpriseId = dataParam.getEnterpriseId();
        //获取有效期
        String finishedAt = dataParam.getFinishedAt();
        //获取类型
        int type = dataParam.getType();

        //校验参数
        if (Objects.isNull(dwCertList)) {
            return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "集合获取错误.", null), HttpStatus.OK);
        } else {
            log.debug("req：{}", dwCertList);
            //存储重复的凭证号
            List<DWCert> repeatCertList = new ArrayList<>();
            //存储唯一凭证号
            List<DWCert> noRepeatCertList = new ArrayList<>();

            if (log.isDebugEnabled()) {
                log.debug("cert条数：{}", dwCertList.size());
            }
            log.info("开始时间：{}", new Date());
            //定义变量,来校验录凭证号是否成功
            int isRight = 0;
            //标识重复的个数
            int repeatCount = 0;
            //标识唯一的个数
            int noRepeatCount = 0;

            //校验集合里是否存在数据
            if (dwCertList.size() > 0) {
                //循环校验是否存在重复的卡号
                for (DWCert dwCert : dwCertList) {
                    //设置归属公司、有效期、类型
                    dwCert.setEnterpriseId(enterpriseId);
                    dwCert.setFinishedAt(finishedAt);
                    dwCert.setType(type);
                    if (dwCertService.selectDWCertNumberCount(dwCert.getCertNumber())) {
                        repeatCertList.add(dwCert);
                        repeatCount++;
                    } else {
                        noRepeatCertList.add(dwCert);
                        noRepeatCount++;
                    }
                }
                //移除凭证号集合中重复的凭证号信息，得到真正不重复的凭证号集合
                for ( int i = 0; i < noRepeatCertList.size() - 1; i ++ ) {
                    for ( int j = noRepeatCertList.size() - 1; j > i; j -- ) {
                        if (noRepeatCertList.get(j).equals(noRepeatCertList.get(i))) {
                            noRepeatCertList.remove(j);
                            noRepeatCount--;
                        }
                    }
                }
                log.info("结束时间：{}", new Date());
                if (log.isDebugEnabled()) {
                    log.debug("重复个数：{}", repeatCount);
                    log.debug("repeatCertList：{}", repeatCertList);
                    log.debug("不重复个数：{}", noRepeatCount);
                    log.debug("noRepeatCertList：{}", noRepeatCertList);
                }
            } else {
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "集合无数据.", null), HttpStatus.OK);
            }

            //定义一个数组，来接收凭证号
            String[] resultCertNumber = null;
            //定义一个对象，来接收JSON转化的数组
            Object cert = null;
            //校验凭证号是否有重复的
            if (dwCertList.size() == repeatCount) {
                resultCertNumber = new String[repeatCertList.size()];
                for (int i = 0; i < repeatCertList.size(); i++) {
                    resultCertNumber[i] = repeatCertList.get(i).getCertNumber();
                }
                cert = JacksonUtil.bean2Json(resultCertNumber);
                return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "凭证号已存在.", cert), HttpStatus.OK);
            } else {
                //调用批量导入凭证号方法
                isRight = dwCertService.importDWCertData(noRepeatCertList);
                resultCertNumber = new String[noRepeatCertList.size()];
                for (int i = 0; i < noRepeatCertList.size(); i++) {
                    resultCertNumber[i] = noRepeatCertList.get(i).getCertNumber();
                }
                cert = JacksonUtil.bean2Json(resultCertNumber);
                //校验是否调用成功
                if (isRight < 0) {
                    return new ResponseEntity<CommonResult>(CommonResult.buildResults(1, "调用批量导入方法错误.", null), HttpStatus.OK);
                } else {
                    return new ResponseEntity<CommonResult>(CommonResult.buildResults(0, "导入成功.", cert), HttpStatus.OK);
                }
            }
        }

    }

    public ResponseEntity<CommonResult> defaultSendMessage(@RequestBody String req) {
        log.info("[defaultSendMessage][req]{}", req);
        return new ResponseEntity<CommonResult>(CommonResult.buildResult(-1), HttpStatus.OK);
    }
}
