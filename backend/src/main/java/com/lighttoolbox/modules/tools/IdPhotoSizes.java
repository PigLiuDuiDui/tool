package com.lighttoolbox.modules.tools;

import java.util.List;
import java.util.Map;

/**
 * 证件照规格。
 */
public final class IdPhotoSizes {

    private IdPhotoSizes() {
    }

    public static List<Map<String, Object>> all() {
        return List.of(
                size("1inch", "一寸", 295, 413, "25×35mm"),
                size("2inch", "二寸", 413, 579, "35×49mm"),
                size("exam", "考试报名", 480, 640, "常见网报"),
                size("passport", "护照", 390, 567, "33×48mm"),
                size("custom", "自定义", 0, 0, "自定义像素")
        );
    }

    public static Map<String, Object> get(String code) {
        return all().stream().filter(s -> code.equals(s.get("code"))).findFirst().orElse(size("1inch", "一寸", 295, 413, "25×35mm"));
    }

    private static Map<String, Object> size(String code, String name, int w, int h, String tip) {
        return Map.of("code", code, "name", name, "width", w, "height", h, "tip", tip);
    }
}
