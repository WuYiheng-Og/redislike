package com.csu.edu.redisLikeDemo.util;

import com.csu.edu.redisLikeDemo.service.DBService;
import com.csu.edu.redisLikeDemo.service.LikeService;
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
     * 定时任务
     */
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("LikeTask-------- {}", sdf.format(new Date()));

        //将 Redis 里的点赞信息同步到数据库里
        dbService.transLikedFromRedis2DB();
        dbService.transLikedCountFromRedis2DB();
    }
}
