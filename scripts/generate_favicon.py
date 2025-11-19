"""Generate favicon assets without third-party imaging libraries."""

import struct
import zlib
from pathlib import Path
from typing import Tuple


ROOT = Path(__file__).resolve().parents[1]
ICON_DIR = ROOT / "src" / "main" / "resources" / "static" / "icons"

# Colors (RGBA)
BACKGROUND = (6, 15, 31, 255)
ACCENT = (246, 96, 37, 255)
FOREGROUND = (255, 255, 255, 255)


def in_circle(x: float, y: float, radius: float = 0.38) -> bool:
    dx = x - 0.5
    dy = y - 0.5
    return dx * dx + dy * dy <= radius * radius


def in_rounded_rect(
    x: float, y: float, x0: float, x1: float, y0: float, y1: float, radius: float
) -> bool:
    if not (x0 <= x <= x1 and y0 <= y <= y1):
        return False
    if radius <= 0:
        return True

    inner_x0 = x0 + radius
    inner_x1 = x1 - radius
    inner_y0 = y0 + radius
    inner_y1 = y1 - radius

    if inner_x0 <= x <= inner_x1 or inner_y0 <= y <= inner_y1:
        return True

    cx = inner_x0 if x < inner_x0 else inner_x1
    cy = inner_y0 if y < inner_y0 else inner_y1
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= radius * radius


def color_at(x: float, y: float) -> Tuple[int, int, int, int]:
    in_badge = in_circle(x, y)
    color = ACCENT if in_badge else BACKGROUND
    if not in_badge:
        return color

    radius = 0.025
    if in_rounded_rect(x, y, 0.35, 0.46, 0.28, 0.77, radius):
        return FOREGROUND
    if in_rounded_rect(x, y, 0.35, 0.74, 0.28, 0.38, radius):
        return FOREGROUND
    if in_rounded_rect(x, y, 0.35, 0.68, 0.46, 0.56, radius):
        return FOREGROUND

    return color


def sample_pixel(x: int, y: int, size: int) -> Tuple[int, int, int, int]:
    offsets = (0.25, 0.75)
    accum = [0.0, 0.0, 0.0, 0.0]
    count = 0
    for ox in offsets:
        for oy in offsets:
            fx = (x + ox) / size
            fy = (y + oy) / size
            rgba = color_at(fx, fy)
            for i in range(4):
                accum[i] += rgba[i]
            count += 1
    return tuple(int(round(v / count)) for v in accum)


def build_pixel_buffer(size: int) -> bytes:
    buf = bytearray()
    for y in range(size):
        for x in range(size):
            buf.extend(sample_pixel(x, y, size))
    return bytes(buf)


def chunk(tag: bytes, data: bytes) -> bytes:
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


def png_bytes(width: int, height: int, pixels: bytes) -> bytes:
    assert len(pixels) == width * height * 4
    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        start = y * stride
        raw.extend(pixels[start : start + stride])
    compressed = zlib.compress(bytes(raw))
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")


def render_png(size: int) -> bytes:
    pixels = build_pixel_buffer(size)
    return png_bytes(size, size, pixels)


def save_png(path: Path, size: int) -> bytes:
    data = render_png(size)
    path.write_bytes(data)
    print(f"Created {path.relative_to(ROOT)}")
    return data


def save_ico(path: Path, png_data: bytes, size: int) -> None:
    width_byte = size if size < 256 else 0
    entry = struct.pack(
        "<BBBBHHII",
        width_byte,
        width_byte,
        0,
        0,
        1,
        32,
        len(png_data),
        6 + 16,
    )
    header = struct.pack("<HHH", 0, 1, 1)
    path.write_bytes(header + entry + png_data)
    print(f"Created {path.relative_to(ROOT)}")


def main() -> None:
    ICON_DIR.mkdir(parents=True, exist_ok=True)
    sizes = [512, 192, 180, 128, 64, 32]
    for size in sizes:
        png_path = ICON_DIR / f"icon-{size}.png"
        save_png(png_path, size)
    ico_path = ICON_DIR / "favicon.ico"
    ico_size = 256
    ico_png = render_png(ico_size)
    save_ico(ico_path, ico_png, size=ico_size)


if __name__ == "__main__":
    main()
