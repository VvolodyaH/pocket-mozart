import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Exports existing atlas cells as native resources; no drawing or new artwork. */
public final class ExportWidgetFrames {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        BufferedImage base = ImageIO.read(root.resolve("app/src/main/assets/sprites/mozart.png").toFile());
        BufferedImage walk = ImageIO.read(root.resolve("app/src/main/assets/sprites/mozart-walk.png").toFile());
        String[] poses = {"idle", "walk", "score", "piano", "bow", "birthday"};
        Path png = root.resolve("app/src/main/res/drawable-nodpi");
        Path xml = root.resolve("app/src/main/res/drawable");
        Files.createDirectories(png); Files.createDirectories(xml);
        for (int row = 0; row < poses.length; row++) for (int frame = 0; frame < (row == 1 ? 8 : 4); frame++) {
            BufferedImage source = row == 1 ? walk : base;
            BufferedImage cell = source.getSubimage(frame % 4 * 256, (row == 1 ? frame / 4 : row) * 256, 256, 256);
            export(png, xml, poses[row] + "_" + frame, cell);
            if (row == 1) {
                BufferedImage left = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = left.createGraphics(); g.setComposite(AlphaComposite.Src);
                g.drawImage(cell, 256, 0, -256, 256, null); g.dispose();
                export(png, xml, "walk_left_" + frame, left);
            }
        }
        BufferedImage extra = ImageIO.read(root.resolve("art/mozart-extra.png").toFile());
        String[] more = {"violin","conduct","flute","quill","hum","tea","book","letter","watch","wig",
                "dance","stretch","yawn","nap","wave","tricorn","rose","music_box","travel","applause"};
        for (int row = 0; row < more.length; row++) for (int frame = 0; frame < 4; frame++)
            export(png, xml, more[row] + "_" + frame, extra.getSubimage(frame * 256, row * 256, 256, 256));
        System.out.println("116 native sprite resources exported; original pixels and transparency preserved.");
    }
    private static void export(Path png, Path xml, String name, BufferedImage cell) throws Exception {
        ImageIO.write(cell, "png", png.resolve("sprite_" + name + ".png").toFile());
        Files.writeString(xml.resolve("pixel_" + name + ".xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<bitmap xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:src=\"@drawable/sprite_" + name + "\" android:filter=\"false\" android:antialias=\"false\" android:autoMirrored=\"true\" />\n");
    }
}
