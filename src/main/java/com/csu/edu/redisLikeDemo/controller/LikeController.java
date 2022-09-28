package com.csu.edu.redisLikeDemo.controller;

import com.csu.edu.redisLikeDemo.common.CommonResponse;
import com.csu.edu.redisLikeDemo.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/")
public class LikeController {
    @Autowired
    private LikeService likeService;

    @PostMapping("like")
    public CommonResponse<Object> likeInfo(
            String infoId,
            String userId){
        return likeService.likeInfo(infoId, userId);
    }

    @PostMapping("dislike")
    public CommonResponse<Object> dislikeInfo(
            String infoId,
            String userId){
        return likeService.dislikeInfo(infoId, userId);
    }
}
