"""轻工具箱 photo-worker：证件照与图片处理。

优先尝试 rembg 抠图；未安装时使用简化处理（缩放+铺底），保证本地可跑通。
"""

from __future__ import annotations

import hashlib
import io
import math
import os
import uuid
from collections import OrderedDict
from pathlib import Path
from typing import Optional

import numpy as np
from fastapi import FastAPI, File, Form, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image, ImageOps

app = FastAPI(title="light-toolbox-photo-worker")
UPLOAD_DIR = Path(os.getenv("UPLOAD_DIR", "./uploads")).resolve()
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

_rembg_session = None
# 同一张原图只抠一次。预览和正式生成共用，换底色不必重跑模型。
_cutout_cache: OrderedDict[str, tuple[Image.Image, bool]] = OrderedDict()


def rembg_available() -> bool:
    try:
        import rembg  # noqa: F401

        return True
    except Exception:
        return False


def _try_rembg(img: Image.Image) -> tuple[Image.Image, bool]:
    """返回 (图像, 是否使用了 rembg)。未安装时降级为整图 RGBA。"""
    global _rembg_session
    try:
        from rembg import remove, new_session  # type: ignore

        if _rembg_session is None:
            portrait = Path.home() / ".u2net" / "birefnet-portrait.onnx"
            # 完整肖像模型约 973MB。没下完时用已缓存的 u2net，避免请求一直等下载。
            model = "birefnet-portrait" if portrait.exists() and portrait.stat().st_size > 900_000_000 else "u2net"
            _rembg_session = new_session(model)
        out = remove(img, session=_rembg_session)
        if isinstance(out, bytes):
            return Image.open(io.BytesIO(out)).convert("RGBA"), True
        return out.convert("RGBA"), True
    except Exception:
        return img.convert("RGBA"), False


def _parse_color(color: str) -> tuple[int, int, int]:
    c = (color or "#FFFFFF").strip()
    if c.startswith("#") and len(c) == 7:
        return int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16)
    named = {
        "white": (255, 255, 255),
        "blue": (67, 142, 219),
        "red": (255, 0, 0),
        "#ffffff": (255, 255, 255),
        "#438edb": (67, 142, 219),
        "#ff0000": (255, 0, 0),
    }
    return named.get(c.lower(), (255, 255, 255))


def _cutout(raw: bytes, img: Image.Image) -> tuple[Image.Image, bool]:
    digest = hashlib.sha256(raw).hexdigest()
    cached = _cutout_cache.get(digest)
    if cached is not None:
        _cutout_cache.move_to_end(digest)
        return cached[0].copy(), cached[1]
    cut, used = _try_rembg(img)
    _cutout_cache[digest] = (cut, used)
    while len(_cutout_cache) > 8:
        _cutout_cache.popitem(last=False)
    return cut.copy(), used


def _fit_canvas(
    fg: Image.Image,
    width: int,
    height: int,
    bg: tuple[int, int, int],
    transparent: bool = False,
) -> Image.Image:
    if transparent:
        canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    else:
        canvas = Image.new("RGB", (width, height), bg)
    fg = fg.convert("RGBA")
    # 按短边适配并居中，头肩留在画面里
    ratio = min(width / fg.width, height / fg.height)
    nw, nh = max(1, int(fg.width * ratio)), max(1, int(fg.height * ratio))
    fg = fg.resize((nw, nh), Image.Resampling.LANCZOS)
    x, y = (width - nw) // 2, (height - nh) // 2
    canvas.paste(fg, (x, y), fg)
    return canvas


def _save(img: Image.Image, suffix: str = ".jpg", quality: int = 92) -> tuple[Path, str, int]:
    name = f"{uuid.uuid4().hex}{suffix}"
    path = UPLOAD_DIR / name
    if suffix.lower() in {".jpg", ".jpeg"}:
        img.convert("RGB").save(path, format="JPEG", quality=quality, optimize=True)
    elif suffix.lower() == ".png":
        img.save(path, format="PNG", optimize=True)
    elif suffix.lower() == ".webp":
        img.save(path, format="WEBP", quality=quality)
    else:
        img.convert("RGB").save(path, format="JPEG", quality=quality)
    return path, name, path.stat().st_size


