import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import org.junit.Test;

public class WxTest {
    @Test
    public void  decodetest() throws WxPayException {



        String xmlData="<xml>\n" +
                "    <return_code>SUCCESS</return_code>\n" +
                "    <appid>\n" +
                "        <![CDATA[wx3d287da002d86404]]>\n" +
                "    </appid>\n" +
                "    <mch_id>\n" +
                "        <![CDATA[1510523931]]>\n" +
                "    </mch_id>\n" +
                "    <nonce_str>\n" +
                "        <![CDATA[3f084ab199125f8e1c95e0889269540e]]>\n" +
                "    </nonce_str>\n" +
                "    <req_info>\n" +
                "        <![CDATA[+oesxnpQkJIKxJe6M7B7hmCHWVM4jgLW85mFK6Cex8QEj5Fwd9bbRXb2kkQWTwPxH5f2Li6067G0CHdzpPUu2jKcDhX5/slSZUDV21qQfhKHlAb9oT/pRAuEep33FOsXaYoyDD6PqxEYmwd+qzHXuXHGeJc6m5gdpCY8BUs6Fv02OhF632SYSJMIs/vEFx6TWI9QCfRgz8G4mPfHoG0ob9194dZauwRaVg29vYn4Ln9Hvhf7497agHeUoExBKVBt/IO+NumUVY1b1Ry/glgwNU3OI2ljlWb3uE4igUH43wqT6Z91SIMGvY26HysYoylPaEIKuuZhJLVXTKfy688vDizZq1fGVDKr/6IgPuOP3Hf1mXwH1angd5B5kjrE+IyR3GAff9eu1CwZJ2PvZm/8u3GwZiP7w2yVc7tx3Pav4EuHpJuAiZzNBOROkIODB4/hxU4Xg7Z9MrwW/XqFyLcXPuGb6GM5+f8/xMMjz0L6uI5SCyUSZlUvC/G0PnWPypu9Uy0eyaGUsdXw/kqhwm/QyerSu/sRSVOXKuW/TREHXAcnmKUDBC5Nv8UN+TQzpm3CTJw9XO4uJoU7RYA2rZziJIGFR2IXeb+8saxklj0R82LOF24E6vzF8qMY8Y2xI5l76I08cyZ3SdB/TnFdrJZOoVQiQJGDjq6JwaWcHC7iLeg6Gw+HSllukWmh+uFxIEkQOINYOPjpnIwFjBxwk6wmRhsmxKbT6MWRBYY98Db8S6M1PAM8DtgrE5gOocX40XDTnc0xYUgwjBLNB6OYf4KRPkT+wTQRW56MQoRpAzTg2bhrcxQoFkDj7YO2rAbppDTZTuQz/NXLiq6Y6omn/xH+IVuMh8A3OXJoyMnSY7Dy5OANltMP5ffpgyS+TvaMObR1/1/f8PCdk7r7yknSFGYUvVGSdVf1If0aWDMgbiIAUj2Bwj+Q6FKM6m1n/Q6uay5afKo89atRQkhQyRrBI5ZIRfGMUqf4QBBIMjK3c7vNUuwIXk4cUPO8docuVA3YV5SPRv/GyaBMFLSb3/qmFIt6L2BObtlT8HWJHbYp0+0ClkbjHrBEhu8OvZ2VSjyzh3OW]]>\n" +
                "    </req_info>\n" +
                "</xml>";


        WxPayRefundNotifyResult result = WxPayRefundNotifyUtils.fromXML(xmlData, "elecfrog201807261634363683848380");

        System.out.println("result = " + result);
    }
}
