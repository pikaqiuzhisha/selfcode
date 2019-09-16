/**
 * 
 */
package com.chargedot.charge.handler.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author gmm
 *
 */
public abstract class Request {

    @JsonProperty("OperationType")
    private String operationType;

    /**
     * deviceNumber
     */
    @JsonProperty("DeviceNumber")
    String deviceNumber;

    /**
     * created at
     */
    @JsonProperty("createdAt")
    private long createdAt;

    /**
     * 
     */
    public Request() {
    }


    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDeviceNumber() {
        return deviceNumber;
    }

    public void setDeviceNumber(String deviceNumber) {
        this.deviceNumber = deviceNumber;
    }
}