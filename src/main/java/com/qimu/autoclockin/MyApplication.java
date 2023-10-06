package com.qimu.autoclockin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:50:10
 * @Version: 1.0
 * @Description: 我申请
 */
@SpringBootApplication
@EnableRedisRepositories
@EnableScheduling
@MapperScan("com.qimu.autoclockin.mapper")
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }

}
