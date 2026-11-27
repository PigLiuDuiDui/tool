"""移植自桌面「文档图片压缩工具」：DOCX/PDF 压嵌套图 + 体积预估。"""

from __future__ import annotations

import io
import zipfile
from pathlib import Path
from typing import Callable

from PIL import Image
from pypdf import PdfReader, PdfWriter

MEDIA_PREFIX = "word/media/"
IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".tiff", ".tif"}
MAX_SAMPLES = 10


def _is_image(name: str) -> bool:
    return Path(name).suffix.lower() in IMAGE_EXTS


def _resize(img: Image.Image, scale: float, max_dimension: int) -> Image.Image:
    if 0 < scale < 0.999:
        img = img.resize(
            (max(1, int(img.width * scale)), max(1, int(img.height * scale))),
            Image.Resampling.LANCZOS,
        )
    if max_dimension > 0 and max(img.size) > max_dimension:
        ratio = max_dimension / max(img.size)
        img = img.resize(
            (max(1, int(img.width * ratio)), max(1, int(img.height * ratio))),
            Image.Resampling.LANCZOS,
        )
    return img


def compress_image_bytes(
    data: bytes,
    quality: int = 80,
    max_dimension: int = 1920,
    output_format: str = "jpeg",
    scale: float = 1.0,
) -> tuple[bytes, str]:
    img = Image.open(io.BytesIO(data))
    src = (img.format or "").upper()
    fmt = (output_format or "jpeg").lower()
    if fmt in {"keep", "original"}:
        if src == "PNG":
            fmt = "png"
        elif src == "WEBP":
            fmt = "webp"
        elif src == "GIF":
            fmt = "gif"
        else:
            fmt = "jpeg"
    if fmt == "gif":
        return data, ".gif"
    img = _resize(img, float(scale or 1), int(max_dimension or 0))
    buf = io.BytesIO()
    quality = max(40, min(95, int(quality or 80)))
    if fmt == "png":
        if img.mode not in ("RGB", "RGBA", "L"):
            img = img.convert("RGBA")
        img.save(buf, format="PNG", optimize=True)
        return buf.getvalue(), ".png"
    if fmt == "webp":
        if img.mode not in ("RGB", "RGBA"):
            img = img.convert("RGBA")
        img.save(buf, format="WEBP", quality=quality, method=4)
        return buf.getvalue(), ".webp"
    if img.mode != "RGB":
        img = img.convert("RGB")
    img.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue(), ".jpg"


def _patch_refs(blob: bytes, pairs: list[tuple[str, str]]) -> bytes:
    if not pairs:
        return blob
    text = blob.decode("utf-8", "replace")
    for old, new in pairs:
        text = text.replace(old, new)
        text = text.replace(old[len("word/"):], new[len("word/"):])
    if any(new.endswith(".jpg") for _, new in pairs):
        if 'Extension="jpg"' not in text and 'Extension="jpeg"' not in text and "</Types>" in text:
            text = text.replace(
                "</Types>",
                '<Default Extension="jpeg" ContentType="image/jpeg"/>'
                '<Default Extension="jpg" ContentType="image/jpeg"/></Types>',
            )
    if any(new.endswith(".png") for _, new in pairs):
        if 'Extension="png"' not in text and "</Types>" in text:
            text = text.replace(
                "</Types>",
                '<Default Extension="png" ContentType="image/png"/></Types>',
            )
    return text.encode("utf-8")


