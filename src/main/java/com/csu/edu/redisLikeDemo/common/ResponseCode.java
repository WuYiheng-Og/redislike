package com.csu.edu.redisLikeDemo.common;


import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS(0,"SUCCESS"),
    ERROR(1,"ERROR"),
    ARGUMENT_ILLEGAL(10,"ARGUMENT_ILLEGAL"),
    NEED_LOGIN(11,"NEED_LOGIN"),
    PAGE_OUT_RANGE(12,"PAGE_OUT_RANGE"),
    TOKEN_INVALID(13,"TOKEN_INVALID");

    private final int code;
    private final String description;

    ResponseCode(int code, String description){
        this.code = code;
        this.description = description;
    }
}