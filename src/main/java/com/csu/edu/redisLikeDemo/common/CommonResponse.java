package com.csu.edu.redisLikeDemo.common;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
//Springboot Jackson序列化为JSON字符串
@JsonInclude(JsonInclude.Include.NON_NULL)//仅仅序列化非空值
public class CommonResponse<T> {
    private Integer code;
    private String message;
    private T data;

    protected CommonResponse(Integer code, String message, T data){
        this.code = code;
        this.message = message;
        this.data = data;
    }

    //请求成功，无数据返回
    public static <T> CommonResponse<T>  createForSuccess(){
        return new CommonResponse<>(ResponseCode.SUCCESS.getCode(),ResponseCode.SUCCESS.getDescription(),null);
    }

    //请求成功，并返回响应数据
    public static <T> CommonResponse<T> createForSuccess(T data){
        return new CommonResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getDescription(),  data);
    }

    //请求错误，默认错误信息
    public static <T> CommonResponse<T> createForError(){
        return new CommonResponse<>(ResponseCode.ERROR.getCode(), ResponseCode.ERROR.getDescription(), null);
    }

    //请求错误，指定错误信息
    public static <T> CommonResponse<T> createForError(String message){
        return new CommonResponse<>(ResponseCode.ERROR.getCode(), message, null);
    }

    //请求错误，指定错误码和错误信息
    public static <T> CommonResponse<T> createForError(Integer code, String message){
        return new CommonResponse<>(code, message, null);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ResponseCode.SUCCESS.getCode();
    }
}