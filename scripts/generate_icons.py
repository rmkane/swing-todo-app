#!/usr/bin/env python3
"""
Generate app icons from assets/logo.png or assets/logo.svg using ImageMagick.

Outputs:
  - src/main/resources/icons/icon-{16,32,48,64,128,256}.png
  - assets/icons/app.ico
  - assets/icons/app.icns

Requires:
  - ImageMagick 7+ (`magick` on PATH)
  - macOS `iconutil` for .icns generation
"""

from __future__ import annotations

import argparse
import platform
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "assets"
RESOURCES_ICONS = ROOT / "src" / "main" / "resources" / "icons"
PACKAGED_ICONS = ASSETS / "icons"

PNG_SIZES = (16, 32, 48, 64, 128, 256)

# Transparent inset per side (20px on the original 256×256 layout). SVG is full-bleed;
# padding is applied here so dock/taskbar icons are not edge-to-edge.
DEFAULT_PADDING_RATIO = 20 / 256

# From assets/logo.svg: rx on the 256×256 rounded rect.
DEFAULT_CORNER_RADIUS_RATIO = 64 / 256

ICNS_ICONSET = (
    ("icon_16x16.png", 16),
    ("icon_16x16@2x.png", 32),
    ("icon_32x32.png", 32),
    ("icon_32x32@2x.png", 64),
    ("icon_128x128.png", 128),
    ("icon_128x128@2x.png", 256),
    ("icon_256x256.png", 256),
    ("icon_256x256@2x.png", 512),
    ("icon_512x512.png", 512),
    ("icon_512x512@2x.png", 1024),
)


def find_magick() -> str:
    for name in ("magick", "convert"):
        path = shutil.which(name)
        if path:
            return path
    sys.exit("ImageMagick not found. Install it and ensure `magick` is on PATH.")


def resolve_source(explicit: Path | None) -> Path:
    if explicit is not None:
        if not explicit.is_file():
            sys.exit(f"Source image not found: {explicit}")
        return explicit

    png = ASSETS / "logo.png"
    svg = ASSETS / "logo.svg"

    if png.is_file():
        return png
    if svg.is_file():
        return svg

    sys.exit(f"No source image found. Add {png} or {svg}, or pass --source.")


def run_magick(magick: str, args: list[str]) -> None:
    cmd = [magick, *args]
    print(" ", " ".join(cmd))
    subprocess.run(cmd, check=True)


def content_side(size: int, padding_ratio: float) -> int:
    """Pixel side length for artwork after inset padding (minimum 1)."""
    inset = max(0.0, min(padding_ratio, 0.49))
    return max(1, round(size * (1 - 2 * inset)))


def resize_png(
    magick: str,
    source: Path,
    size: int,
    dest: Path,
    *,
    mask_corners: bool = True,
    corner_radius_ratio: float = DEFAULT_CORNER_RADIUS_RATIO,
    padding_ratio: float = DEFAULT_PADDING_RATIO,
) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)

    inner = content_side(size, padding_ratio)

    args = [
        "-background",
        "none",
        str(source),
        "-alpha",
        "on",
        "-resize",
        f"{inner}x{inner}",
        "-gravity",
        "center",
        "-background",
        "none",
        "-extent",
        f"{size}x{size}",
    ]

    if mask_corners:
        radius = round(size * corner_radius_ratio)

        # DstIn clips image to the roundrect; CopyOpacity + -alpha off leaves
        # black RGB in padded areas (shows as black corners in Swing / previews).
        args.extend(
            [
                "(",
                "-size",
                f"{size}x{size}",
                "xc:none",
                "-fill",
                "white",
                "-draw",
                f"roundrectangle 0,0 {size - 1},{size - 1} {radius},{radius}",
                ")",
                "-compose",
                "DstIn",
                "-composite",
            ]
        )

    args.extend(
        [
            "-define",
            "png:color-type=6",
            str(dest),
        ]
    )

    run_magick(magick, args)


def generate_pngs(
    magick: str,
    source: Path,
    *,
    mask_corners: bool,
    corner_radius_ratio: float,
    padding_ratio: float,
) -> dict[int, Path]:
    generated: dict[int, Path] = {}

    for size in PNG_SIZES:
        dest = RESOURCES_ICONS / f"icon-{size}.png"
        resize_png(
            magick,
            source,
            size,
            dest,
            mask_corners=mask_corners,
            corner_radius_ratio=corner_radius_ratio,
            padding_ratio=padding_ratio,
        )
        generated[size] = dest

    return generated


