package com.csu.edu.redisLikeDemo.service.impl;

import com.csu.edu.redisLikeDemo.common.CONSTANT;
import com.csu.edu.redisLikeDemo.common.CommonResponse;
import com.csu.edu.redisLikeDemo.service.LikeService;
import com.csu.edu.redisLikeDemo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("likeService")
public class LikeServiceImpl implements LikeService {

    @Autowired
    private RedisService redisService;

    @Override
    public CommonResponse<Object> likeInfo(String infoId, String userId) {
        // 查询Redis是否已经存储为喜欢
        Integer status = redisService.getLikeStatus(infoId, userId);
        if (status == CONSTANT.LikedStatusEum.LIKE.getCode()){// 已经存在喜欢
            return CommonResponse.createForSuccess("已经点赞该内容啦，请勿重复点赞！");
        }
        // 不存在或者之前是取消喜欢
        try {
            redisService.saveLiked2Redis(infoId, userId);
            redisService.in_decrementLikedCount(infoId,1);
            return CommonResponse.createForSuccess("喜欢内容写入redis缓存成功");
        }catch (Exception e){
            e.printStackTrace();
            return CommonResponse.createForError("喜欢内容写入redis缓存失败，请稍后重试！");
        }
    }

    @Override
    public CommonResponse<Object> dislikeInfo(String infoId, String userId) {
        // 查询Redis是否已经存储为取消喜欢
        Integer status = redisService.getLikeStatus(infoId, userId);
        if (status == CONSTANT.LikedStatusEum.UNLIKE.getCode()){// 已经存在取消喜欢
            return CommonResponse.createForSuccess("已经取消点赞该内容啦，请勿重复取消点赞！");
        }
        else if (status == CONSTANT.LikedStatusEum.NOT_EXIST.getCode()) {// 不存在取消喜欢，修改状态，增加0条
            redisService.unlikeFromRedis(infoId, userId);
            redisService.in_decrementLikedCount(infoId,0);
            return CommonResponse.createForSuccess("取消喜欢内容写入redis缓存成功");
        }
        else{// 之前已经喜欢，则修改状态,并喜欢数-1
            try {
                redisService.unlikeFromRedis(infoId, userId);
                redisService.in_decrementLikedCount(infoId,-1);
                return CommonResponse.createForSuccess("取消喜欢内容写入redis缓存成功");
            }catch (Exception e){
                e.printStackTrace();
                return CommonResponse.createForError("取消喜欢内容写入redis缓存失败，请稍后重试！");
            }
        }
    }
}
