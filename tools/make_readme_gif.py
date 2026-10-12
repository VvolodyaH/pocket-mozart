"""Capture a README GIF from the app's sprites and Java motion engine.

Requires JDK 17, Chromium, FFmpeg, and the Python playwright package.
This renders an animation showcase; it does not modify any source artwork.
"""
from pathlib import Path
import base64
import json
import os
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
FPS = 12
COUNT = 288
OUTPUT = ROOT / "art/mozart-preview.gif"

JAVA = r'''
package it.mozart.pocket;
import java.time.LocalDate;
public final class ReadmePreviewFrames {
    public static void main(String[] args) {
        WidgetTimeline.Frame[] route = null;
        int chosen = 0;
        for (int seed = 1; seed <= 500; seed++) {
            var candidate = ResidentTimeline.create(seed, 600, 132, 104, 45, false,
                    LocalDate.of(2026,10,12), 83, 288);
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            boolean left = false, right = false;
            int notes = 0;
            for (var frame : candidate) {
                lo = Math.min(lo, frame.x); hi = Math.max(hi, frame.x);
                if (frame.pose == MotionEngine.Pose.WALK) {
                    left |= frame.left; right |= !frame.left;
                }
                if (frame.noteAlpha > 0) notes++;
            }
            if (hi - lo >= 65 && left && right && notes >= 5 && candidate.length >= 220) {
                route = candidate; chosen = seed; break;
            }
        }
        if (route == null) throw new IllegalStateException("No representative route found");
        StringBuilder out = new StringBuilder("{\"seed\":").append(chosen).append(",\"route\":[");
        for (int i = 0; i < route.length; i++) {
            var f = route[i]; if (i > 0) out.append(',');
            out.append('[').append(f.pose.ordinal()).append(',').append(f.sprite).append(',')
                .append(f.x).append(',').append(f.y).append(',').append(f.size).append(',')
                .append(f.left).append(',').append(f.noteAlpha).append(',').append(f.noteVariant).append(',')
                .append(f.noteX).append(',').append(f.noteY).append(',').append(f.noteSize).append(']');
        }
        out.append("],\"sprites\":[");
        for (int p = 0; p < MotionEngine.Pose.values().length; p++) {
            if (p > 0) out.append(','); out.append('[');
            MotionEngine engine = new MotionEngine(42 + p);
            engine.setBounds(128,128,104);
            engine.force(MotionEngine.Pose.values()[p],30);
            for (int i = 0; i < 72; i++) {
                if (i > 0) out.append(','); out.append(engine.frame()); engine.update(1f/12);
            }
            out.append(']');
        }
        System.out.println(out.append("]}"));
    }
}
'''

HTML = r'''<!doctype html><html lang="en"><meta charset="utf-8">
<style>html,body{margin:0;width:640px;height:576px;overflow:hidden;background:#f8f4ef}canvas{display:block;width:640px;height:576px;image-rendering:pixelated}img{display:none}</style>
<canvas id="stage" width="640" height="576" aria-label="Pocket Mozart animation showcase"></canvas>
<img id="atlas" src="__ATLAS__"><img id="walk" src="__WALK__"><img id="extra" src="__EXTRA__">
<script>
const ctx=document.querySelector('#stage').getContext('2d');
const images=[document.querySelector('#atlas'),document.querySelector('#walk'),document.querySelector('#extra')];
const labels=['Breathing','Walking','Sheet music','Piano','Bow','Birthday','Violin','Conducting','Flute','Composing','Humming','Tea','Reading','Letter','Watch','Wig','Minuet','Stretching','Yawning','Napping','Waving','Tricorn','Rose','Music box','Travel','Applause'];
let data;
function box(x,y,w,h,fill,border){ctx.fillStyle=fill;ctx.beginPath();ctx.roundRect(x,y,w,h,12);ctx.fill();if(border){ctx.strokeStyle=border;ctx.lineWidth=1;ctx.stroke()}}
function text(value,x,y,size,color,bold=false){ctx.font=`${bold?'600':'400'} ${size}px system-ui,sans-serif`;ctx.fillStyle=color;ctx.fillText(value,x,y)}
function sprite(pose,index,x,y,size,left){
 const image=pose===1?images[1]:pose>=6?images[2]:images[0];
 const row=pose===1?Math.floor(index/4):pose>=6?pose-6:pose;
 ctx.save();ctx.translate(Math.round(x),Math.round(y));if(left){ctx.translate(size,0);ctx.scale(-1,1)}
 ctx.drawImage(image,(index%4)*256,row*256,256,256,0,0,size,size);ctx.restore();
}
window.prepare=async function(payload){
 data=payload;await Promise.all(images.map(image=>image.decode()));
 data.notes.forEach(note=>note.paths.forEach(path=>path.shape=new Path2D(path.d)));
};
window.render=function(index){
 const tick=(index+12)%288,group=Math.floor(tick/72),local=tick%72;
 ctx.clearRect(0,0,640,576);ctx.fillStyle='#f8f4ef';ctx.fillRect(0,0,640,576);ctx.imageSmoothingEnabled=false;
 ctx.fillStyle='#302b36';ctx.font='700 28px Georgia,serif';ctx.fillText('Pocket Mozart',20,33);
 text('26 sprite animations. One little composer.',20,54,13,'#847d83');
 const f=data.route[Math.floor(tick*data.route.length/288)];
 text('HOME SCREEN MOVEMENT',20,73,10,'#847d83',true);
 const current=f[0]===1?`Walking ${f[5]?'left':'right'}`:labels[f[0]];
 ctx.textAlign='right';text(current,620,73,12,'#ae373c',true);ctx.textAlign='left';
 box(20,82,600,132,'#eee3df','#cbb8b4');
 ctx.save();ctx.beginPath();ctx.roundRect(20,82,600,132,12);ctx.clip();
 sprite(f[0],f[1],20+f[2],82+f[3],f[4],f[5]);
 if(f[6]>0){
  const note=data.notes[f[7]],size=f[10];ctx.save();ctx.globalAlpha=f[6]/255;
  ctx.translate(Math.round(20+f[8]+(size-size*note.width/note.height)/2),Math.round(82+f[9]));
  ctx.scale(size/note.height,size/note.height);
  note.paths.forEach(path=>{ctx.fillStyle=path.color;ctx.fill(path.shape)});ctx.restore();
 }
 ctx.restore();
 text('ANIMATION SHOWCASE',20,238,10,'#847d83',true);
 for(let slot=0;slot<6;slot++){
  const pose=2+group*6+slot,x=20+(slot%3)*203,y=250+Math.floor(slot/3)*150;
  const resting=local<6||local>=66,drawPose=resting?0:pose;
  box(x,y,194,140,'#fffdfa','#e6ded5');
  sprite(drawPose,data.sprites[drawPose][local],x+41,y+5,112,group%2===1);
  ctx.textAlign='center';text(labels[pose],x+97,y+130,14,'#302b36',true);ctx.textAlign='left';
 }
 ctx.textAlign='center';text('Original app sprites · breathing, walking, music, and little breaks',320,564,11,'#847d83');ctx.textAlign='left';
};
</script></html>'''


