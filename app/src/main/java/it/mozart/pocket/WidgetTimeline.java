package it.mozart.pocket;

import java.time.LocalDate;

/** Prepares random motion ahead of playback; the engine continues across batches. */
final class WidgetTimeline {
    static final int SECONDS = 45;
    static final class Frame {
        final MotionEngine.Pose pose;
        final int sprite;
        final float x, y, size;
        final boolean left;
        final int noteAlpha, noteVariant;
        final float noteX, noteY, noteSize;
        Frame(MotionEngine engine, float margin) {
            pose = engine.pose(); sprite = engine.frame(); x = engine.x() + margin; y = engine.y() + margin;
            size = engine.size(); left = engine.facingLeft();
            noteAlpha = engine.noteAlpha(); noteVariant = engine.noteVariant();
            noteX = engine.noteX() + margin; noteY = engine.noteY() + margin; noteSize = engine.noteSize();
        }
        Frame(Frame source, boolean facingLeft, int alpha) {
            pose = source.pose; sprite = source.sprite; x = source.x; y = source.y; size = source.size;
            left = facingLeft; noteAlpha = alpha; noteVariant = source.noteVariant;
            noteX = source.noteX; noteY = source.noteY; noteSize = source.noteSize;
        }
    }
    private final MotionEngine engine;
    private final int interval, count;
    private final float margin;

    WidgetTimeline(long seed, float width, float height, float desiredSize,
                   float speed, boolean dates, int intervalMillis) {
        interval = Math.max(50, intervalMillis);
        count = Math.max(2, Math.round(SECONDS * 1000f / interval));
        margin = Math.min(4, Math.min(width, height) / 4);
        engine = new MotionEngine(seed);
        engine.setBounds(width - margin * 2, height - margin * 2, desiredSize);
        engine.setSpeed(speed);
        engine.setSpecialDates(dates);
    }

    int count() { return count; }

    Frame[] next(LocalDate date) {
        engine.setDate(date);
        return frames(engine, margin, count, interval);
    }

    static Frame[] frames(MotionEngine engine, float margin, int count, int interval) {
        Frame[] frames = new Frame[count];
        float delta = interval / 1000f;
        int steps = Math.max(1, (int) Math.ceil(delta / 0.12f));
        for (int i = 0; i < count; i++) {
            frames[i] = new Frame(engine, margin);
            // Preserve the motion engine's pause guard even at the lower native frame rate.
            for (int step = 0; step < steps; step++) engine.update(delta / steps);
        }
        return frames;
    }
}