def _load(upload: UploadFile) -> tuple[bytes, Image.Image]:
    data = upload.file.read()
    img = ImageOps.exif_transpose(Image.open(io.BytesIO(data)))
    return data, img


def _detect_face(img: Image.Image) -> Optional[tuple[int, int, int, int]]:
    """返回原图像素坐标 (x, y, w, h)。当前环境的 cv2 没有级联分类器时直接放弃，改用人物区域。"""
    try:
        import cv2

        if not hasattr(cv2, "CascadeClassifier"):
            return None
        rgb = np.array(img.convert("RGB"))
        height, width = rgb.shape[:2]
        scale = 1.0
        view = rgb
        if max(height, width) > 1280:
            scale = 1280 / max(height, width)
            view = cv2.resize(
                rgb,
                (max(1, int(width * scale)), max(1, int(height * scale))),
                interpolation=cv2.INTER_AREA,
            )
        gray = cv2.cvtColor(view, cv2.COLOR_RGB2GRAY)
        cascade_dir = getattr(getattr(cv2, "data", None), "haarcascades", "")
        cascade = cv2.CascadeClassifier(cascade_dir + "haarcascade_frontalface_default.xml")
        if cascade.empty():
            return None
        faces = cascade.detectMultiScale(gray, scaleFactor=1.08, minNeighbors=5, minSize=(48, 48))
        if faces is None or len(faces) == 0:
            return None
        x, y, fw, fh = max(faces, key=lambda f: int(f[2]) * int(f[3]))
        inv = 1.0 / scale
        return int(x * inv), int(y * inv), int(fw * inv), int(fh * inv)
    except Exception as e:
        print(f"face detect skipped: {e}")
        return None


def _face_from_subject(cut: Image.Image) -> Optional[tuple[int, int, int, int]]:
    """没检测到脸时，用抠图区域的上半部分估算头部，避免再把脸撑满。"""
    alpha = np.array(cut.convert("RGBA"))[:, :, 3]
    ys, xs = np.where(alpha > 24)
    if len(xs) == 0:
        return None
    x0, x1 = int(xs.min()), int(xs.max())
    y0, y1 = int(ys.min()), int(ys.max())
    body_w, body_h = max(1, x1 - x0), max(1, y1 - y0)
    fw = max(1, int(body_w * 0.62))
    fh = max(1, int(body_h * 0.34))
    fx = x0 + (body_w - fw) // 2
    fy = y0 + int(body_h * 0.04)
    return fx, fy, fw, fh


def _shift_headroom(img: Image.Image, min_ratio: float = 0.10, max_ratio: float = 0.12) -> Image.Image:
    """头顶到照片上沿留出约 10%–12%，和 HivisionIDPhotos 的 head_top_range 一致。"""
    alpha = np.array(img)[:, :, 3]
    rows = np.where(alpha.max(axis=1) > 16)[0]
    if len(rows) == 0:
        return img
    top = int(rows[0])
    gap = top / img.height
    if min_ratio <= gap <= max_ratio:
        return img
    target = int(img.height * (min_ratio + max_ratio) / 2)
    shift = target - top
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, shift))
    return out


