package it.mozart.pocket;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A bounded random route, completely available to the launcher before playback starts. */
final class ResidentTimeline {
    static WidgetTimeline.Frame[] create(long seed, float width, float height, float desiredSize,
            float speed, boolean dates, LocalDate date, int interval, int budget) {
        float margin = Math.min(4, Math.min(width, height) / 4);
        // Daily variety without rewriting the numbered frames during normal host reapplication.
        MotionEngine engine = new MotionEngine(seed ^ (date.toEpochDay() * 0x9E3779B97F4A7C15L));
        engine.setBounds(width - margin * 2, height - margin * 2, desiredSize);
        engine.setSpeed(speed); engine.setSpecialDates(dates); engine.setDate(date);
        float homeX = engine.x(), homeY = engine.y();
        // Keep enough time for a real return walk, including very large / thin widgets.
        float radius = speed * budget * interval / 1000f * .08f;
        List<WidgetTimeline.Frame> prefix = new ArrayList<>();
        List<WidgetTimeline.Frame> best = null;
        while (prefix.size() < budget) {
            MotionEngine closing = engine.copy();
            List<WidgetTimeline.Frame> tail = cycle(closing, margin, interval,
                    budget - prefix.size(), homeX, homeY, radius, true);
            if (tail == null) break;
            best = new ArrayList<>(prefix);
            best.addAll(tail);
            List<WidgetTimeline.Frame> next = cycle(engine, margin, interval,
                    budget - prefix.size(), homeX, homeY, radius, false);
            if (next == null) break;
            prefix.addAll(next);
        }
        if (best == null) throw new IllegalArgumentException("Insufficient resident frame budget");
        boolean facing = best.get(best.size() - 1).left;
        boolean openingIdle = true;
        for (int i = 0; i < best.size(); i++) {
            WidgetTimeline.Frame f = best.get(i);
            openingIdle &= f.pose == MotionEngine.Pose.IDLE;
            // The body never fades. Only let a musical effect finish before the route wraps.
            int alpha = Math.min(f.noteAlpha, Math.round(255 * Math.min(1f,
                    (best.size() - 1 - i) * interval / 650f)));
            if (openingIdle || alpha != f.noteAlpha)
                best.set(i, new WidgetTimeline.Frame(f, openingIdle ? facing : f.left, alpha));
        }
        return best.toArray(new WidgetTimeline.Frame[0]);
    }

    private static List<WidgetTimeline.Frame> cycle(MotionEngine engine, float margin, int interval,
            int limit, float homeX, float homeY, float radius, boolean close) {
        List<WidgetTimeline.Frame> frames = new ArrayList<>();
        int steps = Math.max(1, (int) Math.ceil(interval / 120f));
        float dt = interval / 1000f / steps;
        boolean acted = false;
        while (frames.size() < limit) {
            frames.add(new WidgetTimeline.Frame(engine, margin));
            for (int step = 0; step < steps; step++) {
                MotionEngine.Pose previous = engine.pose();
                engine.update(dt);
                if (previous != MotionEngine.Pose.WALK && engine.pose() == MotionEngine.Pose.WALK) {
                    if (close) engine.retargetWalk(homeX, homeY);
                    else engine.constrainWalkTo(homeX, homeY, radius);
                }
                acted |= engine.pose().row >= 2;
                // Stop exactly at the next idle's start: no reset of the breathing phase at wrap.
                if (acted && engine.pose() == MotionEngine.Pose.IDLE) return frames;
            }
        }
        return null;
    }
}
