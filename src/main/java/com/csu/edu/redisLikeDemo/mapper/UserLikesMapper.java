package com.csu.edu.redisLikeDemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csu.edu.redisLikeDemo.domain.UserLikes;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLikesMapper extends BaseMapper<UserLikes> {
}
