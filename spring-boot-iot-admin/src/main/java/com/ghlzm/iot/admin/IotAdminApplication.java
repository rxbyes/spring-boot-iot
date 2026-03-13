package com.ghlzm.iot.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ghlzm.iot.**.mapper")
@SpringBootApplication
public class IotAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotAdminApplication.class, args);
    }

}
