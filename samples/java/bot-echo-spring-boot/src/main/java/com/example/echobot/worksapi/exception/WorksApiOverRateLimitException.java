package com.example.echobot.worksapi.exception;

public class WorksApiOverRateLimitException extends WorksApiBaseException {
    public WorksApiOverRateLimitException(String msg) {
        super(msg);
    }
}

