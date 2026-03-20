package com.ghlzm.iot.framework.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * 统一注册 Jackson 模块，保持 HTTP 响应与业务注入使用一致的 JSON 序列化行为。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JacksonModule longToStringJacksonModule() {
        SimpleModule module = new SimpleModule();
        // Long 类型统一序列化为字符串，避免前端 JS number 精度丢失导致主键错位
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return module;
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(JacksonModule longToStringJacksonModule) {
        return builder -> builder.addModule(longToStringJacksonModule);
    }
}
