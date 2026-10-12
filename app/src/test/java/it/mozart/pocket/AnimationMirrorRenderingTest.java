package it.mozart.pocket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,35},qualifiers="mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AnimationMirrorRenderingTest {
    private static MotionEngine facing(long seed,boolean left) {
        MotionEngine e=new MotionEngine(seed);
        e.setBounds(400,300,100); e.setSpecialDates(false);
        for(int i=0;i<100 && e.pose()!=MotionEngine.Pose.WALK;i++) e.update(.1f);
        assertEquals(left,e.facingLeft());
        // Expand the area without recentering: whole-view mirroring would move the character.
        e.setBounds(600,300,256);
        return e;
    }

    @Test public void nativeWidgetMirrorsEveryFrameInsideTheSameOffCentreCell() {
        Context c=RuntimeEnvironment.getApplication();
        MotionEngine right=facing(42,false),left=facing(4096,true);
        for(MotionEngine.Pose pose:MotionEngine.Pose.values()) {
            right.force(pose,7); left.force(pose,7);
            WidgetTimeline.Frame rightFrame=new WidgetTimeline.Frame(right,4),leftFrame=new WidgetTimeline.Frame(left,4);
            assertEquals(rightFrame.x,leftFrame.x,0); assertEquals(rightFrame.y,leftFrame.y,0);
            for(int frame=0;frame<pose.frameCount;frame++) {
                RemoteViews r=NativeWidgetRenderer.frame(c,rightFrame,608,308);
                RemoteViews l=NativeWidgetRenderer.frame(c,leftFrame,608,308);
                // Exercise every source frame with the renderer's actual direction actions.
                r.setImageViewResource(R.id.widget_frame,PixelSprites.drawable(pose,frame,false));
                l.setImageViewResource(R.id.widget_frame,PixelSprites.drawable(pose,frame,false));
                ImageView ri=image(c,r),li=image(c,l);
                assertEquals(ri.getPaddingLeft(),li.getPaddingLeft());
                assertEquals(ri.getPaddingRight(),li.getPaddingRight());
                assertEquals(View.LAYOUT_DIRECTION_RTL,li.getDrawable().getLayoutDirection());
                assertEquals(255,li.getImageAlpha());
                int[] rp=cell(ri,rightFrame),lp=cell(li,leftFrame);
                mirrored(pose+" frame "+frame,rp,lp);
            }
        }
    }

    @Test public void liveAppPreviewAlsoMirrorsEveryPoseAndFrame() {
        SpriteAtlas atlas=SpriteAtlas.get(RuntimeEnvironment.getApplication());
        for(MotionEngine.Pose pose:MotionEngine.Pose.values()) for(int frame=0;frame<pose.frameCount;frame++) {
            Bitmap right=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
            Bitmap left=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
            atlas.draw(new Canvas(right),pose,frame,0,0,256,false);
            atlas.draw(new Canvas(left),pose,frame,0,0,256,true);
            int[] rp=new int[256*256],lp=new int[256*256];
            right.getPixels(rp,0,256,0,0,256,256); left.getPixels(lp,0,256,0,0,256,256);
            mirrored("preview "+pose+" frame "+frame,rp,lp);
            right.recycle(); left.recycle();
        }
    }

    private static ImageView image(Context c,RemoteViews remote) {
        View root=remote.apply(c,new FrameLayout(c));
        root.measure(View.MeasureSpec.makeMeasureSpec(608,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(308,View.MeasureSpec.EXACTLY));
        root.layout(0,0,608,308);
        return root.findViewById(R.id.widget_frame);
    }
    private static int[] cell(ImageView image,WidgetTimeline.Frame frame) {
        Bitmap pixels=Bitmap.createBitmap(608,308,Bitmap.Config.ARGB_8888);
        image.draw(new Canvas(pixels));
        int[] result=new int[256*256];
        pixels.getPixels(result,0,256,Math.round(frame.x),Math.round(frame.y),256,256);
        pixels.recycle(); return result;
    }
    private static void mirrored(String label,int[] right,int[] left) {
        boolean visible=false;
        for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
            int expected=right[y*256+255-x];
            if(expected!=left[y*256+x]) fail(label+" is not mirrored at "+x+","+y);
            visible |= expected>>>24!=0;
        }
        assertTrue(label+" must actually render",visible);
    }
}
