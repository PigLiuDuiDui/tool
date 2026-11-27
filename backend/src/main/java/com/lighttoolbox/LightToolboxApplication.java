package com.lighttoolbox;

import com.lighttoolbox.config.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 轻工具箱后端入口。
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
@MapperScan(basePackages = "com.lighttoolbox.modules", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class LightToolboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(LightToolboxApplication.class, args);
    }
}
