package com.lighttoolbox.modules.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lighttoolbox.common.Result;
import com.lighttoolbox.modules.job.entity.UserRecentTool;
import com.lighttoolbox.modules.job.mapper.UserRecentToolMapper;
import com.lighttoolbox.security.SecurityUtils;
import com.lighttoolbox.security.WxPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工具目录与最近使用。
 */
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class ToolController {

    private final UserRecentToolMapper recentToolMapper;

    @GetMapping("/catalog")
    public Result<Map<String, Object>> catalog(@RequestParam(required = false) String keyword) {
        List<Map<String, Object>> categories = ToolCatalog.categories();
        List<Map<String, Object>> flat = ToolCatalog.flatTools();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase(Locale.ROOT);
            flat = flat.stream()
                    .filter(t -> String.valueOf(t.get("name")).toLowerCase(Locale.ROOT).contains(kw)
                            || String.valueOf(t.get("desc")).toLowerCase(Locale.ROOT).contains(kw)
                            || String.valueOf(t.get("code")).toLowerCase(Locale.ROOT).contains(kw))
                    .collect(Collectors.toList());
        }
        return Result.ok(Map.of(
                "categories", categories,
                "tools", flat,
                "idSizes", IdPhotoSizes.all()
        ));
    }

    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent() {
        WxPrincipal user = SecurityUtils.requireUser();
        List<UserRecentTool> rows = recentToolMapper.selectList(new LambdaQueryWrapper<UserRecentTool>()
                .eq(UserRecentTool::getUserId, user.getUserId())
                .orderByDesc(UserRecentTool::getUsedAt)
                .last("LIMIT 12"));
        List<Map<String, Object>> result = rows.stream()
                .map(r -> ToolCatalog.find(r.getToolCode()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Result.ok(result);
    }
}
