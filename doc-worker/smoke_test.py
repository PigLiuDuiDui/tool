from pathlib import Path
import io
import zipfile

import requests
from PIL import Image

root = Path(__file__).resolve().parents[1] / "uploads"
root.mkdir(exist_ok=True)
img = Image.new("RGB", (800, 600), (20, 40, 200))
buf = io.BytesIO()
img.save(buf, format="JPEG", quality=95)
raw = buf.getvalue()
docx = root / "sample.docx"
with zipfile.ZipFile(docx, "w") as z:
    z.writestr(
        "[Content_Types].xml",
        '<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"></Types>',
    )
    z.writestr(
        "word/document.xml",
        '<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"></w:document>',
    )
    z.writestr("word/media/a.jpg", raw)

print("docx_size", docx.stat().st_size)
with open(docx, "rb") as f:
    r = requests.post(
        "http://127.0.0.1:8092/estimate",
        data={"action": "word_compress", "mode": "standard"},
        files={"file": f},
    )
print("estimate", r.status_code, r.text)
with open(docx, "rb") as f:
    r2 = requests.post(
        "http://127.0.0.1:8092/process",
        data={"action": "word_compress", "mode": "standard"},
        files={"file": f},
    )
print("process", r2.status_code, r2.text[:400])
