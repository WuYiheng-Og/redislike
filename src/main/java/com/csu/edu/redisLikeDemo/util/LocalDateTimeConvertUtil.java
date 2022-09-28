package com.csu.edu.redisLikeDemo.util;

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
