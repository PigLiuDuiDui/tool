package com.lighttoolbox.modules.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lighttoolbox.config.AppProperties;
import com.lighttoolbox.modules.job.entity.ToolJob;
import com.lighttoolbox.modules.job.mapper.ToolJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 清理过期上传/结果文件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupJob {

    private final ToolJobMapper toolJobMapper;
    private final FileStorageService fileStorageService;
    private final AppProperties appProperties;

    @Scheduled(cron = "0 15 * * * ?")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        List<ToolJob> expired = toolJobMapper.selectList(new LambdaQueryWrapper<ToolJob>()
                .lt(ToolJob::getExpireAt, now)
                .last("LIMIT 200"));
        for (ToolJob job : expired) {
            deleteQuietly(job.getInputPath());
            deleteQuietly(job.getOutputPath());
        }
        log.info("过期文件清理完成，扫描 {} 条，retainHours={}", expired.size(), appProperties.getFile().getRetainHours());
    }

    private void deleteQuietly(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Path p = fileStorageService.resolve(path);
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.debug("删除失败 {}: {}", path, e.getMessage());
        }
    }
}
