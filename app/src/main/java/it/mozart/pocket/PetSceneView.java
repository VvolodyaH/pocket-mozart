package it.mozart.pocket;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import java.time.LocalDate;

public final class PetSceneView extends View {
    private final MotionEngine engine = new MotionEngine(System.nanoTime());
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF borderRect = new RectF();
    private final SpriteAtlas atlas;
    private final Drawable[] notes = new Drawable[3];
    private boolean running, preview;
    private long previous;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running || !isAttachedToWindow()) return;
            long now = SystemClock.uptimeMillis();
            if (previous != 0) engine.update((now - previous) / 1000f);
            previous = now;
            engine.setDate(LocalDate.now());
            invalidate();
            postDelayed(this, 1000 / Prefs.fps(getContext()));
        }
    };

    public PetSceneView(Context c) {
        super(c);
        atlas = SpriteAtlas.get(c);
        for (int i = 0; i < notes.length; i++) notes[i] = c.getDrawable(PixelSprites.note(i)).mutate();
        setBackgroundColor(Color.TRANSPARENT);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        border.setColor(0x779B5B64);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1));
        border.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(5)}, 0));
    }

    public void setPreview(boolean value) { preview = value; }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void refresh() {
        float tempo = new float[]{0.65f, 1f, 1.5f}[Math.max(0, Math.min(2, Prefs.tempo(getContext())))];
        engine.setSpeed(dp(30) * tempo);
        engine.setSpecialDates(Prefs.dates(getContext()));
        engine.setBounds(Math.max(1, getWidth() - dp(8)), Math.max(1, getHeight() - dp(8)),
                dp(Prefs.size(getContext()) * 1.23f));
        invalidate();
    }

    public void demo(MotionEngine.Pose pose) { engine.request(pose, 7); invalidate(); }
    public void setRunning(boolean value) {
        running = value;
        previous = 0;
        removeCallbacks(tick);
        if (value && isAttachedToWindow()) post(tick);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { refresh(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); setRunning(true); }
    @Override protected void onDetachedFromWindow() { setRunning(false); super.onDetachedFromWindow(); }
    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        // View's constructor can call this before our fields are initialized.
        if (engine != null) setRunning(visibility == VISIBLE);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!preview && Prefs.border(getContext())) {
            borderRect.set(dp(1), dp(1), getWidth() - dp(1), getHeight() - dp(1));
            canvas.drawRoundRect(borderRect, dp(12), dp(12), border);
        }
        canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        atlas.draw(canvas, engine.pose(), engine.frame(), engine.x() + dp(4), engine.y() + dp(4),
                engine.size(), engine.facingLeft());
        if (engine.noteAlpha() > 0) {
            Drawable note = notes[engine.noteVariant()];
            float size = engine.noteSize();
            float width = size * note.getIntrinsicWidth() / note.getIntrinsicHeight();
            float x = engine.noteX() + dp(4) + (size - width) / 2;
            float y = engine.noteY() + dp(4);
            note.setBounds(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + size));
            note.setAlpha(engine.noteAlpha()); note.draw(canvas);
        }
        canvas.restore();
    }
}