def program(name):
    home = os.environ.get("JAVA_HOME")
    if home and name in {"java", "javac"}:
        candidate = Path(home) / "bin" / name
        if candidate.exists():
            return str(candidate)
    path = shutil.which(name)
    if not path:
        raise RuntimeError(f"Required program is missing: {name}")
    return path


def main():
    with tempfile.TemporaryDirectory(prefix="mozart-readme-") as directory:
        temp = Path(directory)
        java_source = temp / "ReadmePreviewFrames.java"
        java_source.write_text(JAVA)
        classes = temp / "classes"
        source = ROOT / "app/src/main/java/it/mozart/pocket"
        subprocess.run([program("javac"), "-d", str(classes),
                        *(str(source / name) for name in ["MotionEngine.java", "WidgetTimeline.java", "ResidentTimeline.java"]),
                        str(java_source)], check=True)
        payload = json.loads(subprocess.check_output([program("java"), "-cp", str(classes), "it.mozart.pocket.ReadmePreviewFrames"], text=True))
        namespace = "{http://schemas.android.com/apk/res/android}"
        payload["notes"] = []
        for name in ["quarter", "eighth", "pair"]:
            vector = ET.parse(ROOT / f"app/src/main/res/drawable/pixel_note_{name}.xml").getroot()
            payload["notes"].append({"width": int(vector.get(namespace + "viewportWidth")), "height": 24,
                                     "paths": [{"color": path.get(namespace + "fillColor"), "d": path.get(namespace + "pathData")} for path in vector]})
        def image_url(path):
            return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode("ascii")
        html = HTML.replace("__ATLAS__", image_url(ROOT / "app/src/main/assets/sprites/mozart.png"))
        html = html.replace("__WALK__", image_url(ROOT / "app/src/main/assets/sprites/mozart-walk.png"))
        html = html.replace("__EXTRA__", image_url(ROOT / "art/mozart-extra.png"))
        frames = temp / "frames"
        frames.mkdir()
        with sync_playwright() as playwright:
            chromium = shutil.which("chromium") or shutil.which("google-chrome")
            launch = {"headless": True, "args": ["--no-sandbox", "--disable-dev-shm-usage"]}
            if chromium:
                launch["executable_path"] = chromium
            browser = playwright.chromium.launch(**launch)
            page = browser.new_page(viewport={"width": 640, "height": 576}, device_scale_factor=1)
            page.set_content(html)
            page.evaluate("payload=>window.prepare(payload)", payload)
            for frame in range(COUNT):
                page.evaluate("frame=>window.render(frame)", frame)
                page.screenshot(path=str(frames / f"{frame:04d}.png"))
                if frame in {0, 72, 144, 216}:
                    print(f"Captured animation group {frame//72+1}/4", flush=True)
            page.screenshot(path=str(temp / "last.png"))
            browser.close()
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([program("ffmpeg"), "-hide_banner", "-loglevel", "error", "-y", "-threads", "2",
                        "-filter_complex_threads", "1", "-framerate", str(FPS), "-i", str(frames / "%04d.png"),
                        "-filter_complex", "split[a][b];[a]palettegen=max_colors=256:reserve_transparent=0[p];[b][p]paletteuse=dither=none",
                        "-loop", "0", str(OUTPUT)], check=True)
        shutil.copy2(frames / "0000.png", OUTPUT.with_suffix(".png"))
        # Keep the static poster useful for inspecting the generated media.
        print(f"Created {OUTPUT.relative_to(ROOT)}: {OUTPUT.stat().st_size:,} bytes; 24 seconds at {FPS} fps; route seed {payload['seed']}")


if __name__ == "__main__":
    main()
