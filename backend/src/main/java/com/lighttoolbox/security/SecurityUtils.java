package com.lighttoolbox.security;

import com.lighttoolbox.common.BusinessException;
import com.lighttoolbox.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static WxPrincipal requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof WxPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal;
    }
}
