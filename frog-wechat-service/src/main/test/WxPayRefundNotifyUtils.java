import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
import com.github.binarywang.wxpay.bean.result.BaseWxPayResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class WxPayRefundNotifyUtils {

    public static WxPayRefundNotifyResult fromXML(String xmlString, String mchKey) throws WxPayException {
        WxPayRefundNotifyResult result = (WxPayRefundNotifyResult) BaseWxPayResult.fromXML(xmlString, WxPayRefundNotifyResult.class);
        if ("FAIL".equals(result.getReturnCode())) {
            return result;
        } else {
            String reqInfoString = result.getReqInfoString();

            try {
                String keyMd5String = DigestUtils.md5Hex(mchKey).toLowerCase();
                SecretKeySpec key = new SecretKeySpec(keyMd5String.getBytes(StandardCharsets.UTF_8), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
                cipher.init(2, key);
                result.setReqInfo(WxPayRefundNotifyResult.ReqInfo.fromXML(new String(cipher.doFinal(Base64.decodeBase64(reqInfoString)), StandardCharsets.UTF_8)));
                return result;
            } catch (Exception var7) {
                throw new WxPayException("解密退款通知加密信息时出错", var7);
            }
        }
    }

}