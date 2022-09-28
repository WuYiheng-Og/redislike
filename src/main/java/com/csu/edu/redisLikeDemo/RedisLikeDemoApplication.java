package com.csu.edu.redisLikeDemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.csu.edu.redisLikeDemo.mapper")
//@EnableCaching
public class RedisLikeDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisLikeDemoApplication.class, args);
    }

}
