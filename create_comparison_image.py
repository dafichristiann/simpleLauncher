import os
from PIL import Image, ImageDraw, ImageFont

def make_comparison():
    # Load screenshots
    img_before = Image.open("drawer_investigate.png") # The bug (charcoal donuts)
    img_automask = Image.open("drawer_real_screen.png") # Fixed AutoMask
    img_pack = Image.open("drawer_with_pack_screen.png") # Fixed Icon Pack

    # Target width and height for composite
    w, h = img_before.size
    # We want a 3-column comparison: Before (Bug), After (AutoMask), After (Icon Pack)
    # Scale each down slightly for a side-by-side poster
    scale = 0.5
    thumb_w = int(w * scale)
    thumb_h = int(h * scale)

    b_thumb = img_before.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
    a_thumb = img_automask.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
    p_thumb = img_pack.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)

    header_h = 100
    banner_w = thumb_w * 3 + 80
    banner_h = thumb_h + header_h + 40

    composite = Image.new("RGBA", (banner_w, banner_h), (237, 230, 216, 255)) # SoftBackground
    draw = ImageDraw.Draw(composite)

    # Draw header text
    try:
        font_title = ImageFont.truetype("arial.ttf", 32)
        font_subtitle = ImageFont.truetype("arial.ttf", 20)
    except:
        font_title = font_subtitle = ImageFont.load_default()

    draw.text((40, 25), "SOFT / HOME - Icon Rendering Pipeline Verification", fill=(43, 43, 43, 255), font=font_title)
    draw.text((40, 65), "Left: Bug (Charcoal Donut) | Center: Fixed AutoMask (Real Glyphs) | Right: Fixed Icon Pack (Pack Decode)", fill=(129, 121, 109, 255), font=font_subtitle)

    x1 = 20
    x2 = x1 + thumb_w + 20
    x3 = x2 + thumb_w + 20
    y = header_h

    composite.paste(b_thumb, (x1, y))
    composite.paste(a_thumb, (x2, y))
    composite.paste(p_thumb, (x3, y))

    # Add label badges over columns
    draw.rounded_rectangle([x1 + 10, y + 10, x1 + 220, y + 50], radius=10, fill=(200, 60, 60, 230))
    draw.text((x1 + 20, y + 18), "BEFORE: BUG (Donuts)", fill=(255, 255, 255, 255), font=font_subtitle)

    draw.rounded_rectangle([x2 + 10, y + 10, x2 + 250, y + 50], radius=10, fill=(40, 140, 60, 230))
    draw.text((x2 + 20, y + 18), "FIX: Real AutoMask", fill=(255, 255, 255, 255), font=font_subtitle)

    draw.rounded_rectangle([x3 + 10, y + 10, x3 + 270, y + 50], radius=10, fill=(40, 100, 180, 230))
    draw.text((x3 + 20, y + 18), "FIX: Real Icon Pack", fill=(255, 255, 255, 255), font=font_subtitle)

    composite.save("verification_comparison.png", "PNG")
    print("verification_comparison.png saved successfully.")

    # Also build a detailed tile comparison grid for 12 apps
    # Each tile in drawer_real_screen.png and drawer_with_pack_screen.png can be cropped
    # Drawer grid starts around y=630, x=70. Tile size approx 210x210, gap ~35
    # Let's crop individual tiles and make a clean grid
    tile_crops = [
        ("Calendar", 0, 0),
        ("Camera", 0, 1),
        ("Chrome", 0, 2),
        ("Clock", 0, 3),
        ("Contacts", 1, 0),
        ("Drive", 1, 1),
        ("Gmail", 1, 2),
        ("Maps", 1, 3),
        ("Messaging", 2, 0),
        ("Phone", 2, 1),
        ("Photos", 2, 2),
        ("Settings", 2, 3),
    ]

    grid_canvas = Image.new("RGBA", (1000, 1400), (232, 223, 208, 255))
    gdraw = ImageDraw.Draw(grid_canvas)
    gdraw.text((50, 30), "Detailed Per-App Tile Comparison (8-12 Apps)", fill=(43, 43, 43, 255), font=font_title)
    gdraw.text((50, 75), "Comparison of Before vs AutoMask vs Pack Decode per application", fill=(129, 121, 109, 255), font=font_subtitle)

    # Column headers
    gdraw.text((60, 130), "App Name", fill=(43, 43, 43, 255), font=font_subtitle)
    gdraw.text((260, 130), "Before (Bug)", fill=(180, 50, 50, 255), font=font_subtitle)
    gdraw.text((500, 130), "AutoMask (Fix)", fill=(40, 140, 60, 255), font=font_subtitle)
    gdraw.text((740, 130), "Icon Pack (Fix)", fill=(40, 100, 180, 255), font=font_subtitle)
    gdraw.line([(50, 165), (950, 165)], fill=(129, 121, 109, 150), width=2)

    # Tile coordinates in 1080x2400 screenshot
    # In drawer_real_screen:
    # 4 columns, tiles start around x=70, step ~245. row starts around y=625, step ~245
    start_x = 73
    start_y = 625
    step_x = 247
    step_y = 247
    tile_size = 200

    row_y = 180
    for name, r, c in tile_crops:
        tx = start_x + c * step_x
        ty = start_y + r * step_y
        box = (tx, ty, tx + tile_size, ty + tile_size)

        t_before = img_before.crop(box).resize((85, 85), Image.Resampling.LANCZOS)
        t_auto = img_automask.crop(box).resize((85, 85), Image.Resampling.LANCZOS)
        t_pack = img_pack.crop(box).resize((85, 85), Image.Resampling.LANCZOS)

        gdraw.text((60, row_y + 30), name, fill=(43, 43, 43, 255), font=font_subtitle)
        grid_canvas.paste(t_before, (280, row_y))
        grid_canvas.paste(t_auto, (520, row_y))
        grid_canvas.paste(t_pack, (760, row_y))

        gdraw.line([(50, row_y + 95), (950, row_y + 95)], fill=(216, 200, 182, 120), width=1)
        row_y += 100

    grid_canvas.save("tile_comparison_grid.png", "PNG")
    print("tile_comparison_grid.png saved successfully.")

if __name__ == "__main__":
    make_comparison()