def compress_docx(
    input_path: Path,
    output_path: Path,
    quality: int = 80,
    max_dimension: int = 1920,
    output_format: str = "jpeg",
    scale: float = 1.0,
    progress: Callable | None = None,
) -> dict:
    progress and progress("extract")
    with zipfile.ZipFile(input_path, "r") as zin:
        names = zin.namelist()
        media_names = [n for n in names if n.startswith(MEDIA_PREFIX) and _is_image(n)]
        if not media_names:
            output_path.write_bytes(input_path.read_bytes())
            return {"imageCount": 0, "skipped": True}

        progress and progress("compress")
        mapping: dict[str, tuple[str, bytes]] = {}
        for name in media_names:
            raw = zin.read(name)
            try:
                data, ext = compress_image_bytes(raw, quality, max_dimension, output_format, scale)
                new_name = f"{MEDIA_PREFIX}{Path(name).stem}{ext}"
                mapping[name] = (new_name, data)
            except Exception:
                mapping[name] = (name, raw)

        renamed = [(old, new) for old, (new, _) in mapping.items() if old != new]

        progress and progress("write")
        with zipfile.ZipFile(output_path, "w", compression=zipfile.ZIP_DEFLATED) as zout:
            written = set()
            for name in names:
                if name.endswith("/"):
                    continue
                if name in mapping:
                    new_name, data = mapping[name]
                    if new_name in written:
                        continue
                    zout.writestr(new_name, data)
                    written.add(new_name)
                    continue
                blob = zin.read(name)
                if renamed and (name.endswith(".xml") or name.endswith(".rels")):
                    blob = _patch_refs(blob, renamed)
                zout.writestr(name, blob)
                written.add(name)

    return {"imageCount": len(media_names), "skipped": False}


def estimate_docx(
    input_path: Path,
    quality: int = 80,
    max_dimension: int = 1920,
    output_format: str = "jpeg",
    scale: float = 1.0,
) -> dict:
    original = input_path.stat().st_size
    with zipfile.ZipFile(input_path, "r") as zin:
        infos = zin.infolist()
        media = [
            e
            for e in infos
            if not e.is_dir() and e.filename.startswith(MEDIA_PREFIX) and _is_image(e.filename)
        ]
        if not media:
            return {"originalSize": original, "estimatedSize": original, "imageCount": 0, "sampled": 0}

        non_image = sum(
            e.file_size
            for e in infos
            if not e.is_dir() and not (e.filename.startswith(MEDIA_PREFIX) and _is_image(e.filename))
        )
        indices = _sample_indices(len(media), MAX_SAMPLES)
        sample_orig = 0
        sample_comp = 0
        for i in indices:
            e = media[i]
            raw = zin.read(e.filename)
            sample_orig += e.file_size or len(raw)
            try:
                data, _ = compress_image_bytes(raw, quality, max_dimension, output_format, scale)
                sample_comp += len(data)
            except Exception:
                sample_comp += e.file_size or len(raw)
        total_media = sum((e.file_size or 0) for e in media)
        ratio = (sample_comp / sample_orig) if sample_orig else 1
        estimated = int(non_image + total_media * ratio)
        return {
            "originalSize": original,
            "estimatedSize": max(estimated, 1),
            "imageCount": len(media),
            "sampled": len(indices),
        }


def compress_pdf(
    input_path: Path,
    output_path: Path,
    quality: int = 80,
    max_dimension: int = 1920,
    progress: Callable | None = None,
) -> dict:
    progress and progress("extract")
    reader = PdfReader(str(input_path))
    writer = PdfWriter()
    for page in reader.pages:
        try:
            page.compress_content_streams()
        except Exception:
            pass
        writer.add_page(page)
    try:
        writer.compress_identical_objects(remove_identicals=True, remove_orphans=True)
    except Exception:
        pass
    progress and progress("write")
    with open(output_path, "wb") as f:
        writer.write(f)
    return {"imageCount": len(reader.pages), "skipped": False}


def estimate_pdf(input_path: Path, quality: int = 80, max_dimension: int = 1920) -> dict:
    original = input_path.stat().st_size
    factor = max(0.35, min(0.95, quality / 100))
    return {
        "originalSize": original,
        "estimatedSize": int(original * factor),
        "imageCount": 0,
        "sampled": 0,
    }


def _sample_indices(total: int, max_samples: int) -> list[int]:
    if total <= 0:
        return []
    if total <= max_samples:
        return list(range(total))
    return sorted({int((i + 0.5) * total / max_samples) for i in range(max_samples)})
