package com.ghlzm.iot.framework.config;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;

public class MybatisScopedSlf4jImpl extends Slf4jImpl {

    public MybatisScopedSlf4jImpl(String loggerName) {
        super(toLoggerName(loggerName));
    }

    static String toLoggerName(String loggerName) {
        return MybatisLoggingConstants.SQL_LOG_PREFIX + loggerName;
    }
}
