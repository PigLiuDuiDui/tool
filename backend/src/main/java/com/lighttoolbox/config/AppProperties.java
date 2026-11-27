package com.lighttoolbox.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用配置 app.*。
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Wechat wechat = new Wechat();
    private Photo photo = new Photo();
    private Doc doc = new Doc();
    private Quota quota = new Quota();
    private FileStore file = new FileStore();

    @Data
    public static class Jwt {
        private String secret;
        private long expireSeconds = 86400;
    }

    @Data
    public static class Security {
        private List<String> permitAll = new ArrayList<>();
    }

    @Data
    public static class Wechat {
        /** 为 true 时用 mockOpenId，无需真实 AppSecret */
        private boolean mock = true;
        private String appId = "";
        private String appSecret = "";
        private String mockOpenId = "dev-openid-light-toolbox";
    }

    @Data
    public static class Photo {
        private String workerUrl = "http://127.0.0.1:8091";
    }

    @Data
    public static class Doc {
        private String workerUrl = "http://127.0.0.1:8092";
    }

    @Data
    public static class Quota {
        private int dailyLimit = 10;
    }

    @Data
    public static class FileStore {
        private String uploadDir = "./uploads";
        private int retainHours = 24;
    }
}
