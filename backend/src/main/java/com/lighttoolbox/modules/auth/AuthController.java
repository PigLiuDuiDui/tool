package com.lighttoolbox.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lighttoolbox.common.Result;
import com.lighttoolbox.config.AppProperties;
import com.lighttoolbox.modules.user.entity.WxUser;
import com.lighttoolbox.modules.user.mapper.WxUserMapper;
import com.lighttoolbox.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 微信登录（开发可 mock）。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppProperties appProperties;
    private final WxUserMapper wxUserMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@RequestBody Map<String, String> body) {
        String openId;
        if (appProperties.getWechat().isMock()) {
            openId = StringUtils.hasText(body.get("openId"))
                    ? body.get("openId")
                    : appProperties.getWechat().getMockOpenId();
        } else {
            // 真实环境应调用 jscode2session；此处留扩展点
            String code = body.get("code");
            if (!StringUtils.hasText(code)) {
                return Result.fail(400, "缺少 code");
            }
            openId = "wx-" + code;
            log.warn("微信真实登录未完整接入，临时使用 code 派生 openId");
        }

        WxUser user = wxUserMapper.selectOne(new LambdaQueryWrapper<WxUser>().eq(WxUser::getOpenId, openId));
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new WxUser();
            user.setId(IdWorker.getId());
            user.setOpenId(openId);
            user.setNickName(body.getOrDefault("nickName", "微信用户"));
            user.setAvatarUrl(body.get("avatarUrl"));
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user.setDeleted(0);
            wxUserMapper.insert(user);
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getOpenId());
        return Result.ok(Map.of(
                "accessToken", token,
                "user", Map.of(
                        "id", user.getId(),
                        "openId", user.getOpenId(),
                        "nickName", user.getNickName() == null ? "微信用户" : user.getNickName(),
                        "avatarUrl", user.getAvatarUrl() == null ? "" : user.getAvatarUrl()
                )
        ));
    }
}
