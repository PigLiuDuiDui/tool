package com.lighttoolbox.modules.job;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lighttoolbox.common.BusinessException;
import com.lighttoolbox.common.ErrorCode;
import com.lighttoolbox.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地临时文件存储。
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AppProperties appProperties;

    public Path root() {
        Path root = Path.of(appProperties.getFile().getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL, "无法创建上传目录");
        }
        return root;
    }

    public StoredFile saveUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传文件");
        }
        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
        String safeName = original.replaceAll("[\\\\/:*?\"<>|]", "_");
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir = root().resolve(day);
        try {
            Files.createDirectories(dir);
            String stored = IdWorker.getIdStr() + "_" + safeName;
            Path target = dir.resolve(stored);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(target.toString(), safeName, Files.size(target));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL, "保存上传文件失败");
        }
    }

    public Path resolve(String absoluteOrRelative) {
        Path p = Path.of(absoluteOrRelative);
        if (!p.isAbsolute()) {
            p = root().resolve(absoluteOrRelative);
        }
        return p.normalize();
    }

    public record StoredFile(String path, String name, long size) {
    }
}
