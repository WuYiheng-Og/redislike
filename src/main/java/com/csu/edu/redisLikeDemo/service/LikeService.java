package com.csu.edu.redisLikeDemo.service;

import com.csu.edu.redisLikeDemo.common.CommonResponse;

public interface LikeService {
    /**
     * 喜欢数据到缓存
     */
    CommonResponse<Object> likeInfo(String infoId, String userId);

    /**
     * 取消喜欢数据到缓存
     */
    CommonResponse<Object> dislikeInfo(String infoId,String userId);
}
