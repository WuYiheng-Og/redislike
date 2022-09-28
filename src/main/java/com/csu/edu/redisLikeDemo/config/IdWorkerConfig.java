package com.csu.edu.redisLikeDemo.config;

import org.n3r.idworker.Sid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * IdWorker 配置
 * 产生唯一（不重复）自增式的id，多用于分布式
 * 非必须，可以根据选择采取不同的ID方式
 */
@Configuration
public class IdWorkerConfig {
    @Bean
    public Sid Sid(){
        return new Sid();
    }
}
