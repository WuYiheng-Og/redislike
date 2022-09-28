package com.csu.edu.redisLikeDemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csu.edu.redisLikeDemo.common.CONSTANT;
import com.csu.edu.redisLikeDemo.domain.DTO.UserLikeCountDTO;
import com.csu.edu.redisLikeDemo.domain.DTO.UserLikesDTO;
import com.csu.edu.redisLikeDemo.domain.UserLikes;
import com.csu.edu.redisLikeDemo.domain.ViewItem;
import com.csu.edu.redisLikeDemo.mapper.UserLikesMapper;
import com.csu.edu.redisLikeDemo.mapper.ViewItemMapper;
import com.csu.edu.redisLikeDemo.service.DBService;
import com.csu.edu.redisLikeDemo.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.n3r.idworker.Sid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service("dbService")
@Slf4j
public class DBServiceImpl implements DBService {

    @Autowired
    private RedisService redisService;
    @Autowired
    private UserLikesMapper userLikesMapper;
    @Autowired
    private ViewItemMapper viewItemMapper;
    @Autowired
    private Sid sid;// Id生成器，利用idWorker产生唯一（不重复）自增式的id，可以根据需求选用其他方式

    @Override
    public Boolean save(UserLikes userLike) {
        int rows = userLikesMapper.insert(userLike);
        return rows > 0;
    }

    @Override
    public Boolean update(UserLikes userLike) {
        UpdateWrapper<UserLikes> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("status", userLike.getStatus());
        updateWrapper.set("update_time",userLike.getUpdateTime());
        updateWrapper.eq("id",userLike.getId());

        int rows = userLikesMapper.update(userLike,updateWrapper);
        return rows > 0;
    }

    @Override
    public Page<UserLikes> getLikedListByInfoId(String infoId, int pageNum, int pageSize) {
        // 分页获取喜欢列表信息
        Page<UserLikes> result = new Page<>();
        result.setCurrent(pageNum);
        result.setSize(pageSize);

        // 获取内容的id查询点赞列表
        QueryWrapper<UserLikes> queryWrapper = new QueryWrapper();
        queryWrapper.eq("info_id",infoId);
        result = userLikesMapper.selectPage(result, queryWrapper);
        log.info("获得内容的id查询点赞列表（即查询都有谁给这个内容点赞过）");
        return result;
    }

    @Override
    public Page<UserLikes> getLikedListByLikeUserId (String likeUserId, int pageNum, int pageSize) {
        // 分页获取喜欢列表信息
        Page<UserLikes> result = new Page<>();
        result.setCurrent(pageNum);
        result.setSize(pageSize);

        // 获取用户的id查询点赞列表
        QueryWrapper<UserLikes> queryWrapper = new QueryWrapper();
        queryWrapper.eq("like_user_id", likeUserId);
        result = userLikesMapper.selectPage(result, queryWrapper);
        log.info("获取点赞人的id查询点赞列表（即查询这个人都给哪些内容点赞过）");
        return result;
    }

    @Override
    public UserLikes getByInfoIdAndLikeUserId (String infoId, String likeUserId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("info_id",infoId);
        map.put("like_user_id",likeUserId);
        try{
            UserLikes userLikes = userLikesMapper.selectByMap(map).get(0);
            log.info("通过被点赞人和点赞人id查询是否存在点赞记录");
            return userLikes;
        }catch (Exception e){
            log.info("当前查询的被点赞人和点赞人id不存在点赞记录");
            return null;
        }
    }

    @Override
    public void transLikedFromRedis2DB() {
        // 批量获取缓存中的点赞数据
        List<UserLikesDTO> list = redisService.getLikedDataFromRedis();
        if (CollectionUtils.isEmpty(list))// 为空，不写入
            return;
        for (UserLikesDTO item: list){
            UserLikes userLikes = getByInfoIdAndLikeUserId(item.getInfoId(), item.getLikeUserId());// 在数据库中查询
            if (userLikes == null) {// 无记录，新增
                if(!save(userLikesDTOtoUserLikes(item))){
                    log.info("新增点赞数据失败！");
                    return;
                // System.out.println("缓存记录写入数据库失败！请重试");
                }
            }else {// 有记录，更新
                // 判断数据库中点赞状态与缓存中点赞状态一致性
                if (userLikes.getStatus()==item.getStatus()){// 一致，无需持久化，点赞数量-1
                    redisService.in_decrementLikedCount(item.getInfoId(), -1);
                }else{// 不一致
                    if (userLikes.getStatus()== CONSTANT.LikedStatusEum.LIKE.getCode()){// 在数据库中已经是点赞，则取消点赞，同时记得redis中的count-1
                        // 之前是点赞，现在改为取消点赞 1.设置更改status 2. redis中的count要-1（消除在数据库中自己的记录）
                        userLikes.setStatus(CONSTANT.LikedStatusEum.UNLIKE.getCode());
                        redisService.in_decrementLikedCount(item.getInfoId(), -1);
                    } else if (userLikes.getStatus()== CONSTANT.LikedStatusEum.UNLIKE.getCode()) {// 未点赞，则点赞，修改点赞状态和点赞数据+1
                        userLikes.setStatus(CONSTANT.LikedStatusEum.LIKE.getCode());
                        redisService.in_decrementLikedCount(item.getInfoId(), 1);
                    }
                    userLikes.setUpdateTime(LocalDateTime.now());
                    if(!update(userLikes)){// 更新点赞数据
                        log.info("更新点赞数据失败！");
                        return;
                    // System.out.println("缓存记录更新数据库失败！请重试");
                    }
                }
            }
        }
    }

    @Override
    public void transLikedCountFromRedis2DB() {
// 获取缓存中内容的点赞数列表
        List<UserLikeCountDTO> list = redisService.getLikedCountFromRedis();
        if (CollectionUtils.isEmpty(list))// 为空，不写入
            return;
        for (UserLikeCountDTO item: list){
            ViewItem viewItem = viewItemMapper.selectById(item.getInfoId());
            if (viewItem != null) {// 新增点赞数
                Integer likeCount = viewItem.getLikeCount() + item.getLikeCount();
                System.out.println("内容id不为空，更新内容点赞数量");
                viewItem.setLikeCount(likeCount);

                UpdateWrapper<ViewItem> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("like_count", viewItem.getLikeCount());
                updateWrapper.eq("id", viewItem.getId());
                int rows = viewItemMapper.update(viewItem, updateWrapper);
                if (rows > 0) {
                    System.out.println("成功更新内容点赞数！");
                    return;
                }
            }
            System.out.println("内容id不存在，无法将缓存数据持久化！");
        }
    }

    private UserLikes userLikesDTOtoUserLikes(UserLikesDTO userLikesDTO){
        UserLikes userLikes = new UserLikes();
        userLikes.setId(sid.nextShort());
        BeanUtils.copyProperties(userLikesDTO,userLikes);
        userLikes.setCreateTime(LocalDateTime.now());
        userLikes.setUpdateTime(LocalDateTime.now());
        return userLikes;
    }
}
