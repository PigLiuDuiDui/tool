package com.lighttoolbox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 静态映射处理后的临时文件（仍需 JWT 接口下载优先）。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(appProperties.getFile().getUploadDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/static-files/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
