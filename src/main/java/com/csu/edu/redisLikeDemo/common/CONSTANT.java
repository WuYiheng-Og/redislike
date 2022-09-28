package com.csu.edu.redisLikeDemo.common;

import lombok.Getter;

public class CONSTANT {
    @Getter
    public enum LikedStatusEum {
        LIKE(0, "点赞"),
        UNLIKE(1, "未点赞/取消点赞"),
        NOT_EXIST(2,"不存在点赞信息");

        private final int code;
        private final String description;

        LikedStatusEum(Integer code, String description) {
            this.code = code;
            this.description = description;
        }
    }
}
