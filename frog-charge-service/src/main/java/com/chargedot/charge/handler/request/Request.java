/**
 * 
 */
package com.chargedot.charge.handler.request;

/**
 * @author gmm
 *
 */
public abstract class Request {

    /**
     * event type
     */
    private int type;
    /**
     * created at
     */
    private long createdAt;

    /**
     * 
     */
    public Request() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}