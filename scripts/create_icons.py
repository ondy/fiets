import math
import os
import struct
import zlib

OUTPUT_DIR = "src/main/resources/static/icons"
os.makedirs(OUTPUT_DIR, exist_ok=True)

WIDTH = HEIGHT = 512


def create_canvas(width, height, color=(0, 0, 0, 0)):
    r, g, b, a = color
    return [[[r, g, b, a] for _ in range(width)] for _ in range(height)]


def blend_pixel(pixels, x, y, color):
    height = len(pixels)
    width = len(pixels[0])
    if not (0 <= x < width and 0 <= y < height):
        return
    dst = pixels[y][x]
    alpha = color[3] / 255.0
    inv = 1.0 - alpha
    for i in range(3):
        dst[i] = int(color[i] * alpha + dst[i] * inv)
    dst[3] = int(255 - (1.0 - alpha) * (255 - dst[3]))


def draw_disc(pixels, cx, cy, radius, color):
    min_x = int(max(0, math.floor(cx - radius - 1)))
    max_x = int(min(len(pixels[0]) - 1, math.ceil(cx + radius + 1)))
    min_y = int(max(0, math.floor(cy - radius - 1)))
    max_y = int(min(len(pixels) - 1, math.ceil(cy + radius + 1)))
    radius_sq = radius * radius
    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            if dx * dx + dy * dy <= radius_sq:
                blend_pixel(pixels, x, y, color)


def draw_ring(pixels, cx, cy, radius, thickness, color):
    outer = radius + thickness / 2
    inner = max(0, radius - thickness / 2)
    outer_sq = outer * outer
    inner_sq = inner * inner
    min_x = int(max(0, math.floor(cx - outer - 1)))
    max_x = int(min(len(pixels[0]) - 1, math.ceil(cx + outer + 1)))
    min_y = int(max(0, math.floor(cy - outer - 1)))
    max_y = int(min(len(pixels) - 1, math.ceil(cy + outer + 1)))
    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            dist_sq = dx * dx + dy * dy
            if inner_sq <= dist_sq <= outer_sq:
                blend_pixel(pixels, x, y, color)


def draw_thick_line(pixels, start, end, width, color):
    steps = int(max(abs(end[0] - start[0]), abs(end[1] - start[1])) * 2) + 1
    for i in range(steps + 1):
        t = i / steps if steps else 0
        x = start[0] * (1 - t) + end[0] * t
        y = start[1] * (1 - t) + end[1] * t
        draw_disc(pixels, x, y, width / 2, color)


def draw_polyline(pixels, points, width, color):
    for i in range(len(points) - 1):
        draw_thick_line(pixels, points[i], points[i + 1], width, color)


def apply_glow(pixels, intensity=0.18):
    height = len(pixels)
    width = len(pixels[0])
    cx = width / 2
    cy = height / 2
    radius = min(width, height) * 0.45
    for y in range(height):
        for x in range(width):
            dx = x - cx
            dy = y - cy
            dist = math.sqrt(dx * dx + dy * dy)
            glow = max(0.0, 1.0 - dist / radius)
            if glow <= 0:
                continue
            blend = glow * intensity
            pixel = pixels[y][x]
            for i in range(3):
                pixel[i] = int(pixel[i] * (1 - blend) + 255 * blend)


def lerp_color(a, b, t):
    return [int(a[i] + (b[i] - a[i]) * t) for i in range(4)]


def resize_image(pixels, target_size):
    src_h = len(pixels)
    src_w = len(pixels[0])
    dst = [[[0, 0, 0, 0] for _ in range(target_size)] for _ in range(target_size)]
    scale_x = src_w / target_size
    scale_y = src_h / target_size
    for y in range(target_size):
        src_y = (y + 0.5) * scale_y - 0.5
        y0 = max(0, min(src_h - 1, int(math.floor(src_y))))
        y1 = max(0, min(src_h - 1, y0 + 1))
        ty = src_y - y0
        for x in range(target_size):
            src_x = (x + 0.5) * scale_x - 0.5
            x0 = max(0, min(src_w - 1, int(math.floor(src_x))))
            x1 = max(0, min(src_w - 1, x0 + 1))
            tx = src_x - x0
            c00 = pixels[y0][x0]
            c10 = pixels[y0][x1]
            c01 = pixels[y1][x0]
            c11 = pixels[y1][x1]
            top = [c00[i] * (1 - tx) + c10[i] * tx for i in range(4)]
            bottom = [c01[i] * (1 - tx) + c11[i] * tx for i in range(4)]
            value = [int(top[i] * (1 - ty) + bottom[i] * ty) for i in range(4)]
            dst[y][x] = value
    return dst


