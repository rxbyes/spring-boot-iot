package com.ghlzm.iot.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.framework.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongIdJsonSerializationTests {

    @Test
    void shouldSerializeLongIdAsStringInHttpResponses() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JacksonConfig.class)) {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            Map<String, Object> payload = Map.of("id", 2033720817103306754L);

            assertEquals(
                    "{\"id\":\"2033720817103306754\"}",
                    objectMapper.writeValueAsString(payload)
            );
        }
    }
}
