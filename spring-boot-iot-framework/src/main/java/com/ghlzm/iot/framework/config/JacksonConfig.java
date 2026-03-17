package com.ghlzm.iot.framework.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一注册 Jackson 模块，保持 HTTP 响应与业务注入使用一致的 JSON 序列化行为。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Module longToStringJacksonModule() {
        SimpleModule module = new SimpleModule();
        // Long 类型统一序列化为字符串，避免前端 JS number 精度丢失导致主键错位
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return module;
    }

    @Bean
    public ObjectMapper objectMapper(Module longToStringJacksonModule) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.registerModule(longToStringJacksonModule);
        return objectMapper;
    }
}
