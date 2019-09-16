package com.chargedot.charge.handler.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Eric Gui
 * @date 2019/4/26
 */
@Data
public class CheckInRequest extends Request {

    @JsonProperty("ConnectNetMode")
    private int connectNetMode;

    @JsonProperty("AuthorType")
    private int authorType;
}