def _crop_idphoto(cut: Image.Image, face: tuple[int, int, int, int], out_w: int, out_h: int) -> Image.Image:
    """按 HivisionIDPhotos：人脸面积约占成片 1/5，面部中心在高度 45% 处。"""
    x, y, fw, fh = face
    face_cx = x + fw / 2
    face_cy = y + fh / 2
    crop_measure = (fw * fh) / 0.2
    scale = math.sqrt(crop_measure / max(1, out_w * out_h))
    crop_w = max(1, int(round(out_w * scale)))
    crop_h = max(1, int(round(out_h * scale)))
    x1 = int(face_cx - crop_w / 2)
    y1 = int(face_cy - crop_h * 0.45)
    sheet = Image.new("RGBA", (crop_w, crop_h), (0, 0, 0, 0))
    src_x1, src_y1 = max(0, x1), max(0, y1)
    src_x2, src_y2 = min(cut.width, x1 + crop_w), min(cut.height, y1 + crop_h)
    if src_x2 > src_x1 and src_y2 > src_y1:
        patch = cut.crop((src_x1, src_y1, src_x2, src_y2))
        sheet.paste(patch, (src_x1 - x1, src_y1 - y1), patch)
    sheet = _shift_headroom(sheet)
    return sheet.resize((out_w, out_h), Image.Resampling.LANCZOS)


def _compose_idphoto(
    cut: Image.Image,
    source: Image.Image,
    width: int,
    height: int,
    bg: tuple[int, int, int],
    transparent: bool,
) -> Image.Image:
    face = _detect_face(source) or _face_from_subject(cut)
    if face and source.size != cut.size and source.width and source.height:
        sx = cut.width / source.width
        sy = cut.height / source.height
        face = (
            int(face[0] * sx),
            int(face[1] * sy),
            max(1, int(face[2] * sx)),
            max(1, int(face[3] * sy)),
        )
    # 预览用两倍分辨率，手机上看才不会发虚；正式保存再缩回规格像素。
    sharp_w, sharp_h = width * 2, height * 2
    if face is None:
        laid = _fit_canvas(cut, sharp_w, sharp_h, bg, transparent=True)
    else:
        laid = _crop_idphoto(cut, face, sharp_w, sharp_h)
    if transparent:
        return laid
    small = laid.resize((width, height), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (width, height), bg)
    canvas.paste(small, (0, 0), small)
    return canvas


@app.get("/health")
def health():
    return {"ok": True, "rembg": rembg_available()}


def _compress_image(img: Image.Image, max_dimension, output_format, scale) -> tuple[Image.Image, str]:
    try:
        max_dim = int(max_dimension if max_dimension not in (None, "") else "1920")
    except ValueError:
        max_dim = 1920
    try:
        sc = float(scale or "1")
    except ValueError:
        sc = 1.0
    if sc <= 0 or sc > 1:
        sc = 1.0
    fmt = (output_format or "jpeg").lower()
    src = (img.format or "JPEG").upper()
    if fmt in {"original", "keep"}:
        fmt = "png" if src == "PNG" else "webp" if src == "WEBP" else "jpeg"
    if fmt == "jpg":
        fmt = "jpeg"
    if fmt not in {"jpeg", "png", "webp"}:
        fmt = "jpeg"
    work = img
    if 0 < sc < 0.999:
        work = work.resize(
            (max(1, int(work.width * sc)), max(1, int(work.height * sc))),
            Image.Resampling.LANCZOS,
        )
    if max_dim > 0 and max(work.size) > max_dim:
        ratio = max_dim / max(work.size)
        work = work.resize(
            (max(1, int(work.width * ratio)), max(1, int(work.height * ratio))),
            Image.Resampling.LANCZOS,
        )
    suffix = {"jpeg": ".jpg", "png": ".png", "webp": ".webp"}[fmt]
    if fmt == "jpeg":
        work = work.convert("RGB")
    elif work.mode not in ("RGB", "RGBA", "L"):
        work = work.convert("RGBA")
    return work, suffix


