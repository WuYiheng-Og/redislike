# 0.前言

本文基于Springboot利用Redis实现点赞功能的缓存和定时持久化接口。

用户对浏览内容进行【点赞/取赞】，并发送【点赞/取赞】请求到后端，这些信息先存入Redis中缓存，再每隔两小时将Redis中的内容直接写入数据库持久化存储。

# 1.项目目录结构

![image-20220928151522321](http://42.193.105.222:9090/index.php/s/QD39geamcPxSNXk/preview)

# 2.Redis缓存点赞消息

## 1.设计思路

- 用户点赞一条数据，设置状态为0，并且更新被点赞内容的likeCount+1
- 用户取消点赞一条数据，设置状态为1，并且更新被点赞内容的likeCount+0

### 1.1 Redis键值对设计

选用Hash（散列）存储

1. 点赞信息
   - key: (String) 浏览信息id和点赞用户id拼接而成, 分隔符为::
   - value: (HashMap) 存储点赞状态(0: 点赞 1:取消点赞)和更新时间的时间戳
   - ```key: "浏览信息id::点赞用户id" value: {status: Integer, updateTime: long}```
2. 点赞数量
   - key: (String) 浏览信息id
   - value: (Integer) 点赞数量

 ![点赞信息表](http://42.193.105.222:9090/index.php/s/E3akb6Kd7p9qQc8/preview)

![点赞数量表](http://42.193.105.222:9090/index.php/s/c8xEnqGYK2kQHga/preview)

### 1.2 点赞

![image-20220928083021122](http://42.193.105.222:9090/index.php/s/CAE8gyTeK5qp77d/preview)

1. 用户点赞信息，发送点赞请求到后端
2. 后端判断该点赞信息在Redis中的状态
   - 【不存在（没有对应key) 】|| 【取消点赞（即取出的status为1）】
     1. 更新/新增点赞信息
     2. 更新/新增点赞量
   - 【点赞（即取出的status为0，此时相当于重复点赞行为）】
     - Redis不进行存储，并提醒前端重复存储。
3. 一次点赞请求完毕

### 1.3 取消点赞

![image-20220928091056812](http://42.193.105.222:9090/index.php/s/kAj7Scn3kef3cGg/preview)

1. 用户取消点赞信息，发送取消点赞请求到后端
2. 后端判断该点赞信息在Redis中的状态
   - 【不存在（没有对应key) 】
     1. 更新/新增点赞信息
     2. 增加0条内容点赞量
   - 【取消点赞（即取出的status为1，此时相当于重复取消点赞行为）】
     - Redis不进行存储，并提醒前端重复存储。
   - 【点赞（即取出的status为0）】
     1. 更新/新增点赞信息
     2. 更新/新增点赞量
3. 一次取消点赞请求完毕

## 2.核心代码实现

### 2.1 Redis封装

> 具体实现参考该博客，不在赘述。https://www.cnblogs.com/caizhaokai/p/11037610.html

### 2.2 工具类

#### 1.时间戳转化为LocalDateTime

```java
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 工具类：将时间戳转化为LocalDateTime
 * 主要是因为redis不好存储LocalDateTime，存储timestamp方便一点，而且格式可以随意改变
 */
public class LocalDateTimeConvertUtil {
    public static LocalDateTime getDateTimeOfTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZoneId zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }
}
```

#### 2.RedisKey处理类

```java
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
```



### 2.3 DTO

```java
// UserLikesDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLikesDTO {
    private String infoId;
    private String likeUserId;
    private Integer status;
    private LocalDateTime updateTime;
}

// UserLikeCountDTO.java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLikeCountDTO {
    private String infoId;
    private Integer likeCount;
}
```

### 2.4 Service

#### 1.interface

```java
// RedisService.java

import com.csu.edu.redisLikeDemo.domain.DTO.UserLikeCountDTO;
import com.csu.edu.redisLikeDemo.domain.DTO.UserLikesDTO;

import java.util.List;

/**
 * 负责将数据写入Redis缓存
 */
public interface RedisService {
    /**
     * 获取点赞状态
     * @param infoId
     * @param likeUserId
     */
    Integer getLikeStatus(String infoId, String likeUserId);
    /**
     * 点赞。状态为1
     * @param infoId
     * @param likeUserId
     */
    void saveLiked2Redis(String infoId, String likeUserId);

    /**
     * 取消点赞。将状态改变为0
     * @param infoId
     * @param likeUserId
     */
    void unlikeFromRedis(String infoId, String likeUserId);

    /**
     * 从Redis中删除一条点赞数据
     * @param infoId
     * @param likeUserId
     */
    void deleteLikedFromRedis(String infoId, String likeUserId);

    /**
     * 该内容的点赞数变化Δdelta
     * @param infoId
     */
    void in_decrementLikedCount(String infoId, Integer delta);

    /**
     * 获取Redis中存储的所有点赞数据
     * @return
     */
    List<UserLikesDTO> getLikedDataFromRedis();

    /**
     * 获取Redis中存储的所有点赞数量
     * @return
     */
    List<UserLikeCountDTO> getLikedCountFromRedis();
}
```

#### 2.implement

```java
import com.csu.edu.redisLikeDemo.common.CONSTANT;
import com.csu.edu.redisLikeDemo.domain.DTO.UserLikeCountDTO;
import com.csu.edu.redisLikeDemo.domain.DTO.UserLikesDTO;
import com.csu.edu.redisLikeDemo.service.RedisService;
import com.csu.edu.redisLikeDemo.util.LocalDateTimeConvertUtil;
import com.csu.edu.redisLikeDemo.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("redisService")
@Slf4j
public class RedisServiceImpl implements RedisService {

    @Autowired
    private HashOperations<String, String, Object> redisHash;// Redis Hash

    @Override
    public Integer getLikeStatus(String infoId, String likeUserId) {
        if (redisHash.hasKey(RedisKeyUtils.MAP_KEY_USER_LIKED, RedisKeyUtils.getLikedKey(infoId, likeUserId))){
            HashMap<String, Object> map = (HashMap<String, Object>) redisHash.get(RedisKeyUtils.MAP_KEY_USER_LIKED, RedisKeyUtils.getLikedKey(infoId, likeUserId));
            return (Integer) map.get("status");
        }
        return CONSTANT.LikedStatusEum.NOT_EXIST.getCode();
    }

    @Override
    public void saveLiked2Redis(String infoId, String likeUserId) {
        // 生成key
        String key = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        // 封装value 喜欢状态 更新时间
        HashMap<String,Object> map = new HashMap<>();
        map.put("status",CONSTANT.LikedStatusEum.LIKE.getCode());
        map.put("updateTime", System.currentTimeMillis());

        redisHash.put(RedisKeyUtils.MAP_KEY_USER_LIKED, key, map);
    }

    @Override
    public void unlikeFromRedis(String infoId, String likeUserId) {
        // 生成key
        String key = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        // 封装value 喜欢状态 更新时间
        HashMap<String,Object> map = new HashMap<>();
        map.put("status",CONSTANT.LikedStatusEum.UNLIKE.getCode());
        map.put("updateTime", System.currentTimeMillis());// 存入当前时间戳

        redisHash.put(RedisKeyUtils.MAP_KEY_USER_LIKED, key, map);
    }

    @Override
    public void deleteLikedFromRedis(String infoId, String likeUserId) {
        String key = RedisKeyUtils.getLikedKey(infoId, likeUserId);
        redisHash.delete(RedisKeyUtils.MAP_KEY_USER_LIKED, key);
    }

    @Override
    public void in_decrementLikedCount(String infoId, Integer delta) {
        redisHash.increment(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, infoId, delta);
    }

    @Override
    public List<UserLikesDTO> getLikedDataFromRedis() {
        // scan 读取数据，比key匹配优雅
        Cursor<Map.Entry<String, Object>> cursor = redisHash.scan(RedisKeyUtils.MAP_KEY_USER_LIKED, ScanOptions.NONE);

        List<UserLikesDTO> list = new ArrayList<>();
        while (cursor.hasNext()){
            Map.Entry<String, Object> entry = cursor.next();
            String key = (String) entry.getKey();
            //分离出 infoId，likedPostId, 解析value
            String[] split = key.split("::");
            String infoId = split[0];
            String likeUserId = split[1];
            HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
            Integer status = (Integer) map.get("status");
            long updateTimeStamp = (long) map.get("updateTime");
            LocalDateTime updateTime = LocalDateTimeConvertUtil.getDateTimeOfTimestamp(updateTimeStamp);// 时间戳转为LocalDateTime

            //组装成 UserLike 对象
            UserLikesDTO userLikesDTO = new UserLikesDTO(infoId, likeUserId, status, updateTime);
            list.add(userLikesDTO);

            //存到 list 后从 Redis 中清理缓存
            redisHash.delete(RedisKeyUtils.MAP_KEY_USER_LIKED, key);
        }
        return list;
    }

    @Override
    public List<UserLikeCountDTO> getLikedCountFromRedis() {
        // scan 读取数据，比key匹配优雅
        Cursor<Map.Entry<String, Object>> cursor = redisHash.scan(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, ScanOptions.NONE);
        List<UserLikeCountDTO> list = new ArrayList<>();
        while (cursor.hasNext()){
            Map.Entry<String, Object> map = cursor.next();
            //将点赞数量存储在 LikedCountDT
            String key = (String)map.getKey();
            UserLikeCountDTO dto = new UserLikeCountDTO(key, (Integer) map.getValue());
            list.add(dto);
            //从Redis中删除这条记录
            redisHash.delete(RedisKeyUtils.MAP_KEY_USER_LIKED_COUNT, key);
        }
        return list;
    }
}
```
### 2.5 controller & API

#### 1.controller

```java
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
```

#### 2.接口测试

![点赞](http://42.193.105.222:9090/index.php/s/r5rmZa4EkJGQMCp/preview)

![取消点赞](http://42.193.105.222:9090/index.php/s/FMPar8nxgkJ4SEw/preview)

![重复取消点赞](http://42.193.105.222:9090/index.php/s/gf8smrrPcy3oHpN/preview)

# 3.Redis定时持久化

## 1.设计思路

### 1.1 数据库设计

```sql
# 浏览内容表
DROP TABLE IF EXISTS `view_item`;
CREATE TABLE `view_item`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '内容id（如文章、短视频等等）',
  `create_user` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `like_count` int(11) NULL DEFAULT NULL COMMENT '点赞数',
  `cmt_count` int(11) NULL DEFAULT NULL COMMENT '评论数',
  `share_count` int(11) NULL DEFAULT NULL COMMENT '分享数',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

# 点赞-用户表
DROP TABLE IF EXISTS `user_likes`;
CREATE TABLE `user_likes`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '点赞信息ID',
  `info_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '点赞对象id',
  `like_user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '点赞人ID',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '0 点赞 1 取消 ',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `agdkey`(`like_user_id`, `info_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '点赞记录表' ROW_FORMAT = Dynamic;
```

![image-20220928131306177](http://42.193.105.222:9090/index.php/s/KZ39kZYkQmSaRtg/preview)

![image-20220928131328753](http://42.193.105.222:9090/index.php/s/x8DEKNE29d8da2T/preview)

### 1.2 流程

![image-20220928125232420](http://42.193.105.222:9090/index.php/s/BfFMQxGXDks6Zco/preview)

1. 遍历Redis的【点赞信息】，==**仅改变数据库中点赞信息的状态**==
2. 判断当前点赞信息是否在数据库中
   - 否，则更新数据
     - 数据库中新增点赞-用户记录
     - 更新内容的点赞量
     - 转到6
   - 是
     - 转到第3步
3. 判断数据库中的点赞状态与缓存中的点赞状态（status）
   - 一致
     - 状态不改变
     - 点赞数量-1（两种情况逻辑分析有差异，但是最终结果均为-1）
     - 结束
   - 不一致，则需要针对具体情况改变
     - 转到步骤4
4. 判断数据库点赞状态
   - 已经点赞，需要更改为取消点赞
     - 数据库中修改为取消点赞状态
     - 更新缓存中的点赞数量-1（减去数据库中持久化的一个点赞量，一会儿缓存会和数据库点赞总量加和）
   - 取消点赞，需要更改
     - 数据库中修改为点赞状态
     - 无需更新缓存中的点赞数量，因为缓存中已经+1（即该点赞数据的点赞量）
5. 将缓存【点赞数量】持久化并清理缓存 ==**此处修改数据库中的点赞数量**==
6. 完成缓存持久化

### 1.3定时写入

使用 Quartz redis 定时任务持久化存储到数据库

```xml
        <!-- Quartz redis定时任务持久化存储到数据库 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>
```

## 2.核心代码实现

### 2.1Bean

```java
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
```

```java
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
```

### 2.2 Service

#### 1.interface

```java
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csu.edu.redisLikeDemo.domain.UserLikes;

/**
 * 负责将Redis缓存中的数据持久化到数据库中
 */
public interface DBService {
    /**
     * 保存点赞记录
     * @param userLike
     * @return
     */
    Boolean save(UserLikes userLike);
    /**
     * 更新点赞记录
     * @param userLike
     * @return
     */
    Boolean update(UserLikes userLike);
    /**
     * 根据内容的id查询点赞列表（即查询都谁给这个内容点赞过）
     * @param infoId 内容的id
     * @return
     */
    Page<UserLikes> getLikedListByInfoId(String infoId, int pageNum, int pageSize);

    /**
     * 根据点赞人的id查询点赞列表（即查询这个人都给哪些内容点赞过）
     * @param likeUserId
     * @return
     */
    Page<UserLikes> getLikedListByLikeUserId(String likeUserId, int pageNum, int pageSize);

    /**
     * 通过被点赞内容和点赞人id查询是否存在点赞记录
     * @param infoId
     * @param likeUserId
     * @return
     */
    UserLikes getByInfoIdAndLikeUserId(String infoId, String likeUserId);

    /**
     * 将Redis里的点赞数据存入数据库中,True 表示还需要进一步持久化， False表示数据库中已存在该数据，无需进一步持久化
     */
    void transLikedFromRedis2DB();

    /**
     * 将Redis中的点赞数量数据存入数据库
     */
    void transLikedCountFromRedis2DB();
}
```

#### 2.implement

```java
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
    private Sid sid;// Id生成器，利用idWorker产生唯一（不重复）自增式的id，可以根据需求选用其他方式，比如MyBatisPlus的自增

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
```

## 3. 定时更新数据库

### 3.1 定时任务

```java
import com.csu.edu.redisLikeDemo.service.DBService; 
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 定时任务
 */
@Slf4j
public class CronUtil extends QuartzJobBean {
    @Autowired
    private DBService dbService;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 执行的定时任务
     */
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("LikeTask-------- {}", sdf.format(new Date()));

        //将 Redis 里的点赞信息同步到数据库里
        dbService.transLikedFromRedis2DB();
        dbService.transLikedCountFromRedis2DB();
    }
}
```

### 3.2 定时任务配置

设置每两个小时更新一次数据库

```java
import com.csu.edu.redisLikeDemo.util.CronUtil;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开启定时任务持久化存储到数据库
 */
@Configuration
public class QuartzConfig {
    private static final String LIKE_TASK_IDENTITY = "LikeTaskQuartz";
    @Bean
    public JobDetail quartzDetail(){
        return JobBuilder.newJob(CronUtil.class).withIdentity(LIKE_TASK_IDENTITY).storeDurably().build();
    }

    @Bean
    public Trigger quartzTrigger(){
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
//                .withIntervalInSeconds(20)  //设置时间周期单位秒
                .withIntervalInHours(2)  //两个小时执行一次
                .repeatForever();
        return TriggerBuilder.newTrigger().forJob(quartzDetail())
                .withIdentity(LIKE_TASK_IDENTITY)
                .withSchedule(scheduleBuilder)
                .build();
    }
}
```

# 4.项目源码地址 & 参考

>项目源码：https://github.com/WuYiheng-Og/redislike
>
>点赞任务参考：[[Redis 是如何实现点赞、取消点赞的？ - 腾讯云开发者社区-腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1593444)](https://cloud.tencent.com/developer/article/1445905)
>
>redis封装: https://www.cnblogs.com/caizhaokai/p/11037610.html
