package com.ghlzm.iot.admin;

import com.ghlzm.iot.framework.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongIdJsonSerializationTests {

    @Test
    void shouldSerializeLongIdAsStringInHttpResponses() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JacksonConfig.class)) {
            JsonMapperBuilderCustomizer customizer = context.getBean(JsonMapperBuilderCustomizer.class);
            JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules();
            customizer.customize(builder);
            ObjectMapper objectMapper = builder.build();
            Map<String, Object> payload = Map.of("id", 2033720817103306754L);

            assertEquals(
                    "{\"id\":\"2033720817103306754\"}",
                    objectMapper.writeValueAsString(payload)
            );
        }
    }
}
