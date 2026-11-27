import sys
from PIL import Image, ImageDraw, ImageFont
def render(font, lines, out, size=72, width=1800, rtl_lines=()):
    f = ImageFont.truetype(font, size, layout_engine=ImageFont.Layout.RAQM)
    lh = int(size*1.45)
    img = Image.new("L", (width, lh*len(lines)+40), 255)
    d = ImageDraw.Draw(img)
    for i, t in enumerate(lines):
        kw = {}
        if i in rtl_lines:
            kw = dict(direction="rtl", language="ar")
            w = d.textlength(t, font=f, **kw)
            d.text((width-20-w, 20+i*lh), t, font=f, fill=0, **kw)
        else:
            d.text((20, 20+i*lh), t, font=f, fill=0, **kw)
    img.save(out)
if __name__ == "__main__":
    pass
