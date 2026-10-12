"""Packages the existing atlas into an offline HTML preview; does not edit images."""
from pathlib import Path
import base64
import json
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
encoded = base64.b64encode((root / "app/src/main/assets/sprites/mozart.png").read_bytes()).decode("ascii")
walk = base64.b64encode((root / "app/src/main/assets/sprites/mozart-walk.png").read_bytes()).decode("ascii")
template = (root / "art/anteprima.template.html").read_text()
extra = base64.b64encode((root / "art/mozart-extra.png").read_bytes()).decode("ascii")
namespace = "{http://schemas.android.com/apk/res/android}"
notes = []
for name in ["quarter", "eighth", "pair"]:
    vector = ET.parse(root / f"app/src/main/res/drawable/pixel_note_{name}.xml").getroot()
    notes.append({"width":int(vector.get(namespace + "viewportWidth")),"height":24,
                  "paths":[{"color":p.get(namespace + "fillColor"),"d":p.get(namespace + "pathData")} for p in vector]})
(root / "art/anteprima.html").write_text(template.replace("__ATLAS_BASE64__", encoded).replace("__WALK_BASE64__", walk)
    .replace("__EXTRA_BASE64__", extra).replace("__NOTE_DATA__", json.dumps(notes)))
print("Created art/anteprima.html (offline, self-contained).")
