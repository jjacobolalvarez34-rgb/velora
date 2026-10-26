"""
Genera core/data/src/main/assets/glyphs/tabler.tsv: los íconos en línea que Velora usa en el
estilo Minimalista (ADR-021), tomados de Tabler Icons (MIT, https://tabler.io/icons).

Uso (desde la raíz del repo):  python tools/icons/build_glyphs.py
Cada línea: nombre <TAB> trazos SVG separados por "|" (viewBox 24, trazo 2, extremos redondos).
"""
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

VERSION = "3.49.0"
BASE = f"https://cdn.jsdelivr.net/npm/@tabler/icons@{VERSION}"
OUT = Path("core/data/src/main/assets/glyphs/tabler.tsv")
LICENSE_OUT = Path("docs/licencias/MIT-TablerIcons.txt")

# Debe coincidir con los nombres que usa GlyphMatcher.kt.
NAMES = """
brand-whatsapp brand-instagram brand-facebook brand-messenger brand-telegram brand-tiktok brand-youtube
brand-spotify brand-netflix brand-x brand-discord brand-reddit brand-snapchat brand-pinterest brand-linkedin
brand-gmail brand-chrome brand-google-maps brand-google-photos brand-google-drive brand-google-play brand-google
brand-zoom brand-amazon brand-paypal brand-twitch brand-steam brand-duolingo brand-waze brand-airbnb brand-shazam
brand-soundcloud brand-deezer brand-threads brand-signal brand-viber brand-skype brand-tinder brand-wikipedia
brand-firefox brand-opera brand-github brand-vlc brand-canva brand-notion brand-trello brand-slack brand-dropbox
brand-edge brand-onedrive brand-teams brand-uber brand-binance brand-strava brand-tumblr brand-vimeo brand-patreon
brand-minecraft brand-playstation brand-xbox brand-openai brand-google-home brand-youtube-kids brand-snapseed
brand-meta brand-whatsapp brand-office brand-paypal brand-mastercard brand-visa brand-cashapp brand-revolut
camera clock phone address-book message calculator calendar settings photo folder music notes cloud mail world
shopping-bag map chess book microphone compass bulb heart wallet device-gamepad-2 lock shield-lock key
layout-grid radio movie microphone-2 language news scan shield trash network download device-tv book-2
crown skull sword users checklist accessible video file-text credit-card building-bank brush palette
headphones player-play pencil cards ghost rocket ball-football car truck plane bike shirt home
""".split()


def fetch(url: str) -> str:
    with urllib.request.urlopen(url, timeout=30) as response:
        return response.read().decode("utf-8")


def element_to_path(el: ET.Element) -> str | None:
    tag = el.tag.split("}")[-1]
    a = el.attrib
    if a.get("stroke") == "none":
        return None  # el cuadro invisible de 24x24 que trae cada ícono
    if tag == "path":
        return a["d"]
    if tag == "circle":
        cx, cy, r = float(a["cx"]), float(a["cy"]), float(a["r"])
        return f"M{cx - r} {cy}a{r} {r} 0 1 0 {2 * r} 0a{r} {r} 0 1 0 {-2 * r} 0"
    if tag == "ellipse":
        cx, cy, rx, ry = float(a["cx"]), float(a["cy"]), float(a["rx"]), float(a["ry"])
        return f"M{cx - rx} {cy}a{rx} {ry} 0 1 0 {2 * rx} 0a{rx} {ry} 0 1 0 {-2 * rx} 0"
    if tag == "line":
        return f"M{a['x1']} {a['y1']}L{a['x2']} {a['y2']}"
    if tag == "rect":
        x, y, w, h = (float(a.get(k, 0)) for k in ("x", "y", "width", "height"))
        return f"M{x} {y}h{w}v{h}h{-w}z"
    if tag in ("polyline", "polygon"):
        pts = a["points"].replace(",", " ").split()
        d = "M" + " L".join(f"{pts[i]} {pts[i + 1]}" for i in range(0, len(pts), 2))
        return d + ("z" if tag == "polygon" else "")
    return None


def main() -> int:
    lines, missing = [], []
    for name in dict.fromkeys(NAMES):  # sin repetidos, en orden
        try:
            svg = ET.fromstring(fetch(f"{BASE}/icons/outline/{name}.svg"))
        except Exception:  # noqa: BLE001 - un nombre que no existe en esta versión
            missing.append(name)
            continue
        paths = [p for p in (element_to_path(el) for el in svg.iter()) if p]
        lines.append(name + "\t" + "|".join(paths))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    LICENSE_OUT.write_text(fetch(f"{BASE}/LICENSE"), encoding="utf-8", newline="\n")
    print(f"{len(lines)} íconos -> {OUT} ({OUT.stat().st_size} bytes)")
    if missing:
        print("No existen en esta versión:", " ".join(missing))
    return 0


if __name__ == "__main__":
    sys.exit(main())
