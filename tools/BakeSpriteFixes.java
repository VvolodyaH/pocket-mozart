import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Bakes a game-animation rig and applies only the generator's transparent cleanup patch. */
public final class BakeSpriteFixes {
    private static final int FOOT_ANCHOR=216, HIP=182;
    private static final int[] RISE={0,2,4,2};
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args.length==0?".":args[0]);
        BufferedImage reference=ImageIO.read(root.resolve("art/mozart-idle-reference.png").toFile());
        if(reference.getWidth()!=256 || reference.getHeight()!=256) throw new IllegalArgumentException("Idle reference must be 256 square");
        BufferedImage base=ImageIO.read(root.resolve("app/src/main/assets/sprites/mozart.png").toFile());
        for(int frame=0;frame<RISE.length;frame++) for(int y=0;y<256;y++) {
            // Torso and head rise, hips stretch gently, feet keep their exact original pixels.
            float weight=Math.max(0,Math.min(1,(FOOT_ANCHOR-y)/(float)(FOOT_ANCHOR-HIP)));
            int sourceY=y+Math.round(RISE[frame]*weight);
            for(int x=0;x<256;x++) base.setRGB(frame*256+x,y,reference.getRGB(x,sourceY));
        }
        ImageIO.write(base,"png",root.resolve("app/src/main/assets/sprites/mozart.png").toFile());
        BufferedImage repaired=ImageIO.read(root.resolve("art/mozart-violin-repaired-generated.png").toFile());
        BufferedImage patch=new BufferedImage(4,13,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<13;y++) for(int x=0;x<4;x++) {
            int gx=(int)((40+x+.5f)*repaired.getWidth()/256f);
            int gy=(int)((171+y+.5f)*repaired.getHeight()/256f);
            int pixel=repaired.getRGB(gx,gy);
            if((pixel>>>24)>=32) throw new IllegalStateException("Generated cleanup patch is not transparent");
            patch.setRGB(x,y,0); // Normalize the generator's transparent matte for hard-alpha pixel art.
        }
        ImageIO.write(patch,"png",root.resolve("art/mozart-violin-cleanup-patch.png").toFile());
        BufferedImage extra=ImageIO.read(root.resolve("art/mozart-extra.png").toFile());
        Graphics2D g=extra.createGraphics();g.setComposite(AlphaComposite.Src);
        for(int frame:new int[]{1,3}) g.drawImage(patch,frame*256+40,171,null);
        g.dispose();
        ImageIO.write(extra,"png",root.resolve("art/mozart-extra.png").toFile());
        for(int sheet=0;sheet<4;sheet++) ImageIO.write(extra.getSubimage(0,sheet*1280,1024,1280),"png",
                root.resolve("app/src/main/assets/sprites/mozart-extra-"+sheet+".png").toFile());
        System.out.println("Idle: 0/2/4/2-pixel torso breathing, feet fixed from row 216; violin: generated transparent patch applied to frames 1 and 3.");
    }
}