@app.post("/process")
async def process(
    action: str = Form(...),
    file: UploadFile = File(...),
    output_path: Optional[str] = Form(None),
    estimate_only: Optional[str] = Form(None),
    width: Optional[str] = Form(None),
    height: Optional[str] = Form(None),
    bgColor: Optional[str] = Form("#FFFFFF"),
    quality: Optional[str] = Form("80"),
    maxDimension: Optional[str] = Form("1920"),
    format: Optional[str] = Form("jpeg"),
    outputFormat: Optional[str] = Form("jpeg"),
    scale: Optional[str] = Form("1"),
    cropX: Optional[str] = Form(None),
    cropY: Optional[str] = Form(None),
    cropW: Optional[str] = Form(None),
    cropH: Optional[str] = Form(None),
    transparent: Optional[str] = Form(None),
):
    try:
        raw, img = _load(file)
        q = int(quality or "80")
        used_rembg = False
        path = name = None
        size = 0
        if action == "idphoto":
            cut, used_rembg = _cutout(raw, img)
            w = int(width or "295") or 295
            h = int(height or "413") or 413
            if w <= 0 or h <= 0:
                w, h = 295, 413
            matte = transparent == "1"
            out = _compose_idphoto(cut, img, w, h, _parse_color(bgColor or "#FFFFFF"), transparent=matte)
            path, name, size = _save(out, ".png" if matte else ".jpg", 95)
        elif action == "image_compress":
            work, suffix = _compress_image(img, maxDimension, outputFormat, scale)
            buf = io.BytesIO()
            if suffix == ".png":
                work.save(buf, format="PNG", optimize=True)
            elif suffix == ".webp":
                work.save(buf, format="WEBP", quality=q)
            else:
                work.convert("RGB").save(buf, format="JPEG", quality=q, optimize=True)
            estimated = buf.tell()
            if estimate_only == "1":
                return {"ok": True, "estimated_size": estimated, "output_size": estimated}
            path, name, size = _save(work, suffix, q)
        elif action == "image_resize":
            w = int(width or img.width)
            h = int(height or img.height)
            out = img.resize((max(1, w), max(1, h)), Image.Resampling.LANCZOS)
            path, name, size = _save(out.convert("RGB"), ".jpg", 92)
        elif action == "image_crop":
            x = int(cropX or 0)
            y = int(cropY or 0)
            w = int(cropW or img.width)
            h = int(cropH or img.height)
            out = img.crop((x, y, min(img.width, x + w), min(img.height, y + h)))
            path, name, size = _save(out.convert("RGB"), ".jpg", 92)
        elif action == "image_format":
            fmt = (format or "jpeg").lower()
            suffix = {"jpeg": ".jpg", "jpg": ".jpg", "png": ".png", "webp": ".webp"}.get(fmt, ".jpg")
            path, name, size = _save(img, suffix, q)
        elif action == "image_bg":
            cut, used_rembg = _cutout(raw, img)
            if transparent == "1":
                side = max(cut.size)
                if side > 1200:
                    ratio = 1200 / side
                    cut = cut.resize(
                        (max(1, int(cut.width * ratio)), max(1, int(cut.height * ratio))),
                        Image.Resampling.LANCZOS,
                    )
                path, name, size = _save(cut, ".png", 92)
            else:
                out = _fit_canvas(cut, cut.width, cut.height, _parse_color(bgColor or "#FFFFFF"))
                path, name, size = _save(out, ".jpg", 92)
        else:
            return JSONResponse({"ok": False, "message": f"未知 action: {action}"})

        if output_path:
            hint = Path(output_path)
            if not hint.suffix:
                hint = hint.with_suffix(path.suffix or ".jpg")
            hint.parent.mkdir(parents=True, exist_ok=True)
            Image.open(path).save(hint)
            path, name, size = hint, hint.name, hint.stat().st_size

        payload = {
            "ok": True,
            "output_path": str(path),
            "output_name": name,
            "output_size": size,
            "estimated_size": size,
            "rembg": used_rembg if action in {"idphoto", "image_bg"} else None,
        }
        if action in {"idphoto", "image_bg"} and not used_rembg:
            payload["warning"] = "未安装 rembg，已使用降级铺底（pip install rembg onnxruntime）"
        return payload
    except Exception as e:
        return JSONResponse({"ok": False, "message": str(e)})
