package com.lighttoolbox.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录主体：微信用户。
 */
@Data
@AllArgsConstructor
public class WxPrincipal {
    private Long userId;
    private String openId;
}
