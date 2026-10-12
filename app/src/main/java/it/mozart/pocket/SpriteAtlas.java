package it.mozart.pocket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.io.IOException;

final class SpriteAtlas {
    private static SpriteAtlas instance;
    private final Bitmap bitmap;
    private final Bitmap walkBitmap;
    private final Bitmap[] extras = new Bitmap[4];
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final int cellWidth, cellHeight;

    static synchronized SpriteAtlas get(Context c) {
        if (instance == null) instance = new SpriteAtlas(c.getApplicationContext());
        return instance;
    }

    private SpriteAtlas(Context c) {
        try (java.io.InputStream input = c.getAssets().open("sprites/mozart.png")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null || bitmap.getWidth() % 4 != 0 || bitmap.getHeight() % 6 != 0)
                throw new IllegalStateException("Atlante Mozart non valido");
        } catch (IOException e) { throw new IllegalStateException("Sprite di Mozart mancanti", e); }
        cellWidth = bitmap.getWidth() / 4;
        cellHeight = bitmap.getHeight() / 6;
        try (java.io.InputStream input = c.getAssets().open("sprites/mozart-walk.png")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            walkBitmap = BitmapFactory.decodeStream(input, null, options);
            if (walkBitmap == null || walkBitmap.getWidth() != cellWidth * 4 || walkBitmap.getHeight() != cellHeight * 2)
                throw new IllegalStateException("Camminata Mozart non valida");
        } catch (IOException e) { throw new IllegalStateException("Fotogrammi della camminata mancanti", e); }
        for (int i = 0; i < extras.length; i++) {
            try (java.io.InputStream input = c.getAssets().open("sprites/mozart-extra-" + i + ".png")) {
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
                extras[i] = BitmapFactory.decodeStream(input, null, options);
                if (extras[i] == null || extras[i].getWidth() != cellWidth * 4 || extras[i].getHeight() != cellHeight * 5)
                    throw new IllegalStateException("Nuove animazioni Mozart non valide");
            } catch (IOException e) { throw new IllegalStateException("Nuove animazioni Mozart mancanti", e); }
        }
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
    }

    void draw(Canvas canvas, MotionEngine.Pose pose, int frame, float x, float y, float size, boolean left) {
        int col = Math.floorMod(frame, 4);
        int row = pose == MotionEngine.Pose.WALK ? Math.floorMod(frame, pose.frameCount) / 4
                : pose.row >= 6 ? (pose.row - 6) % 5 : pose.row;
        Bitmap sheet = pose == MotionEngine.Pose.WALK ? walkBitmap
                : pose.row >= 6 ? extras[(pose.row - 6) / 5] : bitmap;
        source.set(col * cellWidth, row * cellHeight, (col + 1) * cellWidth, (row + 1) * cellHeight);
        destination.set(0, 0, size, size);
        canvas.save();
        canvas.translate(Math.round(x), Math.round(y));
        if (left) {
            canvas.translate(size, 0);
            canvas.scale(-1, 1);
        }
        canvas.drawBitmap(sheet, source, destination, paint);
        canvas.restore();
    }
}
