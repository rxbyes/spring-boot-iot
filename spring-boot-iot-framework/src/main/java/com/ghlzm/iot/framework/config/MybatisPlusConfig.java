package com.ghlzm.iot.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public SlowSqlLoggingInterceptor slowSqlLoggingInterceptor(IotProperties iotProperties) {
        return new SlowSqlLoggingInterceptor(iotProperties);
    }

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return this::customizeMybatisConfiguration;
    }

    void customizeMybatisConfiguration(MybatisConfiguration configuration) {
        configuration.setLogImpl(MybatisScopedSlf4jImpl.class);
    }
}
