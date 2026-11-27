package com.lighttoolbox.modules.job;

import com.lighttoolbox.common.Result;
import com.lighttoolbox.config.AppProperties;
import com.lighttoolbox.modules.job.entity.ToolJob;
import com.lighttoolbox.security.SecurityUtils;
import com.lighttoolbox.security.WxPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务 API。
 */
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final QuotaService quotaService;
    private final AppProperties appProperties;

    @PostMapping(value = "/estimate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> estimate(
            @RequestParam String toolCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> allParams
    ) {
        WxPrincipal user = SecurityUtils.requireUser();
        Map<String, String> params = strip(allParams);
        return Result.ok(jobService.estimate(user, toolCode, file, params));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> preview(
            @RequestParam String toolCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> allParams
    ) {
        SecurityUtils.requireUser();
        return Result.ok(jobService.preview(toolCode, file, strip(allParams)));
    }

    @PostMapping(value = "/stage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> stage(@RequestParam("file") MultipartFile file) {
        SecurityUtils.requireUser();
        var stored = jobService.stage(file);
        return Result.ok(Map.of(
                "path", stored.path(),
                "name", stored.name(),
                "size", stored.size()
        ));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> create(
            @RequestParam String toolCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "extraFiles", required = false) MultipartFile[] extraFiles,
            @RequestParam Map<String, String> allParams
    ) {
        WxPrincipal user = SecurityUtils.requireUser();
        Map<String, String> params = strip(allParams);
        ToolJob job = jobService.createAndRun(user, toolCode, file, extraFiles, params);
        return Result.ok(toView(job, user.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        WxPrincipal user = SecurityUtils.requireUser();
        return Result.ok(toView(jobService.getOwned(user.getUserId(), id), user.getUserId()));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(defaultValue = "20") int limit) {
        WxPrincipal user = SecurityUtils.requireUser();
        return Result.ok(jobService.list(user.getUserId(), limit).stream()
                .map(j -> toView(j, user.getUserId()))
                .toList());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        WxPrincipal user = SecurityUtils.requireUser();
        ToolJob job = jobService.getOwned(user.getUserId(), id);
        Resource resource = jobService.download(user.getUserId(), id);
        String name = job.getOutputName() == null ? "result.bin" : job.getOutputName();
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/quota")
    public Result<Map<String, Object>> quota() {
        WxPrincipal user = SecurityUtils.requireUser();
        int remaining = quotaService.remaining(user.getUserId());
        return Result.ok(Map.of(
                "dailyLimit", appProperties.getQuota().getDailyLimit(),
                "remaining", remaining
        ));
    }

    private Map<String, String> strip(Map<String, String> all) {
        Map<String, String> params = new HashMap<>(all);
        params.remove("toolCode");
        params.remove("file");
        params.remove("extraFiles");
        return params;
    }

    private Map<String, Object> toView(ToolJob job, Long userId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(job.getId()));
        m.put("toolCode", job.getToolCode());
        m.put("status", job.getStatus());
        m.put("inputName", job.getInputName());
        m.put("inputSize", job.getInputSize());
        m.put("outputName", job.getOutputName());
        m.put("outputSize", job.getOutputSize());
        m.put("estimatedSize", job.getEstimatedSize());
        m.put("errorMessage", job.getErrorMessage());
        m.put("expireAt", job.getExpireAt());
        m.put("createdAt", job.getCreatedAt());
        m.put("remaining", quotaService.remaining(userId));
        if ("SUCCESS".equals(job.getStatus())) {
            m.put("downloadUrl", "/api/jobs/" + job.getId() + "/download");
        }
        return m;
    }
}
