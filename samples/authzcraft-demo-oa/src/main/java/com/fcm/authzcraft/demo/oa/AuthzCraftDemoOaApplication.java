package com.fcm.authzcraft.demo.oa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fcm.authzcraft.demo.oa.infrastructure.persistence")
public class AuthzCraftDemoOaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthzCraftDemoOaApplication.class, args);
    }
}