"""Erottaa Corners Apart -logon neljä palaa läpinäkyviksi kuviksi."""

from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter


@dataclass(frozen=True)
class Piece:
    name: str
    roi: tuple[int, int, int, int]
    blocks: tuple[tuple[int, int, int, int], ...]


PIECES = (
    Piece(
        "cyan",
        (150, 125, 630, 630),
        ((178, 151, 402, 376), (393, 151, 616, 402), (178, 365, 420, 611)),
    ),
    Piece(
        "orange",
        (620, 125, 1085, 630),
        ((635, 151, 852, 402), (843, 151, 1068, 402), (830, 365, 1068, 611)),
    ),
    Piece(
        "pink",
        (150, 615, 630, 1100),
        ((178, 633, 420, 856), (178, 845, 402, 1083), (393, 837, 615, 1083)),
    ),
    Piece(
        "green",
        (620, 615, 1085, 1100),
        ((830, 633, 1068, 856), (635, 837, 852, 1083), (843, 837, 1068, 1083)),
    ),
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--android-output", type=Path)
    parser.add_argument("--android-size", type=int, default=768)
    return parser.parse_args()


def binary_filter(mask: np.ndarray, operation: str, size: int) -> np.ndarray:
    image = Image.fromarray(mask.astype(np.uint8) * 255, mode="L")
    filter_type = ImageFilter.MaxFilter if operation == "dilate" else ImageFilter.MinFilter
    return np.asarray(image.filter(filter_type(size=size))) >= 128


def make_geometry_alpha(piece: Piece, roi_size: tuple[int, int]) -> np.ndarray:
    scale = 4
    roi_left, roi_top, _, _ = piece.roi
    width, height = roi_size
    mask = Image.new("L", (width * scale, height * scale), color=0)
    draw = ImageDraw.Draw(mask)

    for left, top, right, bottom in piece.blocks:
        local_box = (
            (left - roi_left) * scale,
            (top - roi_top) * scale,
            (right - roi_left) * scale,
            (bottom - roi_top) * scale,
        )
        draw.rounded_rectangle(local_box, radius=15 * scale, fill=255)

    alpha = np.asarray(
        mask.resize((width, height), Image.Resampling.LANCZOS),
        dtype=np.uint8,
    ).copy()
    alpha[alpha < 3] = 0
    alpha[alpha > 252] = 255
    return alpha


def propagate_core_colors(
    rgb: np.ndarray,
    hard_mask: np.ndarray,
    target_mask: np.ndarray,
) -> np.ndarray:
    propagated = rgb.astype(np.float32)
    known = binary_filter(hard_mask, "erode", 15)

    for _ in range(18):
        unknown = target_mask & ~known
        if not np.any(unknown):
            break

        color_sum = np.zeros_like(propagated)
        neighbor_count = np.zeros(hard_mask.shape, dtype=np.float32)
        for delta_y, delta_x in (
            (-1, -1),
            (-1, 0),
            (-1, 1),
            (0, -1),
            (0, 1),
            (1, -1),
            (1, 0),
            (1, 1),
        ):
            source_y = slice(max(0, -delta_y), hard_mask.shape[0] - max(0, delta_y))
            source_x = slice(max(0, -delta_x), hard_mask.shape[1] - max(0, delta_x))
            target_y = slice(max(0, delta_y), hard_mask.shape[0] - max(0, -delta_y))
            target_x = slice(max(0, delta_x), hard_mask.shape[1] - max(0, -delta_x))
            neighbor_known = known[source_y, source_x]
            color_sum[target_y, target_x] += (
                propagated[source_y, source_x] * neighbor_known[..., np.newaxis]
            )
            neighbor_count[target_y, target_x] += neighbor_known

        frontier = unknown & (neighbor_count > 0)
        propagated[frontier] = color_sum[frontier] / neighbor_count[frontier, np.newaxis]
        known |= frontier

    return np.rint(propagated).astype(np.uint8)


def despill(rgb: np.ndarray, alpha: np.ndarray, name: str) -> np.ndarray:
    cleaned = rgb.copy()
    visible = alpha > 0
    red = cleaned[..., 0]
    green = cleaned[..., 1]
    blue = cleaned[..., 2]

    if name == "cyan":
        red[visible] = np.minimum(red[visible], green[visible])
    elif name == "orange":
        blue[visible] = np.minimum(blue[visible], np.minimum(red[visible], green[visible]))
    elif name == "pink":
        blue[visible] = np.minimum(blue[visible], red[visible])
    elif name == "green":
        red[visible] = np.minimum(red[visible], green[visible])
        blue[visible] = np.minimum(blue[visible], green[visible])

    cleaned[~visible] = 0
    return cleaned


def purple_like(rgb: np.ndarray) -> np.ndarray:
    red = rgb[..., 0].astype(np.int16)
    green = rgb[..., 1].astype(np.int16)
    blue = rgb[..., 2].astype(np.int16)
    return (blue > red + 10) & (red > green + 7) & (blue > green + 18)


def alpha_bbox(alpha: np.ndarray) -> tuple[int, int, int, int]:
    ys, xs = np.nonzero(alpha)
    if len(xs) == 0:
        raise RuntimeError("Maski jäi tyhjäksi")
    return int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)


