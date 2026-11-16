#!/usr/bin/env python3
"""Export platform icon resources from the approved transparent logo.

Requires Pillow. This only scales, pads, masks platform tiles, and encodes files;
the master artwork and its colors are never redrawn or adjusted.
"""

from math import hypot
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
BRAND = Path(__file__).resolve().parent
WHITE = (255, 255, 255, 255)
LANCZOS = Image.Resampling.LANCZOS
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
IOS_FILL = 0.74
DESKTOP_FILL = 0.70
INLINE_FILL = 0.78
ANDROID_VIEWPORT_DP = 108
ANDROID_ARTWORK_DIAMETER_DP = 54
ANDROID_SAFE_DIAMETER_DP = 66


def save_png(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def mask_at(alpha, threshold):
    return alpha.point(lambda value: 255 if value >= threshold else 0)


def place(art, size, fraction):
    canvas = Image.new("RGBA", (size, size))
    scaled = art.copy()
    scaled.thumbnail((round(size * fraction), round(size * fraction)), LANCZOS)
    canvas.alpha_composite(scaled, ((size - scaled.width) // 2, (size - scaled.height) // 2))
    return canvas


def opaque_icon(art, size):
    canvas = Image.new("RGBA", (size, size), WHITE)
    canvas.alpha_composite(place(art, size, IOS_FILL))
    return canvas.convert("RGB")


def desktop_icon(art):
    # Draw at 2x for smooth corners in every exported platform size.
    size = 2048
    tile = Image.new("RGBA", (size, size))
    ImageDraw.Draw(tile).rounded_rectangle((64, 64, 1983, 1983), radius=420, fill=WHITE)
    tile = tile.resize((1024, 1024), LANCZOS)
    tile.alpha_composite(place(art, 1024, DESKTOP_FILL))
    return tile


def adaptive_foreground(art):
    """Keep the circle and crown compact, inside a central 54dp artwork circle."""
    size = 432  # 108dp at xxxhdpi, with the XML retaining a 108dp viewport.
    core = mask_at(art.getchannel("A"), 128)
    left, top, right, bottom = core.getbbox()
    center_x, center_y = (left + right - 1) / 2, (top + bottom - 1) / 2
    radius = 0.0
    for y in range(top, bottom):
        row = core.crop((left, y, right, y + 1)).getbbox()
        if row:
            radius = max(radius, *(hypot(left + x - center_x, y - center_y) for x in (row[0], row[2] - 1)))
    pixels_per_dp = size / ANDROID_VIEWPORT_DP
    # Leave extra space beyond the 54dp artwork, within the 66dp safe circle.
    scale = (ANDROID_ARTWORK_DIAMETER_DP * pixels_per_dp / 2) / radius
    scaled = art.resize((round(art.width * scale), round(art.height * scale)), LANCZOS)
    canvas = Image.new("RGBA", (size, size))
    position = (round(size / 2 - center_x * scale), round(size / 2 - center_y * scale))
    canvas.alpha_composite(scaled, position)
    # Verify the exported resource rather than just the placement calculation.
    alpha = canvas.getchannel("A")
    for y in range(size):
        row = mask_at(alpha.crop((0, y, size, y + 1)), 16).getbbox()
        if row:
            for x in (row[0], row[2] - 1):
                assert hypot(x - size / 2, y - size / 2) <= ANDROID_SAFE_DIAMETER_DP * pixels_per_dp / 2, "Adaptive icon exceeds safe circle"
    return canvas


def export():
    master = Image.open(BRAND / "logo-master.png").convert("RGBA")
    assert master.getchannel("A").getextrema() == (0, 255), "Master must have real transparency"
    # Ignore the background extractor's nearly invisible exterior alpha noise.
    # Keep four source pixels around visible artwork to retain antialiased edges.
    bounds = mask_at(master.getchannel("A"), 16).getbbox()
    assert bounds, "Empty logo master"
    bounds = (max(0, bounds[0] - 4), max(0, bounds[1] - 4),
              min(master.width, bounds[2] + 4), min(master.height, bounds[3] + 4))
    art = master.crop(bounds)

    save_png(place(art, 512, INLINE_FILL), ROOT / "shared/composeResources/drawable/ic_logo.png")
    flat = opaque_icon(art, 1024)
    save_png(flat, ROOT / "shared/composeResources/drawable/ic_logo_app_icon.png")
    save_png(flat, ROOT / "iosApp/src/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png")

    desktop = desktop_icon(art)
    icons = ROOT / "desktopApp/src/main/resources/icons"
    save_png(desktop.resize((512, 512), LANCZOS), icons / "Pomegranate.png")
    desktop.save(icons / "Pomegranate.ico", sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])
    desktop.save(icons / "Pomegranate.icns")

    resources = ROOT / "androidApp/res"
    save_png(adaptive_foreground(art), resources / "drawable-nodpi/ic_launcher_foreground_art.png")
    round_icon = flat.convert("RGBA")
    round_mask = Image.new("L", (1024, 1024))
    ImageDraw.Draw(round_mask).ellipse((0, 0, 1023, 1023), fill=255)
    round_icon.putalpha(round_mask)
    for density, size in DENSITIES.items():
        folder = resources / f"mipmap-{density}"
        save_png(desktop.resize((size, size), LANCZOS), folder / "ic_launcher.png")
        save_png(round_icon.resize((size, size), LANCZOS), folder / "ic_launcher_round.png")

    print("Exported 17 platform resources; Android artwork uses 54dp inside the 66dp safe circle.")


if __name__ == "__main__":
    export()
