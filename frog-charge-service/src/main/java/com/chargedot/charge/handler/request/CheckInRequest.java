package com.chargedot.charge.handler.request;

import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/26
 */
@Data
public class CheckInRequest extends Request {

    private String deviceNumber;

    private int connectNetMode;

    private int authorType;
}
