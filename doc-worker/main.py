"""轻工具箱 doc-worker：格式转换 + Word/PDF 压图压缩。"""

from __future__ import annotations

import os
import shutil
import subprocess
import tempfile
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Optional

from fastapi import FastAPI, File, Form, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image
from pypdf import PdfReader, PdfWriter
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

from compress_utils import compress_docx, compress_pdf, estimate_docx, estimate_pdf

app = FastAPI(title="light-toolbox-doc-worker")
UPLOAD_DIR = Path(os.getenv("UPLOAD_DIR", "./uploads")).resolve()
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
# 与 Gotenberg 一样：Office 转 PDF 走独立转换服务，不要求本机安装 LibreOffice。
GOTENBERG_URL = os.getenv("GOTENBERG_URL", "http://127.0.0.1:3000").rstrip("/")


def _save_upload(upload: UploadFile, suffix: str) -> Path:
    path = UPLOAD_DIR / f"{uuid.uuid4().hex}{suffix}"
    path.write_bytes(upload.file.read())
    return path


def _out_path(hint: Optional[str], suffix: str) -> Path:
    if hint:
        p = Path(hint)
        if not p.suffix:
            p = Path(str(p) + suffix)
        p.parent.mkdir(parents=True, exist_ok=True)
        return p
    return UPLOAD_DIR / f"{uuid.uuid4().hex}{suffix}"


def _compress_opts(quality, max_dimension, output_format, scale) -> tuple[int, int, str, float]:
    try:
        q = int(quality or "80")
    except ValueError:
        q = 80
    q = max(40, min(95, q))
    try:
        md = int(max_dimension if max_dimension not in (None, "") else "1920")
    except ValueError:
        md = 1920
    fmt = (output_format or "jpeg").lower()
    if fmt not in {"jpeg", "jpg", "png", "webp", "original", "keep"}:
        fmt = "jpeg"
    if fmt == "jpg":
        fmt = "jpeg"
    try:
        sc = float(scale or "1")
    except ValueError:
        sc = 1.0
    if sc <= 0 or sc > 1:
        sc = 1.0
    return q, md, fmt, sc


def _gotenberg_ready() -> bool:
    try:
        with urllib.request.urlopen(f"{GOTENBERG_URL}/health", timeout=2) as resp:
            return resp.status == 200
    except Exception:
        return False


