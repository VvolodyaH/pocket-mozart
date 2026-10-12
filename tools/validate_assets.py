"""Read-only validation of the generated Android sprite atlas. Requires Pillow."""
from pathlib import Path
from PIL import Image
from collections import deque

root = Path(__file__).resolve().parents[1]
image = Image.open(root / "app/src/main/assets/sprites/mozart.png")
assert image.mode == "RGBA" and image.size == (1024, 1536)
alpha = image.getchannel("A")
histogram = alpha.histogram()
assert sum(histogram[1:255]) == 0 and histogram[0] > 0 and histogram[255] > 0
for row in range(6):
    for column in range(4):
        cell = alpha.crop((column*256, row*256, (column+1)*256, (row+1)*256))
        bounds = cell.getbbox()
        assert bounds and bounds[0] >= 8 and bounds[1] >= 8 and bounds[2] <= 248 and bounds[3] <= 248, (row, column, bounds)
walk = Image.open(root / "app/src/main/assets/sprites/mozart-walk.png")
assert walk.mode == "RGBA" and walk.size == (1024, 512)
walk_alpha = walk.getchannel("A")
assert sum(walk_alpha.histogram()[1:255]) == 0
for frame in range(8):
    cell = walk_alpha.crop((frame%4*256, frame//4*256, (frame%4+1)*256, (frame//4+1)*256))
    bounds = cell.getbbox()
    assert bounds and min(bounds[:2]) >= 8 and max(bounds[2:]) <= 248, (frame, bounds)
poses = ["idle", "walk", "score", "piano", "bow", "birthday"]
count = 0
for row, pose in enumerate(poses):
    for frame in range(8 if pose == "walk" else 4):
        source = walk if pose == "walk" else image
        y = frame // 4 if pose == "walk" else row
        expected = source.crop((frame % 4 * 256, y * 256, (frame % 4 + 1) * 256, (y + 1) * 256))
        actual = Image.open(root / f"app/src/main/res/drawable-nodpi/sprite_{pose}_{frame}.png").convert("RGBA")
        assert actual.tobytes() == expected.tobytes(), (pose, frame)
        count += 1
        if pose == "walk":
            actual = Image.open(root / f"app/src/main/res/drawable-nodpi/sprite_walk_left_{frame}.png").convert("RGBA")
            assert actual.tobytes() == expected.transpose(Image.Transpose.FLIP_LEFT_RIGHT).tobytes(), frame
            count += 1
extra = Image.open(root / "art/mozart-extra.png")
assert extra.mode == "RGBA" and extra.size == (1024, 5120)
assert sum(extra.getchannel("A").histogram()[1:255]) == 0
new = ["violin","conduct","flute","quill","hum","tea","book","letter","watch","wig",
       "dance","stretch","yawn","nap","wave","tricorn","rose","music_box","travel","applause"]
for row, pose in enumerate(new):
    distinct = set()
    for frame in range(4):
        expected = extra.crop((frame*256,row*256,(frame+1)*256,(row+1)*256))
        actual = Image.open(root / f"app/src/main/res/drawable-nodpi/sprite_{pose}_{frame}.png").convert("RGBA")
        assert actual.tobytes() == expected.tobytes(), (pose,frame)
        bounds = actual.getchannel("A").getbbox()
        assert min(bounds[:2]) >= 8 and max(bounds[2:]) <= 248, (pose,frame,bounds)
        distinct.add(actual.tobytes()); count += 1
    assert len(distinct) == 4, (pose,"duplicate keyframes")
for sheet in range(4):
    actual = Image.open(root / f"app/src/main/assets/sprites/mozart-extra-{sheet}.png").convert("RGBA")
    assert actual.tobytes() == extra.crop((0,sheet*1280,1024,(sheet+1)*1280)).tobytes()
assert count == 116

# A breathing rig must move the chest while the original foot pixels remain anchored.
reference = Image.open(root / "art/mozart-idle-reference.png").convert("RGBA")
chest = []
for frame in range(4):
    sprite = Image.open(root / f"app/src/main/res/drawable-nodpi/sprite_idle_{frame}.png").convert("RGBA")
    assert sprite.crop((0,216,256,256)).tobytes() == reference.crop((0,216,256,256)).tobytes(), (frame,"moving feet")
    positions = [y for y in range(160,198) for x in range(112,159)
                 if (p := sprite.getpixel((x,y)))[3] and min(p[:3]) > 185]
    assert positions, (frame,"missing cravat")
    chest.append(sum(positions)/len(positions))
assert chest[0] > chest[1] > chest[2] and chest[3] > chest[2], chest
assert max(chest)-min(chest) > 1, chest

# The two repaired frames must differ only in the detached dirt rectangle.
for frame in [1,3]:
    before = Image.open(root / f"art/mozart-violin-before-{frame}.png").convert("RGBA")
    after = Image.open(root / f"app/src/main/res/drawable-nodpi/sprite_violin_{frame}.png").convert("RGBA")
    changed = 0
    for y in range(256):
        for x in range(256):
            if before.getpixel((x,y)) != after.getpixel((x,y)):
                assert 40 <= x < 44 and 171 <= y < 184, (frame,"character changed",x,y)
                assert after.getpixel((x,y))[3] == 0
                changed += 1
    assert changed > 0
for frame in range(4):
    alpha = Image.open(root / f"app/src/main/res/drawable-nodpi/sprite_violin_{frame}.png").getchannel("A")
    pixels = alpha.load(); visited = set(); components = 0
    for y in range(256):
        for x in range(256):
            if not pixels[x,y] or (x,y) in visited:
                continue
            components += 1; queue = deque([(x,y)]); visited.add((x,y))
            while queue:
                px,py = queue.popleft()
                for dx in [-1,0,1]:
                    for dy in [-1,0,1]:
                        nx,ny = px+dx,py+dy
                        if 0 <= nx < 256 and 0 <= ny < 256 and pixels[nx,ny] and (nx,ny) not in visited:
                            visited.add((nx,ny)); queue.append((nx,ny))
    assert components == 1, (frame,"stray pixels",components)
print("OK: 116 transparent native resources; idle chest moves with exact foot pixels fixed; all violin frames have no detached fragments; repair preserves every character pixel.")
