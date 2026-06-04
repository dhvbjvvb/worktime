"""Regenerate foreground with minimal margin."""
from PIL import Image
import os

OUT = "app/src/main/res"
src = Image.open("图标.png").convert("RGBA")
w, h = src.size

# Fill entire 108dp minus just 2dp margin
margin = 2
target = 108 - 2 * margin
scale = target / max(w, h)
fw, fh = int(w * scale), int(h * scale)
fg = src.resize((fw, fh), Image.LANCZOS)
canvas = Image.new("RGBA", (108, 108), (0, 0, 0, 0))
canvas.paste(fg, ((108-fw)//2, (108-fh)//2), fg)
canvas.save(os.path.join(OUT, "drawable", "app_icon.png"), "PNG")

src.close()
print("Foreground margin reduced to 2dp.")