def _convert_via_gotenberg(src: Path, out_dir: Path) -> Path:
    """POST /forms/libreoffice/convert，和 Gotenberg / Stirling-PDF 同一条转换链路。"""
    if not _gotenberg_ready():
        raise RuntimeError("转换服务还没准备好，过一会儿再试")
    boundary = uuid.uuid4().hex
    filename = src.name or "document.docx"
    head = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="files"; filename="{filename}"\r\n'
        f"Content-Type: application/octet-stream\r\n\r\n"
    ).encode()
    body = head + src.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(
        f"{GOTENBERG_URL}/forms/libreoffice/convert",
        data=body,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            pdf = resp.read()
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")[:300]
        print(f"gotenberg convert failed {e.code}: {detail}")
        raise RuntimeError("这份文档转不了，换一个再试") from e
    except Exception as e:
        print(f"gotenberg convert failed: {e}")
        raise RuntimeError("这份文档转不了，换一个再试") from e
    if not pdf.startswith(b"%PDF"):
        raise RuntimeError("这份文档转不了，换一个再试")
    dest = out_dir / f"{src.stem}.pdf"
    dest.write_bytes(pdf)
    return dest


def _soffice_convert(src: Path, out_dir: Path, target: str = "pdf") -> Path:
    out_dir.mkdir(parents=True, exist_ok=True)
    soffice = shutil.which("soffice") or shutil.which("libreoffice")
    if soffice and target == "pdf":
        cmd = [
            soffice,
            "--headless",
            "--nologo",
            "--nofirststartwizard",
            "--convert-to",
            target,
            "--outdir",
            str(out_dir),
            str(src),
        ]
        try:
            subprocess.run(cmd, check=True, capture_output=True, timeout=180)
            produced = list(out_dir.glob(src.stem + ".*"))
            if produced:
                return produced[0]
        except Exception as e:
            print(f"local soffice failed, fallback gotenberg: {e}")
    if target != "pdf":
        raise RuntimeError("这份文档转不了，换一个再试")
    return _convert_via_gotenberg(src, out_dir)


def _pdf_page_via_pdfium(src: Path, page_no: int, output: Path) -> Path:
    try:
        import pypdfium2 as pdfium
    except ImportError as e:
        raise RuntimeError("这一页转不成图片，稍后再试") from e
    doc = pdfium.PdfDocument(str(src))
    try:
        if page_no < 1 or page_no > len(doc):
            raise RuntimeError("这一页转不成图片，换一个文件再试")
        page = doc[page_no - 1]
        image = page.render(scale=2).to_pil()
        output.parent.mkdir(parents=True, exist_ok=True)
        image.save(str(output), format="PNG")
        return output
    finally:
        doc.close()


def _pdf_page_to_png(src: Path, page_no: int, output: Path) -> Path:
    """优先本机 pdftoppm，没有就用 PDFium（Chromium 同款渲染），不再要求用户安装 poppler。"""
    pdftoppm = shutil.which("pdftoppm")
    if not pdftoppm:
        return _pdf_page_via_pdfium(src, page_no, output)
    output.parent.mkdir(parents=True, exist_ok=True)
    # pdftoppm 输出前缀，会生成 prefix-1.png
    prefix = output.with_suffix("")
    cmd = [
        pdftoppm,
        "-png",
        "-f",
        str(page_no),
        "-l",
        str(page_no),
        "-singlefile",
        str(src),
        str(prefix),
    ]
    subprocess.run(cmd, check=True, capture_output=True, timeout=120)
    produced = prefix.with_suffix(".png")
    if not produced.exists():
        # 部分版本不支持 -singlefile
        candidates = sorted(prefix.parent.glob(prefix.name + "*.png"))
        if not candidates:
            raise RuntimeError("pdftoppm 未产出图片")
        produced = candidates[0]
    if produced.resolve() != output.resolve():
        shutil.copy2(produced, output)
        try:
            produced.unlink(missing_ok=True)
        except Exception:
            pass
    return output


def _images_to_pdf(paths: list[Path], output: Path) -> None:
    c = canvas.Canvas(str(output), pagesize=A4)
    page_w, page_h = A4
    for p in paths:
        img = Image.open(p)
        iw, ih = img.size
        scale = min(page_w / iw, page_h / ih)
        w, h = iw * scale, ih * scale
        x, y = (page_w - w) / 2, (page_h - h) / 2
        c.drawImage(str(p), x, y, width=w, height=h, preserveAspectRatio=True, mask="auto")
        c.showPage()
    c.save()


@app.get("/health")
def health():
    return {
        "ok": True,
        "libreoffice": bool(shutil.which("soffice") or shutil.which("libreoffice")),
        "gotenberg": _gotenberg_ready(),
        "pdftoppm": bool(shutil.which("pdftoppm")),
    }


@app.post("/estimate")
async def estimate(
    action: str = Form(...),
    file: UploadFile = File(...),
    quality: Optional[str] = Form("80"),
    maxDimension: Optional[str] = Form("1920"),
    mode: Optional[str] = Form("standard"),
    outputFormat: Optional[str] = Form("jpeg"),
    scale: Optional[str] = Form("1"),
):
    try:
        suffix = Path(file.filename or "f.bin").suffix.lower() or ".bin"
        src = _save_upload(file, suffix)
        q, md, fmt, sc = _compress_opts(quality, maxDimension, outputFormat, scale)
        if action == "word_compress":
            if suffix == ".doc":
                return JSONResponse({"ok": False, "message": "不支持 .doc，请转换为 .docx"})
            est = estimate_docx(src, q, md, fmt, sc)
        elif action == "pdf_compress":
            est = estimate_pdf(src, q, md)
        else:
            return JSONResponse({"ok": False, "message": "该动作不支持预估"})
        return {"ok": True, "estimated_size": est["estimatedSize"], "output_size": est["estimatedSize"]}
    except Exception as e:
        return JSONResponse({"ok": False, "message": str(e)})


@app.post("/process")
async def process(
    action: str = Form(...),
    file: UploadFile = File(...),
    output_path: Optional[str] = Form(None),
    extra_files: Optional[str] = Form(None),
    quality: Optional[str] = Form("80"),
    maxDimension: Optional[str] = Form("1920"),
    mode: Optional[str] = Form("standard"),
    pages: Optional[str] = Form(None),
    outputFormat: Optional[str] = Form("jpeg"),
    scale: Optional[str] = Form("1"),
):
    try:
        suffix = Path(file.filename or "f.bin").suffix.lower() or ".bin"
        src = _save_upload(file, suffix)
        q, md, fmt, sc = _compress_opts(quality, maxDimension, outputFormat, scale)

        if action == "word_compress":
            if suffix == ".doc":
                return JSONResponse({"ok": False, "message": "不支持 .doc，请转换为 .docx 后重试"})
            out = _out_path(output_path, ".docx")
            compress_docx(src, out, q, md, fmt, sc)
        elif action == "pdf_compress":
            out = _out_path(output_path, ".pdf")
            compress_pdf(src, out, q, md)
        elif action == "word_to_pdf":
            if suffix not in {".doc", ".docx"}:
                return JSONResponse({"ok": False, "message": "请上传 Word 文档"})
            with tempfile.TemporaryDirectory() as td:
                produced = _soffice_convert(src, Path(td), "pdf")
                out = _out_path(output_path, ".pdf")
                shutil.copy2(produced, out)
        elif action == "image_to_pdf":
            paths = [src]
            if extra_files:
                for p in extra_files.split(","):
                    if p.strip():
                        paths.append(Path(p.strip()))
            out = _out_path(output_path, ".pdf")
            _images_to_pdf(paths, out)
        elif action == "pdf_to_image":
            reader = PdfReader(str(src))
            if not reader.pages:
                return JSONResponse({"ok": False, "message": "空 PDF"})
            page_no = 1
            if pages:
                try:
                    page_no = max(1, int(str(pages).split("-")[0].split(",")[0]))
                except Exception:
                    page_no = 1
            page_no = min(page_no, len(reader.pages))
            out = _pdf_page_to_png(src, page_no, _out_path(output_path, ".png"))
        elif action == "pdf_merge":
            writer = PdfWriter()
            writer.append(str(src))
            if extra_files:
                for p in extra_files.split(","):
                    if p.strip():
                        writer.append(p.strip())
            out = _out_path(output_path, ".pdf")
            with open(out, "wb") as f:
                writer.write(f)
        elif action == "pdf_split":
            reader = PdfReader(str(src))
            # 默认拆出第一页作为结果（小程序单文件下载）；完整 zip 可二期
            if not reader.pages:
                return JSONResponse({"ok": False, "message": "空 PDF"})
            idx = 0
            if pages:
                try:
                    idx = max(0, int(pages.split("-")[0]) - 1)
                except Exception:
                    idx = 0
            idx = min(idx, len(reader.pages) - 1)
            writer = PdfWriter()
            writer.add_page(reader.pages[idx])
            out = _out_path(output_path, ".pdf")
            with open(out, "wb") as f:
                writer.write(f)
        else:
            return JSONResponse({"ok": False, "message": f"未知 action: {action}"})

        return {
            "ok": True,
            "output_path": str(out),
            "output_name": out.name,
            "output_size": out.stat().st_size,
            "estimated_size": out.stat().st_size,
        }
    except Exception as e:
        return JSONResponse({"ok": False, "message": str(e)})
