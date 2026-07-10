# Assets

Source artwork for the app. **Do not load these paths at runtime** — use generated icons on the classpath.

| File | Purpose |
| ---- | ------- |
| `logo.png` | Primary source for icon generation (512×512) |
| `logo.svg` | Vector source (used if `logo.png` is missing) |

## Generate runtime & packaging icons

```bash
make icons
# or
python3 scripts/generate_icons.py
```

**Outputs:**

- `src/main/resources/icons/icon-*.png` — window / taskbar (Swing)
- `assets/icons/app.ico` — Windows (e.g. jpackage)
- `assets/icons/app.icns` — macOS (requires `iconutil`, generated on Darwin only)

Requires [ImageMagick](https://imagemagick.org/) (`magick` on PATH).

**Transparent background:** The script passes `-background none` *before* the source image so SVG/PNG alpha is preserved. If you still see a solid fill, check `logo.svg` — the orange rounded rectangle is part of the artwork, not a generation artifact.