package com.csu.edu.redisLikeDemo.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_likes")
public class UserLikes {
    @TableId
    private String id;
    @TableField("info_id")
    private String infoId;
    @TableField("like_user_id")
    private String likeUserId;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