def resize_rgba_premultiplied(image: Image.Image, size: int) -> Image.Image:
    rgba = np.asarray(image, dtype=np.float32)
    alpha = rgba[..., 3] / 255.0
    premultiplied = rgba[..., :3] * alpha[..., np.newaxis]
    target = (size, size)

    resized_alpha = np.asarray(
        Image.fromarray(alpha, mode="F").resize(target, Image.Resampling.LANCZOS),
        dtype=np.float32,
    )
    resized_premultiplied = np.stack(
        [
            np.asarray(
                Image.fromarray(premultiplied[..., channel], mode="F").resize(
                    target,
                    Image.Resampling.LANCZOS,
                ),
                dtype=np.float32,
            )
            for channel in range(3)
        ],
        axis=-1,
    )

    resized_rgb = np.zeros_like(resized_premultiplied)
    visible = resized_alpha > 1e-5
    resized_rgb[visible] = resized_premultiplied[visible] / resized_alpha[visible, np.newaxis]
    resized_rgb_u8 = np.clip(np.rint(resized_rgb), 0, 255).astype(np.uint8)
    resized_alpha_u8 = np.clip(np.rint(resized_alpha * 255.0), 0, 255).astype(np.uint8)
    resized_rgb_u8[resized_alpha_u8 == 0] = 0
    resized_rgba = np.dstack((resized_rgb_u8, resized_alpha_u8))
    return Image.fromarray(resized_rgba, mode="RGBA")


def main() -> None:
    args = parse_args()
    source_bytes = args.input.read_bytes()
    source = Image.open(args.input).convert("RGB")
    source_rgb = np.asarray(source)
    canvas_width, canvas_height = source.size
    args.output.mkdir(parents=True, exist_ok=True)

    report: dict[str, object] = {
        "source": str(args.input.resolve()),
        "source_sha256": hashlib.sha256(source_bytes).hexdigest(),
        "source_size": [canvas_width, canvas_height],
        "mask_method": "three measured rounded rectangles, 4x supersampling",
        "pieces": {},
    }

    for piece in PIECES:
        left, top, right, bottom = piece.roi
        roi_rgb = source_rgb[top:bottom, left:right]
        alpha = make_geometry_alpha(piece, (right - left, bottom - top))
        hard_mask = alpha >= 128
        edge_cleaned_rgb = propagate_core_colors(roi_rgb, hard_mask, alpha > 0)
        cleaned_rgb = despill(edge_cleaned_rgb, alpha, piece.name)
        rgba = np.dstack((cleaned_rgb, alpha))

        local_bbox = alpha_bbox(alpha)
        bbox_left, bbox_top, bbox_right, bbox_bottom = local_bbox
        padding = 6
        crop_left = max(0, bbox_left - padding)
        crop_top = max(0, bbox_top - padding)
        crop_right = min(rgba.shape[1], bbox_right + padding)
        crop_bottom = min(rgba.shape[0], bbox_bottom + padding)

        tight = Image.fromarray(
            rgba[crop_top:crop_bottom, crop_left:crop_right],
            mode="RGBA",
        )
        tight_path = args.output / f"splash_piece_{piece.name}.png"
        save_png(tight, tight_path)

        canvas = np.zeros((canvas_height, canvas_width, 4), dtype=np.uint8)
        canvas[top:bottom, left:right] = rgba
        canvas_image = Image.fromarray(canvas, mode="RGBA")
        canvas_path = args.output / f"splash_piece_{piece.name}_canvas.png"
        save_png(canvas_image, canvas_path)

        android_path: Path | None = None
        android_purple_edge_pixels: int | None = None
        if args.android_output is not None:
            args.android_output.mkdir(parents=True, exist_ok=True)
            android_image = resize_rgba_premultiplied(canvas_image, args.android_size)
            android_array = np.asarray(android_image).copy()
            android_array[..., :3] = despill(
                android_array[..., :3],
                android_array[..., 3],
                piece.name,
            )
            android_image = Image.fromarray(android_array, mode="RGBA")
            android_path = args.android_output / f"splash_piece_{piece.name}.webp"
            android_image.save(
                android_path,
                format="WEBP",
                lossless=True,
                quality=100,
                method=6,
            )
            android_rgba = np.asarray(Image.open(android_path).convert("RGBA"))
            android_support = android_rgba[..., 3] > 0
            android_inner = binary_filter(android_support, "erode", 5)
            android_edge = android_support & ~android_inner
            android_purple_edge_pixels = int(
                np.count_nonzero(purple_like(android_rgba[..., :3]) & android_edge),
            )

        support = alpha > 0
        inner = binary_filter(support, "erode", 5)
        edge = support & ~inner
        purple_edge_pixels = int(np.count_nonzero(purple_like(cleaned_rgb) & edge))
        transparent_rgb_pixels = int(np.count_nonzero(np.any(cleaned_rgb[~support] != 0, axis=1)))

        report["pieces"][piece.name] = {
            "tight_file": tight_path.name,
            "canvas_file": canvas_path.name,
            "android_file": str(android_path.resolve()) if android_path is not None else None,
            "android_purple_edge_pixels": android_purple_edge_pixels,
            "tight_size": list(tight.size),
            "canvas_bbox": [
                left + bbox_left,
                top + bbox_top,
                left + bbox_right,
                top + bbox_bottom,
            ],
            "visible_pixels": int(np.count_nonzero(support)),
            "semi_transparent_pixels": int(np.count_nonzero((alpha > 0) & (alpha < 255))),
            "purple_edge_pixels": purple_edge_pixels,
            "nonzero_rgb_in_transparent_pixels": transparent_rgb_pixels,
        }

    report_path = args.output / "extraction-report.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
