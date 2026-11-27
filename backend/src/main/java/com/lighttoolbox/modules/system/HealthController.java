package com.lighttoolbox.modules.system;

import com.lighttoolbox.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查。
 */
@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP", "app", "light-toolbox"));
    }
}
