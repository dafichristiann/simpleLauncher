import os
import zipfile
from PIL import Image, ImageDraw

CREAM = (232, 223, 208, 255) # #E8DFD0
STROKE = 24

def new_canvas():
    return Image.new("RGBA", (512, 512), (0, 0, 0, 0))

def draw_calendar():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Calendar body
    d.rounded_rectangle([96, 110, 416, 430], radius=40, outline=CREAM, width=STROKE)
    # Header bar
    d.line([96, 190, 416, 190], fill=CREAM, width=STROKE)
    # Rings on top
    d.rounded_rectangle([160, 70, 190, 140], radius=15, fill=CREAM)
    d.rounded_rectangle([322, 70, 352, 140], radius=15, fill=CREAM)
    # Day dots
    for r in range(2):
        for c in range(3):
            cx = 166 + c * 90
            cy = 260 + r * 80
            d.ellipse([cx - 16, cy - 16, cx + 16, cy + 16], fill=CREAM)
    return im

def draw_camera():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Camera body
    d.rounded_rectangle([80, 150, 432, 420], radius=50, outline=CREAM, width=STROKE)
    # Flash knob
    d.rounded_rectangle([130, 100, 200, 150], radius=16, fill=CREAM)
    # Lens
    d.ellipse([180, 210, 332, 362], outline=CREAM, width=STROKE)
    d.ellipse([230, 260, 282, 312], fill=CREAM)
    # Small viewfinder dot
    d.ellipse([370, 190, 396, 216], fill=CREAM)
    return im

def draw_chrome():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Compass / chrome circle
    d.ellipse([96, 96, 416, 416], outline=CREAM, width=STROKE)
    # Inner center circle
    d.ellipse([206, 206, 306, 306], fill=CREAM)
    # 3 radial separator arcs/lines
    d.line([256, 96, 256, 206], fill=CREAM, width=STROKE)
    d.line([360, 336, 276, 290], fill=CREAM, width=STROKE)
    d.line([152, 336, 236, 290], fill=CREAM, width=STROKE)
    return im

def draw_clock():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    d.ellipse([80, 80, 432, 432], outline=CREAM, width=STROKE)
    d.ellipse([240, 240, 272, 272], fill=CREAM)
    # Hour hand to 10
    d.line([256, 256, 170, 170], fill=CREAM, width=STROKE)
    # Minute hand to 12
    d.line([256, 256, 256, 120], fill=CREAM, width=STROKE)
    return im

def draw_contacts():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Card outline
    d.rounded_rectangle([90, 80, 422, 432], radius=40, outline=CREAM, width=STROKE)
    # Head
    d.ellipse([216, 140, 296, 220], fill=CREAM)
    # Shoulders
    d.pieslice([156, 250, 356, 450], start=180, end=360, fill=CREAM)
    return im

def draw_drive():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Isometric triangle polygon bands
    d.polygon([(256, 80), (390, 310), (330, 414), (196, 184)], fill=CREAM)
    d.polygon([(256, 80), (122, 310), (182, 414), (316, 184)], outline=(43, 43, 43, 255), width=8)
    d.polygon([(122, 310), (390, 310), (330, 414), (62, 414)], fill=CREAM)
    return im

def draw_gmail():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Envelope
    d.rounded_rectangle([80, 130, 432, 382], radius=32, outline=CREAM, width=STROKE)
    # M fold lines
    d.line([(80, 140), (256, 280), (432, 140)], fill=CREAM, width=STROKE)
    d.line([(80, 372), (200, 270)], fill=CREAM, width=STROKE)
    d.line([(432, 372), (312, 270)], fill=CREAM, width=STROKE)
    return im

def draw_maps():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Map pin teardrop
    d.ellipse([136, 80, 376, 320], outline=CREAM, width=STROKE)
    d.polygon([(150, 250), (362, 250), (256, 440)], fill=CREAM)
    d.ellipse([216, 160, 296, 240], fill=(43, 43, 43, 255))
    return im

def draw_messages():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Speech bubble with tail
    d.rounded_rectangle([80, 100, 432, 350], radius=50, outline=CREAM, width=STROKE)
    d.polygon([(140, 345), (140, 425), (210, 345)], fill=CREAM)
    # 3 message dots
    for i in range(3):
        cx = 176 + i * 80
        d.ellipse([cx - 16, 225 - 16, cx + 16, 225 + 16], fill=CREAM)
    return im

def draw_music():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Double eighth note
    d.ellipse([110, 310, 190, 390], fill=CREAM)
    d.ellipse([270, 260, 350, 340], fill=CREAM)
    d.line([180, 350, 180, 130], fill=CREAM, width=STROKE)
    d.line([340, 300, 340, 80], fill=CREAM, width=STROKE)
    d.polygon([(170, 140), (350, 90), (350, 140), (170, 190)], fill=CREAM)
    return im

def draw_phone():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Classic curved handset
    d.arc([100, 100, 412, 412], start=100, end=170, fill=CREAM, width=80)
    d.ellipse([110, 110, 200, 200], fill=CREAM)
    d.ellipse([312, 312, 402, 402], fill=CREAM)
    return im

def draw_photos():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # 4-blade pinwheel
    d.pieslice([136, 136, 256, 256], start=180, end=270, fill=CREAM)
    d.pieslice([256, 136, 376, 256], start=270, end=360, fill=CREAM)
    d.pieslice([256, 256, 376, 376], start=0, end=90, fill=CREAM)
    d.pieslice([136, 256, 256, 376], start=90, end=180, fill=CREAM)
    d.ellipse([226, 226, 286, 286], fill=(43, 43, 43, 255))
    return im

