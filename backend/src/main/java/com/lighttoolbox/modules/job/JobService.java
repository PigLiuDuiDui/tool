package com.lighttoolbox.modules.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lighttoolbox.common.BusinessException;
import com.lighttoolbox.common.ErrorCode;
import com.lighttoolbox.config.AppProperties;
import com.lighttoolbox.modules.job.entity.ToolJob;
import com.lighttoolbox.modules.job.entity.UserRecentTool;
import com.lighttoolbox.modules.job.mapper.ToolJobMapper;
import com.lighttoolbox.modules.job.mapper.UserRecentToolMapper;
import com.lighttoolbox.modules.tools.IdPhotoSizes;
import com.lighttoolbox.modules.tools.ToolCatalog;
import com.lighttoolbox.security.WxPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务创建、预估、查询与下载。
 */
@Service
@RequiredArgsConstructor
public class JobService {

    private static final Set<String> PHOTO_ACTIONS = Set.of(
            "idphoto", "image_compress", "image_crop", "image_resize", "image_format", "image_bg"
    );
    private static final Set<String> DOC_ACTIONS = Set.of(
            "word_to_pdf", "image_to_pdf", "pdf_to_image", "pdf_compress", "word_compress", "pdf_merge", "pdf_split"
    );

    private final ToolJobMapper toolJobMapper;
    private final UserRecentToolMapper recentToolMapper;
    private final FileStorageService fileStorageService;
    private final QuotaService quotaService;
    private final WorkerClient workerClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public Map<String, Object> estimate(WxPrincipal user, String toolCode, MultipartFile file, Map<String, String> params) {
        assertTool(toolCode);
        FileStorageService.StoredFile stored = fileStorageService.saveUpload(file);
        String action = mapAction(toolCode);
        if (!"pdf_compress".equals(action) && !"word_compress".equals(action) && !"image_compress".equals(action)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该工具无需预估");
        }
        WorkerClient.WorkerResult result;
        if (PHOTO_ACTIONS.contains(action) || "image_compress".equals(action)) {
            // 图片压缩预估走 photo worker estimate 同 process 轻量模式
            Map<String, String> p = new HashMap<>(params == null ? Map.of() : params);
            p.put("estimate_only", "1");
            result = workerClient.callPhoto(action, Path.of(stored.path()), p, null);
        } else {
            result = workerClient.callDocEstimate(action, Path.of(stored.path()), params);
        }
        if (!result.ok()) {
            throw new BusinessException(ErrorCode.INTERNAL, result.message());
        }
        return Map.of(
                "inputSize", stored.size(),
                "estimatedSize", result.estimatedSize() > 0 ? result.estimatedSize() : result.outputSize(),
                "tempInputPath", stored.path()
        );
    }

