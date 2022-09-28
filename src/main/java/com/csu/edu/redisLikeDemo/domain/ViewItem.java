package com.csu.edu.redisLikeDemo.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("view_item")
public class ViewItem {
    @TableId
    private String id;
    @TableField("create_user")
    private String createUser;

    @TableField("like_count")
    private Integer likeCount;
    @TableField("cmt_count")
    private Integer cmtCount;
    @TableField("share_count")
    private Integer shareCount;

    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}