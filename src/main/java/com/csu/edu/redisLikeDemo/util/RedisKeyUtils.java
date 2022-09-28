package com.csu.edu.redisLikeDemo.util;

public class RedisKeyUtils {
    /**
     *
     保存用户点赞内容数据的key
     * @date 2021/9/26 14:44
     */
    public static final String MAP_KEY_USER_LIKED = "MAP_USER_LIKED";
    /**
     *
     保存内容被点赞数量的key
     * @date 2021/9/26 14:44
     */
    public static final String MAP_KEY_USER_LIKED_COUNT = "MAP_USER_LIKED_COUNT";

    /**
     * 拼接被点赞的内容id和点赞的人的id作为key。格式 222222::333333
     * @param infoId 被点赞的内容 id
     * @param likeUserId 点赞的人的id
     * @return
     */
    public static String getLikedKey(String infoId, String likeUserId){
        return infoId +
                "::" +
                likeUserId;
    }
}