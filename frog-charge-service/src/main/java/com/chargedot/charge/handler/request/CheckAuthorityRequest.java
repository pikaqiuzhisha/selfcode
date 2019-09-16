package com.chargedot.charge.handler.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Eric Gui
 * @date 2019/4/16
 */
public class CheckAuthorityRequest extends Request {

    @JsonProperty("CardNumber")
    String cardNumber;

    @JsonProperty("Port")
    String port;

    @JsonProperty("SeqNumber")
    int seqNumber;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }
}