def generate_ico(magick: str, pngs: dict[int, Path]) -> Path:
    PACKAGED_ICONS.mkdir(parents=True, exist_ok=True)

    dest = PACKAGED_ICONS / "app.ico"
    sizes = ",".join(str(size) for size in PNG_SIZES)

    run_magick(
        magick,
        [
            *(str(pngs[size]) for size in PNG_SIZES),
            "-background",
            "none",
            "-alpha",
            "on",
            "-define",
            f"icon:auto-resize={sizes}",
            str(dest),
        ],
    )

    return dest


def generate_icns(
    magick: str,
    source: Path,
    *,
    mask_corners: bool,
    corner_radius_ratio: float,
    padding_ratio: float,
) -> Path | None:
    if platform.system() != "Darwin":
        print("Skipping app.icns (iconutil is macOS-only).")
        return None

    iconutil = shutil.which("iconutil")
    if not iconutil:
        print("Skipping app.icns (iconutil not found).")
        return None

    iconset = PACKAGED_ICONS / "app.iconset"

    if iconset.exists():
        shutil.rmtree(iconset)

    iconset.mkdir(parents=True)

    for name, size in ICNS_ICONSET:
        resize_png(
            magick,
            source,
            size,
            iconset / name,
            mask_corners=mask_corners,
            corner_radius_ratio=corner_radius_ratio,
            padding_ratio=padding_ratio,
        )

    dest = PACKAGED_ICONS / "app.icns"

    print(" ", iconutil, "-c", "icns", str(iconset), "-o", str(dest))
    subprocess.run([iconutil, "-c", "icns", str(iconset), "-o", str(dest)], check=True)

    shutil.rmtree(iconset)

    return dest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)

    parser.add_argument(
        "--source",
        type=Path,
        default=None,
        help="Source image. Default: assets/logo.png or assets/logo.svg",
    )
    parser.add_argument(
        "--skip-ico",
        action="store_true",
        help="Skip Windows .ico generation",
    )
    parser.add_argument(
        "--skip-icns",
        action="store_true",
        help="Skip macOS .icns generation",
    )
    parser.add_argument(
        "--no-corner-mask",
        action="store_true",
        help="Do not apply a final rounded-corner transparency mask",
    )
    parser.add_argument(
        "--corner-radius-ratio",
        type=float,
        default=DEFAULT_CORNER_RADIUS_RATIO,
        help=f"Corner radius as a ratio of icon size. Default: {DEFAULT_CORNER_RADIUS_RATIO:.6f}",
    )
    parser.add_argument(
        "--padding-ratio",
        type=float,
        default=DEFAULT_PADDING_RATIO,
        help=(
            "Transparent inset on each side as a ratio of the output icon "
            f"(e.g. 20/256 from the original SVG). Default: {DEFAULT_PADDING_RATIO:.6f}. "
            "Use 0 for edge-to-edge."
        ),
    )

    args = parser.parse_args()

    magick = find_magick()
    source = resolve_source(args.source)
    mask_corners = not args.no_corner_mask

    print(f"Source: {source}")
    print(f"Padding ratio: {args.padding_ratio:.6f} (inner {(1 - 2 * args.padding_ratio):.4f} of icon)")

    pngs = generate_pngs(
        magick,
        source,
        mask_corners=mask_corners,
        corner_radius_ratio=args.corner_radius_ratio,
        padding_ratio=args.padding_ratio,
    )

    print(f"Wrote {len(pngs)} PNGs to {RESOURCES_ICONS}")

    if not args.skip_ico:
        ico = generate_ico(magick, pngs)
        print(f"Wrote {ico}")

    if not args.skip_icns:
        icns = generate_icns(
            magick,
            source,
            mask_corners=mask_corners,
            corner_radius_ratio=args.corner_radius_ratio,
            padding_ratio=args.padding_ratio,
        )

        if icns:
            print(f"Wrote {icns}")

    print("Done.")


if __name__ == "__main__":
    main()
