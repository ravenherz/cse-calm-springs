package com.ravenherz.rhzwe.controller.objects;

import java.io.Serializable;

public final class RestResponse implements Serializable {
    private Integer status;
    private Object restObject;
    private String message;

    public RestResponse() {
    }

    public RestResponse(Integer status) {
        this.status = status;
    }

    public RestResponse(Integer status, Object restObject, String message) {
        this.status = status;
        this.restObject = restObject;
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Object getRestObject() {
        return restObject;
    }

    public void setRestObject(Object restObject) {
        this.restObject = restObject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