def draw_settings():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Gear
    d.ellipse([140, 140, 372, 372], outline=CREAM, width=50)
    # Teeth (8 cogs)
    import math
    for i in range(8):
        ang = i * (math.pi / 4)
        cx = 256 + 170 * math.cos(ang)
        cy = 256 + 170 * math.sin(ang)
        d.rectangle([cx - 22, cy - 22, cx + 22, cy + 22], fill=CREAM)
    d.ellipse([216, 216, 296, 296], fill=(43, 43, 43, 255))
    return im

def draw_youtube():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # Rounded rectangle
    d.rounded_rectangle([80, 140, 432, 372], radius=60, outline=CREAM, width=STROKE)
    # Play triangle
    d.polygon([(220, 196), (330, 256), (220, 316)], fill=CREAM)
    return im

def draw_softhome():
    im = new_canvas()
    d = ImageDraw.Draw(im)
    # House roof
    d.polygon([(256, 90), (420, 240), (92, 240)], fill=CREAM)
    # House body
    d.rectangle([140, 240, 372, 420], outline=CREAM, width=STROKE)
    # Door
    d.rounded_rectangle([216, 290, 296, 420], radius=15, fill=CREAM)
    return im

def main():
    os.makedirs("generated_pack/res/drawable-xxhdpi", exist_ok=True)
    drawables = {
        "pack_calendar": draw_calendar(),
        "pack_camera": draw_camera(),
        "pack_chrome": draw_chrome(),
        "pack_clock": draw_clock(),
        "pack_contacts": draw_contacts(),
        "pack_drive": draw_drive(),
        "pack_gmail": draw_gmail(),
        "pack_maps": draw_maps(),
        "pack_messages": draw_messages(),
        "pack_music": draw_music(),
        "pack_phone": draw_phone(),
        "pack_photos": draw_photos(),
        "pack_settings": draw_settings(),
        "pack_youtube": draw_youtube(),
        "pack_softhome": draw_softhome(),
    }

    for name, img in drawables.items():
        p = f"generated_pack/res/drawable-xxhdpi/{name}.png"
        img.save(p, "PNG")
        print(f"Saved {p}, size={os.path.getsize(p)} bytes")

    appfilter_xml = """<resources>
  <item component="ComponentInfo{com.google.android.calendar/com.android.calendar.AllInOneActivity}" drawable="pack_calendar"/>
  <item component="ComponentInfo{com.android.camera2/com.android.camera.CameraLauncher}" drawable="pack_camera"/>
  <item component="ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}" drawable="pack_chrome"/>
  <item component="ComponentInfo{com.google.android.deskclock/com.android.deskclock.DeskClock}" drawable="pack_clock"/>
  <item component="ComponentInfo{com.google.android.contacts/com.android.contacts.activities.PeopleActivity}" drawable="pack_contacts"/>
  <item component="ComponentInfo{com.google.android.apps.docs/.app.NewMainProxyActivity}" drawable="pack_drive"/>
  <item component="ComponentInfo{com.google.android.apps.docs/com.google.android.apps.docs.app.NewMainProxyActivity}" drawable="pack_drive"/>
  <item component="ComponentInfo{com.google.android.gm/.ConversationListActivityGmail}" drawable="pack_gmail"/>
  <item component="ComponentInfo{com.google.android.gm/com.google.android.gm.ConversationListActivityGmail}" drawable="pack_gmail"/>
  <item component="ComponentInfo{com.google.android.apps.maps/com.google.android.maps.MapsActivity}" drawable="pack_maps"/>
  <item component="ComponentInfo{com.google.android.apps.messaging/.ui.ConversationListActivity}" drawable="pack_messages"/>
  <item component="ComponentInfo{com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity}" drawable="pack_messages"/>
  <item component="ComponentInfo{com.google.android.dialer/.extensions.GoogleDialtactsActivity}" drawable="pack_phone"/>
  <item component="ComponentInfo{com.google.android.dialer/com.google.android.dialer.extensions.GoogleDialtactsActivity}" drawable="pack_phone"/>
  <item component="ComponentInfo{com.google.android.apps.photos/.home.HomeActivity}" drawable="pack_photos"/>
  <item component="ComponentInfo{com.google.android.apps.photos/com.google.android.apps.photos.home.HomeActivity}" drawable="pack_photos"/>
  <item component="ComponentInfo{com.android.settings/.Settings}" drawable="pack_settings"/>
  <item component="ComponentInfo{com.android.settings/com.android.settings.Settings}" drawable="pack_settings"/>
  <item component="ComponentInfo{com.google.android.youtube/.app.honeycomb.Shell$HomeActivity}" drawable="pack_youtube"/>
  <item component="ComponentInfo{com.google.android.youtube/com.google.android.youtube.app.honeycomb.Shell$HomeActivity}" drawable="pack_youtube"/>
  <item component="ComponentInfo{com.google.android.apps.youtube.music/.activities.MusicActivity}" drawable="pack_music"/>
  <item component="ComponentInfo{com.google.android.apps.youtube.music/com.google.android.apps.youtube.music.activities.MusicActivity}" drawable="pack_music"/>
  <item component="ComponentInfo{com.softhome.launcher.debug/com.softhome.launcher.HomeActivity}" drawable="pack_softhome"/>
</resources>
"""
    with open("generated_pack/appfilter.xml", "w", encoding="utf-8") as f:
        f.write(appfilter_xml)

    with zipfile.ZipFile("SoftMonoTest.zip", "w", zipfile.ZIP_DEFLATED) as z:
        z.write("generated_pack/appfilter.xml", "appfilter.xml")
        for name in drawables.keys():
            rel = f"res/drawable-xxhdpi/{name}.png"
            z.write(f"generated_pack/{rel}", rel)

    print("SoftMonoTest.zip created successfully.")

if __name__ == "__main__":
    main()
