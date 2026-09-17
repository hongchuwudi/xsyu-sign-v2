package com.hongchu.qqrobotsign;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableAsync
@EnableScheduling
@MapperScan("com.hongchu.qqrobotsign.**.mapper")
@SpringBootApplication(scanBasePackages = "com.hongchu.qqrobotsign")
public class QqRobotSignApplication {
    public static void main(String[] args) {
        SpringApplication.run(QqRobotSignApplication.class, args);
        log.info("==============! SpringBoot 启动成功 !==============");
    }
}
