"""Light tiles with consistent line glyphs."""
from PIL import Image, ImageDraw, ImageFont
import os

out = r"d:\home\taobao-refund-assistant\miniprogram\src\static\icons"
os.makedirs(out, exist_ok=True)
S = 192


def font(size):
    for path in (r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\msyhbd.ttc"):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()


def tile(bg):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, S - 1, S - 1], radius=40, fill=bg)
    return img, d


def save(img, name):
    img.save(os.path.join(out, name + ".png"))


def file_icon(name, bg, ink, mark, size=28):
    img, d = tile(bg)
    d.rounded_rectangle([62, 40, 130, 152], radius=10, outline=ink, width=6)
    d.line([(104, 40), (130, 66), (104, 66), (104, 40)], fill=ink, width=5)
    f = font(size)
    bbox = d.textbbox((0, 0), mark, font=f)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text((96 - tw / 2, 108 - th / 2), mark, fill=ink, font=f)
    save(img, name)


def idphoto():
    img, d = tile((243, 244, 255, 255))
    ink = (92, 104, 220, 255)
    d.rounded_rectangle([70, 36, 122, 156], radius=8, outline=ink, width=6)
    d.ellipse([84, 56, 108, 80], outline=ink, width=5)
    d.arc([78, 88, 114, 140], 200, 340, fill=ink, width=5)
    save(img, "idphoto")


def img2pdf():
    img, d = tile((243, 246, 255, 255))
    ink = (69, 119, 245, 255)
    d.rounded_rectangle([48, 52, 108, 112], radius=8, outline=ink, width=6)
    d.ellipse([78, 64, 94, 80], outline=ink, width=4)
    d.line([(56, 100), (74, 84), (90, 98), (108, 78)], fill=ink, width=4)
    d.line([(124, 96), (156, 96)], fill=ink, width=6)
    d.polygon([(148, 84), (164, 96), (148, 108)], fill=ink)
    save(img, "img2pdf")


def compress():
    img, d = tile((236, 248, 245, 255))
    ink = (32, 150, 132, 255)
    d.rounded_rectangle([52, 52, 100, 140], radius=8, outline=ink, width=6)
    d.line([(124, 64), (124, 112)], fill=ink, width=6)
    d.polygon([(104, 112), (144, 112), (124, 140)], fill=ink)
    save(img, "compress")


def crop():
    img, d = tile((236, 248, 245, 255))
    ink = (32, 150, 132, 255)
    d.line([(56, 56), (88, 56)], fill=ink, width=6)
    d.line([(56, 56), (56, 88)], fill=ink, width=6)
    d.line([(136, 56), (104, 56)], fill=ink, width=6)
    d.line([(136, 56), (136, 88)], fill=ink, width=6)
    d.line([(56, 136), (56, 104)], fill=ink, width=6)
    d.line([(56, 136), (88, 136)], fill=ink, width=6)
    d.line([(136, 136), (136, 104)], fill=ink, width=6)
    d.line([(136, 136), (104, 136)], fill=ink, width=6)
    save(img, "crop")


def resize():
    img, d = tile((236, 248, 245, 255))
    ink = (32, 150, 132, 255)
    d.rounded_rectangle([48, 64, 112, 128], radius=8, outline=ink, width=6)
    d.line([(128, 96), (160, 96)], fill=ink, width=6)
    d.polygon([(152, 84), (168, 96), (152, 108)], fill=ink)
    save(img, "resize")


def fmt():
    img, d = tile((236, 248, 245, 255))
    ink = (32, 150, 132, 255)
    f = font(26)
    d.text((36, 58), "JPG", fill=ink, font=f)
    d.line([(48, 100), (144, 100)], fill=ink, width=4)
    d.text((40, 112), "PNG", fill=ink, font=font(22))
    save(img, "format")


def bg():
    img, d = tile((236, 248, 245, 255))
    ink = (32, 150, 132, 255)
    d.rounded_rectangle([48, 48, 144, 144], radius=12, outline=ink, width=6)
    d.rectangle([96, 54, 138, 138], fill=(255, 255, 255, 255))
    d.ellipse([82, 70, 110, 98], outline=ink, width=5)
    d.arc([74, 104, 118, 148], 200, 340, fill=ink, width=5)
    save(img, "bg")


def merge():
    img, d = tile((255, 246, 238, 255))
    ink = (214, 122, 48, 255)
    d.rounded_rectangle([48, 56, 96, 136], radius=8, outline=ink, width=6)
    d.rounded_rectangle([96, 72, 148, 152], radius=8, outline=ink, width=6)
    save(img, "merge")


def split():
    img, d = tile((255, 246, 238, 255))
    ink = (214, 122, 48, 255)
    d.rounded_rectangle([40, 56, 88, 136], radius=8, outline=ink, width=6)
    d.rounded_rectangle([104, 56, 152, 136], radius=8, outline=ink, width=6)
    save(img, "split")


idphoto()
file_icon("word", (243, 246, 255, 255), (43, 87, 154, 255), "W", 36)
file_icon("pdf", (253, 244, 244, 255), (196, 64, 64, 255), "PDF", 22)
file_icon("excel", (236, 248, 242, 255), (33, 115, 70, 255), "X", 36)
file_icon("ppt", (253, 244, 238, 255), (196, 78, 40, 255), "P", 36)
file_icon("word_c", (255, 246, 238, 255), (43, 87, 154, 255), "W", 36)
file_icon("pdf_c", (255, 246, 238, 255), (196, 64, 64, 255), "PDF", 22)
img2pdf()
compress()
crop()
resize()
fmt()
bg()
merge()
split()
print("ok")
