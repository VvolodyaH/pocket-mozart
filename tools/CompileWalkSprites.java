import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Imports eight generated walk poses. Only packing, registration and alpha normalization. */
public final class CompileWalkSprites {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Use: CompileWalkSprites generated.png base-atlas.png walk-atlas.png");
        BufferedImage input = ImageIO.read(new File(args[0]));
        for (int y = 0; y < input.getHeight(); y++) for (int x = 0; x < input.getWidth(); x++) {
            int pixel = input.getRGB(x, y);
            input.setRGB(x, y, (pixel >>> 24) >= 220 ? pixel | 0xff000000 : 0);
        }
        BufferedImage[] cells = new BufferedImage[8];
        int[] bottom = new int[8], headCenter = new int[8];
        int tallest = 1;
        for (int frame = 0; frame < 8; frame++) {
            int col = frame % 4, row = frame / 4;
            int left = col * input.getWidth() / 4, top = row * input.getHeight() / 2;
            int right = (col + 1) * input.getWidth() / 4, end = (row + 1) * input.getHeight() / 2;
            BufferedImage cell = input.getSubimage(left, top, right - left, end - top);
            cells[frame] = cell;
            int minY = cell.getHeight(), maxY = -1, headLeft = cell.getWidth(), headRight = -1;
            for (int y = 0; y < cell.getHeight(); y++) for (int x = 0; x < cell.getWidth(); x++) {
                if ((cell.getRGB(x, y) >>> 24) == 0) continue;
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                if (y < cell.getHeight() * 0.55f) {
                    headLeft = Math.min(headLeft, x); headRight = Math.max(headRight, x);
                }
            }
            if (maxY < 0 || headRight < 0) throw new IllegalStateException("Empty walk frame " + frame);
            bottom[frame] = maxY + 1;
            headCenter[frame] = (headLeft + headRight + 1) / 2;
            tallest = Math.max(tallest, maxY - minY + 1);
        }
        float scale = 200f / tallest;
        BufferedImage walk = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = walk.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int frame = 0; frame < 8; frame++) {
            BufferedImage cell = cells[frame];
            int left = frame % 4 * 256 + Math.round(128 - headCenter[frame] * scale);
            int top = frame / 4 * 256 + 239 - Math.round(bottom[frame] * scale);
            g.drawImage(cell, left, top, Math.round(cell.getWidth() * scale), Math.round(cell.getHeight() * scale), null);
        }
        g.dispose();
        File out = new File(args[2]); out.getParentFile().mkdirs(); ImageIO.write(walk, "png", out);

        // Keep the catalogue illustration and legacy sheet consistent with the new walk.
        BufferedImage base = ImageIO.read(new File(args[1]));
        Graphics2D baseGraphics = base.createGraphics();
        baseGraphics.setComposite(AlphaComposite.Src);
        baseGraphics.drawImage(walk, 0, 256, 1024, 512, 0, 0, 1024, 256, null);
        baseGraphics.dispose();
        ImageIO.write(base, "png", new File(args[1]));
        File preview = new File(new File(args[1]).getParentFile(), "../../res/drawable-nodpi/mozart_preview.png");
        preview.getParentFile().mkdirs();
        ImageIO.write(base.getSubimage(0, 0, 256, 256), "png", preview);
        System.out.println("Eight 256x256 walk frames, registered heads, baseline 239, transparent catalogue portrait.");
    }
}
