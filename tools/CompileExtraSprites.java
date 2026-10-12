import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Cuts and registers the generated art; never draws or invents character pixels. */
public final class CompileExtraSprites {
    private static final int[][][] BANDS = {
        {{26,294},{325,594},{624,897},{927,1200},{1229,1499}},
        {{10,286},{299,575},{587,868},{879,1178},{1188,1466}},
        {{24,290},{325,592},{622,893},{922,1190},{1220,1489}},
        {{34,300},{330,594},{620,883},{904,1183},{1204,1475}}
    };
    private static final String[] SHEETS = {"music","study","rest","stage"};
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        BufferedImage atlas = new BufferedImage(1024, 20 * 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        final float scale = .73f;
        for (int sheet = 0; sheet < SHEETS.length; sheet++) {
            BufferedImage input = ImageIO.read(root.resolve("art/mozart-" + SHEETS[sheet] + "-generated.png").toFile());
            if (input.getWidth() != 1024 || input.getHeight() != 1536) throw new IllegalArgumentException("Unexpected grid");
            for (int y = 0; y < input.getHeight(); y++) for (int x = 0; x < input.getWidth(); x++) {
                int p = input.getRGB(x,y);
                input.setRGB(x,y, (p >>> 24) >= 220 ? p | 0xff000000 : 0);
            }
            for (int row = 0; row < 5; row++) for (int col = 0; col < 4; col++) {
                int top = BANDS[sheet][row][0], bottom = BANDS[sheet][row][1];
                // The largest connected skin region is the face, even when a hand is raised.
                float anchor = faceAnchor(input, col * 256, top, bottom);
                int feet = top;
                for (int y = bottom - 40; y <= bottom; y++)
                    for (int x = Math.max(0, (int) anchor - 48); x < Math.min(256, (int) anchor + 48); x++)
                        if ((input.getRGB(col * 256 + x, y) >>> 24) != 0) feet = Math.max(feet, y);
                int drawnWidth = Math.round(256 * scale), drawnHeight = Math.round((bottom - top + 1) * scale);
                int dx = col * 256 + Math.round(140 - anchor * scale);
                int dy = (sheet * 5 + row) * 256 + 239 - Math.round((feet - top + 1) * scale);
                g.drawImage(input, dx, dy, dx + drawnWidth, dy + drawnHeight,
                        col * 256, top, (col + 1) * 256, bottom + 1, null);
            }
        }
        g.dispose();
        Path cleanup=root.resolve("art/mozart-violin-cleanup-patch.png");
        if (java.nio.file.Files.exists(cleanup)) {
            BufferedImage patch=ImageIO.read(cleanup.toFile());
            Graphics2D fix=atlas.createGraphics(); fix.setComposite(AlphaComposite.Src);
            for (int frame:new int[]{1,3}) fix.drawImage(patch,frame*256+40,171,null);
            fix.dispose();
        }
        Path output = root.resolve("art/mozart-extra.png");
        ImageIO.write(atlas, "png", output.toFile());
        for (int sheet = 0; sheet < 4; sheet++)
            ImageIO.write(atlas.getSubimage(0, sheet * 1280, 1024, 1280), "png",
                    root.resolve("app/src/main/assets/sprites/mozart-extra-" + sheet + ".png").toFile());
        System.out.println("20 generated activities, 80 registered transparent frames: " + output);
    }

    private static float faceAnchor(BufferedImage input, int column, int top, int bottom) {
        int length = (bottom - top + 1) * 256;
        boolean[] skin = new boolean[length], visited = new boolean[length];
        int[] queue = new int[length];
        for (int i = 0; i < length; i++) {
            int p = input.getRGB(column + i % 256, top + i / 256);
            int r = p >>> 16 & 255, g = p >>> 8 & 255, b = p & 255;
            skin[i] = (p >>> 24) != 0 && r > 175 && g > 120 && r > g + 20 && b < g + 20;
        }
        int largest = 0, left = 0, right = 255;
        for (int start = 0; start < length; start++) if (skin[start] && !visited[start]) {
            int head = 0, tail = 1, min = 256, max = -1;
            queue[0] = start; visited[start] = true;
            while (head < tail) {
                int current = queue[head++], x = current % 256;
                min = Math.min(min, x); max = Math.max(max, x);
                for (int offset : new int[]{-1,1,-256,256}) {
                    int next = current + offset;
                    if (next < 0 || next >= length || offset == -1 && x == 0 || offset == 1 && x == 255) continue;
                    if (skin[next] && !visited[next]) { visited[next] = true; queue[tail++] = next; }
                }
            }
            if (tail > largest) { largest = tail; left = min; right = max; }
        }
        return (left + right + 1) / 2f;
    }
}
