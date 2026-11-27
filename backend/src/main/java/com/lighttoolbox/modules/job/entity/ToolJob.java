package com.lighttoolbox.modules.job.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具处理任务。
 */
@Data
@TableName("tool_job")
public class ToolJob {
    @TableId
    private Long id;
    private Long userId;
    private String toolCode;
    private String status;
    private String paramsJson;
    private String inputPath;
    private String inputName;
    private Long inputSize;
    private String outputPath;
    private String outputName;
    private Long outputSize;
    private Long estimatedSize;
    private String errorMessage;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
