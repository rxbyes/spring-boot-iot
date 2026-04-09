package com.ghlzm.iot.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlJdbcUrlDefaultsTest {

    private static final Pattern MYSQL_URL_WITH_PUBLIC_KEY_RETRIEVAL = Pattern.compile(
            "IOT_MYSQL_URL:jdbc:mysql://[^\\r\\n]*allowPublicKeyRetrieval=true"
    );

    @Test
    void devProdAndTestProfilesShouldIncludeAllowPublicKeyRetrievalInDefaultMysqlUrl() throws IOException {
        assertMysqlDefaultUrlAllowsPublicKeyRetrieval("application-dev.yml");
        assertMysqlDefaultUrlAllowsPublicKeyRetrieval("application-prod.yml");
        assertMysqlDefaultUrlAllowsPublicKeyRetrieval("application-test.yml");
    }

    private void assertMysqlDefaultUrlAllowsPublicKeyRetrieval(String resourceName) throws IOException {
        String yaml = StreamUtils.copyToString(
                new ClassPathResource(resourceName).getInputStream(),
                StandardCharsets.UTF_8
        );

        assertThat(MYSQL_URL_WITH_PUBLIC_KEY_RETRIEVAL.matcher(yaml).find())
                .as("%s should keep allowPublicKeyRetrieval=true in the default IOT_MYSQL_URL", resourceName)
                .isTrue();
    }
}
