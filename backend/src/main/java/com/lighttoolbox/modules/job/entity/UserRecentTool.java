package com.lighttoolbox.modules.job.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_recent_tool")
public class UserRecentTool {
    @TableId
    private Long id;
    private Long userId;
    private String toolCode;
    private LocalDateTime usedAt;
    @TableLogic
    private Integer deleted;
}
