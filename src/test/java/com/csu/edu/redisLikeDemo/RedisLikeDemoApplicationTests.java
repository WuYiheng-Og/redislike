package com.csu.edu.redisLikeDemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;

import java.util.HashMap;

@SpringBootTest
class RedisLikeDemoApplicationTests {

    @Autowired
    private HashOperations<String, String, Object> redisHash;// Redis Hash
    @Test
    void contextLoads() {
        HashMap<String,Object> map = new HashMap<>();
        long timeStamp = System.currentTimeMillis();
        map.put("1",1);
        map.put("2",timeStamp);
        redisHash.put("testKey","testKeyId",map);
        map = null;
        map = (HashMap<String, Object>) redisHash.get("testKey","testKeyId");
    }

}
