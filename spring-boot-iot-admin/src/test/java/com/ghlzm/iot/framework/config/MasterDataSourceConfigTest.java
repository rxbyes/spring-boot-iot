package com.ghlzm.iot.framework.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MasterDataSourceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(MasterDataSourceConfig.class)
            .withPropertyValues(
                    "spring.datasource.dynamic.datasource.master.url=jdbc:h2:mem:master-config-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.dynamic.datasource.master.username=sa",
                    "spring.datasource.dynamic.datasource.master.password=",
                    "spring.datasource.dynamic.datasource.master.driver-class-name=org.h2.Driver",
                    "spring.datasource.dynamic.datasource.master.hikari.maximum-pool-size=30",
                    "spring.datasource.dynamic.datasource.master.hikari.minimum-idle=5"
            );

    @Test
    void masterDataSourceShouldBindNestedHikariSettings() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DataSource.class);
            assertThat(context.getBean(DataSource.class)).isInstanceOf(HikariDataSource.class);

            HikariDataSource dataSource = (HikariDataSource) context.getBean(DataSource.class);
            assertThat(dataSource.getMaximumPoolSize()).isEqualTo(30);
            assertThat(dataSource.getMinimumIdle()).isEqualTo(5);
        });
    }
}
