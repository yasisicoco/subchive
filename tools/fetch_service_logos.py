from concurrent.futures import ThreadPoolExecutor
from hashlib import sha256
from html.parser import HTMLParser
from pathlib import Path
import json
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
REVISION = "b409eb0d1fa960277f8510d3ae3a19093b910ca4"
CATALOG = {
    "netflix": ("dashboard", "netflix"),
    "youtube": ("dashboard", "youtube"),
    "disney": ("dashboard", "disney-plus"),
    "tving": ("play", "net.cj.cjhv.gs.tving"),
    "wavve": ("play", "kr.co.captv.pooqV2"),
    "coupangplay": ("play", "com.coupang.mobile.play"),
    "spotify": ("dashboard", "spotify"),
    "applemusic": ("dashboard", "apple-music"),
    "melon": ("play", "com.iloen.melon"),
    "chatgpt": ("dashboard", "chatgpt"),
    "claude": ("play", "com.anthropic.claude"),
    "notion": ("dashboard", "notion"),
    "microsoft": ("dashboard", "microsoft-365"),
    "icloud": ("dashboard", "icloud"),
    "googleone": ("dashboard", "google-one"),
    "coupang": ("play", "com.coupang.mobile"),
    "naver": ("play", "com.nhn.android.search"),
    "gamepass": ("dashboard", "xbox-game-pass"),
}

class MetaParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.image = None
    def handle_starttag(self, tag, attributes):
        values = dict(attributes)
        if tag == "meta" and values.get("property") == "og:image":
            self.image = values.get("content")

def read(url):
    request = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(request, timeout=45) as response:
        return response.read()

def fetch(entry):
    key, (provider, name) = entry
    if provider == "dashboard":
        source = f"https://github.com/homarr-labs/dashboard-icons/blob/{REVISION}/png/{name}.png"
        image = f"https://raw.githubusercontent.com/homarr-labs/dashboard-icons/{REVISION}/png/{name}.png"
    else:
        source = f"https://play.google.com/store/apps/details?id={name}&hl=ko"
        parser = MetaParser()
        parser.feed(read(source).decode("utf-8"))
        if not parser.image:
            raise ValueError(f"No published icon for {key}")
        image = parser.image.split("=")[0] + "=s256"
    data = read(image)
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError(f"Expected PNG for {key}: {image}")
    destination = ROOT / "app/src/main/res/drawable-nodpi" / f"service_logo_{key}.png"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    result = {"key": key, "source": source, "download": image, "sha256": sha256(data).hexdigest(), "bytes": len(data), "modified": False}
    if key in ("naver", "coupang"):
        result["variant"] = "Parent service app icon identifying its membership service"
    print(f"{key}: {len(data)} bytes", flush=True)
    return result

if __name__ == "__main__":
    with ThreadPoolExecutor(max_workers=6) as executor:
        results = list(executor.map(fetch, CATALOG.items()))
    destination = ROOT / "app/src/main/assets/logo_sources.json"
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps({"retrieved": "2026-10-20", "icons": results}, ensure_ascii=False, indent=2), encoding="utf-8")
    licenses = ROOT / "app/src/main/assets/licenses"
    licenses.mkdir(exist_ok=True)
    (licenses / "dashboard-icons.txt").write_bytes(read(f"https://raw.githubusercontent.com/homarr-labs/dashboard-icons/{REVISION}/LICENSE"))
