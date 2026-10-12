import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Imports the generated poses into equal game cells without redrawing the character. */
public final class CompileSprites {
    // Actual occupied row bands in the image generator's output; inclusive ends.
    private static final int[][] BANDS = {{25,266},{277,516},{523,766},{778,982},{995,1232},{1245,1513}};
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Use: CompileSprites input.png output.png");
        BufferedImage generated = ImageIO.read(new File(args[0]));
        if (generated.getWidth() != 1024 || generated.getHeight() != 1536)
            throw new IllegalArgumentException("Expected generated sheet 1024x1536");
        // Pixel art uses hard alpha: discard the generator's faint matte and make sprite pixels solid.
        for (int y=0; y<generated.getHeight(); y++) for (int x=0; x<generated.getWidth(); x++) {
            int pixel = generated.getRGB(x,y);
            generated.setRGB(x,y, ((pixel >>> 24) >= 220) ? (pixel | 0xff000000) : 0);
        }
        BufferedImage atlas = new BufferedImage(1024,1536,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int row=0; row<6; row++) for (int col=0; col<4; col++) {
            int sourceHeight = BANDS[row][1] - BANDS[row][0] + 1;
            int drawnWidth = Math.round(256 * 0.84f), drawnHeight = Math.round(sourceHeight * 0.84f);
            int minX=256,maxX=-1;
            // Align the feet across frames so idle never slides sideways. Piano uses its full scene.
            int anchorTop = row==3 ? BANDS[row][0] : BANDS[row][1]-24;
            for (int y=anchorTop;y<=BANDS[row][1];y++) for (int x=0;x<256;x++)
                if ((generated.getRGB(col*256+x,y)>>>24)>0) { minX=Math.min(minX,x); maxX=Math.max(maxX,x); }
            float anchorX = maxX>=minX ? (minX+maxX+1)/2f : 128;
            int left = col * 256 + Math.round(128-anchorX*drawnWidth/256f);
            int top = row * 256 + 239 - drawnHeight;
            g.drawImage(generated,left,top,left+drawnWidth,top+drawnHeight,
                    col*256,BANDS[row][0],(col+1)*256,BANDS[row][1]+1,null);
        }
        g.dispose();
        File out = new File(args[1]); out.getParentFile().mkdirs(); ImageIO.write(atlas,"png",out);
        System.out.println("24 frames, 256x256 cells, baseline 239, binary alpha: " + out);
    }
}