def write_png(path, pixels):
    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw.extend([r, g, b, a])
    compressed = zlib.compress(bytes(raw), 9)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png_data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png_data)


def main():
    pixels = create_canvas(WIDTH, HEIGHT)
    top_color = [11, 52, 100, 255]
    bottom_color = [15, 145, 182, 255]
    for y in range(HEIGHT):
        t = y / (HEIGHT - 1)
        row_color = lerp_color(top_color, bottom_color, t)
        for x in range(WIDTH):
            pixels[y][x] = row_color.copy()
    apply_glow(pixels)

    accent = (255, 255, 255, 240)
    secondary = (220, 247, 255, 180)
    wheel_radius = WIDTH * 0.18
    wheel_thickness = WIDTH * 0.035
    centers = [(WIDTH * 0.3, HEIGHT * 0.68), (WIDTH * 0.7, HEIGHT * 0.68)]
    for cx, cy in centers:
        draw_ring(pixels, cx, cy, wheel_radius, wheel_thickness, accent)
        draw_ring(pixels, cx, cy, wheel_radius * 0.45, wheel_thickness * 0.5, secondary)

    frame = [
        (WIDTH * 0.28, HEIGHT * 0.48),
        (WIDTH * 0.44, HEIGHT * 0.68),
        (WIDTH * 0.64, HEIGHT * 0.46),
        (WIDTH * 0.72, HEIGHT * 0.68),
    ]
    draw_polyline(pixels, frame[:3], wheel_thickness * 0.9, accent)
    draw_polyline(pixels, frame[1:], wheel_thickness * 0.9, accent)

    draw_thick_line(pixels, frame[1], (WIDTH * 0.46, HEIGHT * 0.36), wheel_thickness * 0.9, accent)
    draw_thick_line(pixels, (WIDTH * 0.42, HEIGHT * 0.32), (WIDTH * 0.56, HEIGHT * 0.32), wheel_thickness * 0.6, accent)
    draw_thick_line(pixels, frame[2], (WIDTH * 0.74, HEIGHT * 0.28), wheel_thickness * 0.8, accent)
    draw_thick_line(pixels, (WIDTH * 0.7, HEIGHT * 0.24), (WIDTH * 0.84, HEIGHT * 0.24), wheel_thickness * 0.55, accent)

    sparkle_center = (WIDTH * 0.2, HEIGHT * 0.2)
    draw_ring(pixels, sparkle_center[0], sparkle_center[1], WIDTH * 0.06, WIDTH * 0.02, secondary)
    draw_thick_line(pixels, (sparkle_center[0], sparkle_center[1] - WIDTH * 0.05), (sparkle_center[0], sparkle_center[1] + WIDTH * 0.05), WIDTH * 0.015, secondary)
    draw_thick_line(pixels, (sparkle_center[0] - WIDTH * 0.05, sparkle_center[1]), (sparkle_center[0] + WIDTH * 0.05, sparkle_center[1]), WIDTH * 0.015, secondary)

    base_path = os.path.join(OUTPUT_DIR, "icon-512.png")
    write_png(base_path, pixels)

    for size in [192, 180, 128, 64, 32]:
        resized = resize_image(pixels, size)
        write_png(os.path.join(OUTPUT_DIR, f"icon-{size}.png"), resized)

    icon32 = resize_image(pixels, 32)
    icon16 = resize_image(pixels, 16)

    def png_bytes(pix, name):
        path = os.path.join(OUTPUT_DIR, name)
        write_png(path, pix)
        with open(path, "rb") as handle:
            data = handle.read()
        os.remove(path)
        return data

    data32 = png_bytes(icon32, "_tmp32.png")
    data16 = png_bytes(icon16, "_tmp16.png")

    def ico_dir_entry(size, offset, data):
        return struct.pack("<BBBBHHII", size, size, 0, 0, 1, 32, len(data), offset)

    header = struct.pack("<HHH", 0, 1, 2)
    offset = 6 + 16 * 2
    entry32 = ico_dir_entry(32, offset, data32)
    offset += len(data32)
    entry16 = ico_dir_entry(16, offset, data16)
    ico_bytes = header + entry32 + entry16 + data32 + data16
    with open(os.path.join(OUTPUT_DIR, "favicon.ico"), "wb") as handle:
        handle.write(ico_bytes)


if __name__ == "__main__":
    main()
