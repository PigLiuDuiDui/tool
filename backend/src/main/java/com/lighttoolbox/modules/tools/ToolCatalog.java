package com.lighttoolbox.modules.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具目录（一期/二期状态）。
 */
public final class ToolCatalog {

    private ToolCatalog() {
    }

    public static List<Map<String, Object>> categories() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(category("idphoto", "证件照", "purple", List.of(
                tool("idphoto_smart", "智能证件照", "多种规格，一键制作", true),
                tool("idphoto_1inch", "一寸照片", "25×35mm", true),
                tool("idphoto_2inch", "二寸照片", "35×49mm", true),
                tool("idphoto_exam", "考试报名照", "常见考试规格", true),
                tool("idphoto_passport", "护照照片", "国际护照规格", true),
                tool("idphoto_custom", "自定义尺寸", "自由设定像素", true)
        )));
        list.add(category("convert", "文件转换", "blue", List.of(
                tool("word_to_pdf", "Word转PDF", "DOCX 转 PDF", true),
                tool("pdf_to_word", "PDF转Word", "二期开放", false),
                tool("image_to_pdf", "图片转PDF", "多图合成 PDF", true),
                tool("pdf_to_image", "PDF转图片", "按页导出图片", true),
                tool("excel_to_pdf", "Excel转PDF", "二期开放", false),
                tool("ppt_to_pdf", "PPT转PDF", "二期开放", false)
        )));
        list.add(category("image", "图片处理", "teal", List.of(
                tool("image_compress", "图片压缩", "缩小体积", true),
                tool("image_crop", "图片裁剪", "自由裁剪", true),
                tool("image_resize", "图片尺寸调整", "按像素缩放", true),
                tool("image_format", "图片格式转换", "JPG/PNG/WEBP", true),
                tool("image_bg", "图片背景修改", "更换底色", true)
        )));
        list.add(category("doc", "文档处理", "orange", List.of(
                tool("pdf_compress", "PDF压缩", "压缩内嵌图片", true),
                tool("word_compress", "Word压缩", "压缩文档内图片", true),
                tool("pdf_merge", "PDF合并", "多文件合并", true),
                tool("pdf_split", "PDF拆分", "按页拆分", true)
        )));
        return list;
    }

    public static List<Map<String, Object>> flatTools() {
        List<Map<String, Object>> all = new ArrayList<>();
        for (Map<String, Object> cat : categories()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) cat.get("tools");
            for (Map<String, Object> t : tools) {
                Map<String, Object> copy = new LinkedHashMap<>(t);
                copy.put("category", cat.get("code"));
                copy.put("categoryName", cat.get("name"));
                copy.put("tone", cat.get("tone"));
                all.add(copy);
            }
        }
        return all;
    }

    public static Map<String, Object> find(String code) {
        return flatTools().stream()
                .filter(t -> code.equals(t.get("code")))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> category(String code, String name, String tone, List<Map<String, Object>> tools) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("tone", tone);
        m.put("tools", tools);
        return m;
    }

    private static Map<String, Object> tool(String code, String name, String desc, boolean enabled) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("desc", desc);
        m.put("enabled", enabled);
        return m;
    }
}
