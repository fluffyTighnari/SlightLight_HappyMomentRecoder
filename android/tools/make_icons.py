"""
Regenerates the launcher icons from the 512px web icon.

    python android/tools/make_icons.py
"""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "icon-512.png"
RES = ROOT / "android" / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

LEGACY_DP = 48
FOREGROUND_DP = 108


def resize(image: Image.Image, size: int) -> Image.Image:
    return image.resize((size, size), Image.LANCZOS)


def circle(image: Image.Image) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, image.size[0] - 1, image.size[1] - 1), fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def main() -> None:
    source = Image.open(SRC).convert("RGB")

    for name, scale in DENSITIES.items():
        folder = RES / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)

        legacy = resize(source, int(LEGACY_DP * scale))
        legacy.save(folder / "ic_launcher.png", optimize=True)
        circle(legacy).save(folder / "ic_launcher_round.png", optimize=True)

        foreground = resize(source, int(FOREGROUND_DP * scale))
        foreground.save(folder / "ic_launcher_foreground.png", optimize=True)

    print("icons written to", RES)


if __name__ == "__main__":
    main()