    /**
     * 证件照预览：抠图后按规格输出透明底，不扣每日次数。底色由前端铺上去。
     */
    public Map<String, Object> preview(String toolCode, MultipartFile file, Map<String, String> params) {
        boolean idphoto = toolCode != null && toolCode.startsWith("idphoto");
        boolean imageBg = "image_bg".equals(toolCode);
        if (!idphoto && !imageBg) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "这个工具没有预览");
        }
        assertTool(toolCode);
        FileStorageService.StoredFile stored = fileStorageService.saveUpload(file);
        Map<String, String> workerParams = idphoto ? buildParams(toolCode, params) : new HashMap<>(params == null ? Map.of() : params);
        workerParams.put("transparent", "1");
        WorkerClient.WorkerResult result = workerClient.callPhoto(
                idphoto ? "idphoto" : "image_bg", Path.of(stored.path()), workerParams, null);
        if (!result.ok() || result.outputPath() == null) {
            throw new BusinessException(ErrorCode.INTERNAL, result.message() == null ? "预览失败" : result.message());
        }
        try {
            byte[] bytes = Files.readAllBytes(Path.of(result.outputPath()));
            Map<String, Object> body = new HashMap<>();
            body.put("image", Base64.getEncoder().encodeToString(bytes));
            if (workerParams.get("width") != null) {
                body.put("width", Integer.parseInt(workerParams.get("width")));
                body.put("height", Integer.parseInt(workerParams.get("height")));
            }
            return body;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL, "预览失败");
        }
    }

    public ToolJob createAndRun(WxPrincipal user, String toolCode, MultipartFile file,
                                MultipartFile[] extraFiles, Map<String, String> params) {
        Map<String, Object> tool = assertTool(toolCode);
        if (Boolean.FALSE.equals(tool.get("enabled"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该工具即将开放");
        }
        quotaService.consume(user.getUserId());

        FileStorageService.StoredFile stored = fileStorageService.saveUpload(file);
        LocalDateTime now = LocalDateTime.now();
        ToolJob job = new ToolJob();
        job.setId(IdWorker.getId());
        job.setUserId(user.getUserId());
        job.setToolCode(toolCode);
        job.setStatus("PROCESSING");
        try {
            job.setParamsJson(objectMapper.writeValueAsString(params == null ? Map.of() : params));
        } catch (Exception e) {
            job.setParamsJson("{}");
        }
        job.setInputPath(stored.path());
        job.setInputName(stored.name());
        job.setInputSize(stored.size());
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setExpireAt(now.plusHours(appProperties.getFile().getRetainHours()));
        job.setDeleted(0);
        toolJobMapper.insert(job);

        touchRecent(user.getUserId(), toolCode);

        try {
            String action = mapAction(toolCode);
            Map<String, String> workerParams = buildParams(toolCode, params);
            StringBuilder extras = new StringBuilder();
            if (extraFiles != null) {
                for (MultipartFile extra : extraFiles) {
                    if (extra != null && !extra.isEmpty()) {
                        FileStorageService.StoredFile s = fileStorageService.saveUpload(extra);
                        if (!extras.isEmpty()) {
                            extras.append(',');
                        }
                        extras.append(s.path());
                    }
                }
            }
            // 小程序多文件：先 /jobs/stage 再通过 extraPaths 传入
            String staged = workerParams.remove("extraPaths");
            if (staged != null && !staged.isBlank()) {
                if (!extras.isEmpty()) {
                    extras.append(',');
                }
                extras.append(staged.trim());
            }
            if (!extras.isEmpty()) {
                workerParams.put("extra_files", extras.toString());
            }
            // Worker 用后缀识别格式，勿用无扩展名的 .out
            String outSuffix = guessOutputSuffix(toolCode, stored.name());
            Path outHint = Path.of(stored.path() + ".result" + outSuffix);
            WorkerClient.WorkerResult result;
            if (PHOTO_ACTIONS.contains(action)) {
                result = workerClient.callPhoto(action, Path.of(stored.path()), workerParams, outHint);
            } else if (DOC_ACTIONS.contains(action)) {
                result = workerClient.callDoc(action, Path.of(stored.path()), workerParams, outHint);
            } else {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "未知处理类型");
            }
            if (!result.ok()) {
                job.setStatus("FAILED");
                job.setErrorMessage(result.message());
            } else {
                job.setStatus("SUCCESS");
                job.setOutputPath(result.outputPath());
                job.setOutputName(result.outputName());
                job.setOutputSize(result.outputSize());
                job.setEstimatedSize(result.estimatedSize());
            }
        } catch (BusinessException ex) {
            job.setStatus("FAILED");
            job.setErrorMessage(ex.getMessage());
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setErrorMessage(ex.getMessage());
        }
        job.setUpdatedAt(LocalDateTime.now());
        toolJobMapper.updateById(job);
        return job;
    }

    public ToolJob getOwned(Long userId, Long jobId) {
        ToolJob job = toolJobMapper.selectById(jobId);
        if (job == null || !userId.equals(job.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (job.getExpireAt() != null && job.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件已过期");
        }
        return job;
    }

    public List<ToolJob> list(Long userId, int limit) {
        return toolJobMapper.selectList(new LambdaQueryWrapper<ToolJob>()
                .eq(ToolJob::getUserId, userId)
                .orderByDesc(ToolJob::getCreatedAt)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 50)));
    }

    public Resource download(Long userId, Long jobId) {
        ToolJob job = getOwned(userId, jobId);
        if (!"SUCCESS".equals(job.getStatus()) || job.getOutputPath() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结果不可下载");
        }
        Path path = fileStorageService.resolve(job.getOutputPath());
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "结果文件不存在");
        }
        return new FileSystemResource(path);
    }

    private Map<String, Object> assertTool(String toolCode) {
        Map<String, Object> tool = ToolCatalog.find(toolCode);
        if (tool == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未知工具");
        }
        return tool;
    }

    private String mapAction(String toolCode) {
        if (toolCode.startsWith("idphoto")) {
            return "idphoto";
        }
        return toolCode;
    }

    private Map<String, String> buildParams(String toolCode, Map<String, String> params) {
        Map<String, String> p = new HashMap<>(params == null ? Map.of() : params);
        if (toolCode.startsWith("idphoto")) {
            String sizeCode = switch (toolCode) {
                case "idphoto_1inch" -> "1inch";
                case "idphoto_2inch" -> "2inch";
                case "idphoto_exam" -> "exam";
                case "idphoto_passport" -> "passport";
                case "idphoto_custom" -> "custom";
                default -> p.getOrDefault("sizeCode", "1inch");
            };
            Map<String, Object> size = IdPhotoSizes.get(sizeCode);
            p.put("sizeCode", sizeCode);
            if ("custom".equals(sizeCode)) {
                int w = parsePositive(p.get("width"), 295);
                int h = parsePositive(p.get("height"), 413);
                p.put("width", String.valueOf(clamp(w, 50, 2000)));
                p.put("height", String.valueOf(clamp(h, 50, 2000)));
            } else {
                p.put("width", String.valueOf(size.get("width")));
                p.put("height", String.valueOf(size.get("height")));
            }
            p.putIfAbsent("bgColor", "#FFFFFF");
        }
        return p;
    }

    private String guessOutputSuffix(String toolCode, String inputName) {
        if (toolCode.startsWith("idphoto") || "image_compress".equals(toolCode) || "image_bg".equals(toolCode)
                || "image_crop".equals(toolCode) || "image_resize".equals(toolCode)) {
            return ".jpg";
        }
        if ("image_format".equals(toolCode)) {
            // 实际后缀由 worker 产出；此处仅作 hint
            return ".jpg";
        }
        if ("pdf_to_image".equals(toolCode)) {
            return ".png";
        }
        if (toolCode.contains("pdf") || "word_to_pdf".equals(toolCode) || "image_to_pdf".equals(toolCode)) {
            return ".pdf";
        }
        if ("word_compress".equals(toolCode)) {
            return ".docx";
        }
        String name = inputName == null ? "" : inputName.toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return name.substring(dot);
        }
        return ".bin";
    }

    public FileStorageService.StoredFile stage(MultipartFile file) {
        return fileStorageService.saveUpload(file);
    }

    private static int parsePositive(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private void touchRecent(Long userId, String toolCode) {
        UserRecentTool existing = recentToolMapper.selectOne(new LambdaQueryWrapper<UserRecentTool>()
                .eq(UserRecentTool::getUserId, userId)
                .eq(UserRecentTool::getToolCode, toolCode));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            UserRecentTool row = new UserRecentTool();
            row.setId(IdWorker.getId());
            row.setUserId(userId);
            row.setToolCode(toolCode);
            row.setUsedAt(now);
            row.setDeleted(0);
            recentToolMapper.insert(row);
        } else {
            existing.setUsedAt(now);
            recentToolMapper.updateById(existing);
        }
    }
}
