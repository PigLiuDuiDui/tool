package com.lighttoolbox.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务错误码：成功为 0。
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或令牌已失效"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "业务冲突"),
    QUOTA_EXCEEDED(429, "今日次数已用完"),
    INTERNAL(500, "服务器内部错误");

    private final int code;
    private final String message;
}
