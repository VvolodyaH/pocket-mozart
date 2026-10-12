import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
/** Contact sheet of existing sprites for visual inspection. No character editing. */
public class PreviewCatalog {
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args.length==0?".":args[0]);
  String[] names={"Riposo","Passeggiata","Spartito","Pianoforte","Inchino","Compleanno","Violino","Direttore","Flauto","Composizione","Canticchia","Un tè","Lettura","Lettera","Orologio","Parrucca","Minuetto","Si stiracchia","Sbadiglio","Sonnellino","Saluto","Tricorno","Una rosa","Carillon","In viaggio","Applauso"};
  BufferedImage base=ImageIO.read(root.resolve("app/src/main/assets/sprites/mozart.png").toFile());
  BufferedImage walk=ImageIO.read(root.resolve("app/src/main/assets/sprites/mozart-walk.png").toFile());
  BufferedImage extra=ImageIO.read(root.resolve("art/mozart-extra.png").toFile());
  BufferedImage catalog=new BufferedImage(1536,1400,BufferedImage.TYPE_INT_RGB);
  Graphics2D g=catalog.createGraphics();
  g.setColor(new Color(248,244,239));g.fillRect(0,0,1536,1400);g.setFont(new Font("SansSerif",Font.PLAIN,17));
  for(int i=0;i<26;i++) {
   int x=i%6*256,y=i/6*280;
   BufferedImage sheet=i==1?walk:i<6?base:extra;
   int row=i==1?0:i<6?i:i-6;
   g.drawImage(sheet,x,y,x+256,y+256,0,row*256,256,(row+1)*256,null);
   g.setColor(new Color(60,45,52));g.drawString(names[i],x+22,y+270);
  }
  g.dispose();ImageIO.write(catalog,"png",root.resolve("art/catalogo.png").toFile());
 }
}
