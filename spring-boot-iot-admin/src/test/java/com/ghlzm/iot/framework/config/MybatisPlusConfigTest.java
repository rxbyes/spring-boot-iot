package com.ghlzm.iot.framework.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MybatisPlusConfigTest {

    @Test
    void mybatisConfigurationCustomizerShouldUseScopedSlf4jLogger() {
        MybatisPlusConfig config = new MybatisPlusConfig();
        MybatisConfiguration configuration = new MybatisConfiguration();

        config.mybatisConfigurationCustomizer().customize(configuration);

        assertEquals(MybatisScopedSlf4jImpl.class, configuration.getLogImpl());
        assertEquals(
                MybatisLoggingConstants.SQL_LOG_PREFIX + "com.ghlzm.iot.system.mapper.UserMapper.selectList",
                MybatisScopedSlf4jImpl.toLoggerName("com.ghlzm.iot.system.mapper.UserMapper.selectList")
        );
    }
}
