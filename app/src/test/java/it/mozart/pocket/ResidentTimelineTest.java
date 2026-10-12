package it.mozart.pocket;

import java.time.LocalDate;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResidentTimelineTest {
    @Test public void completeRoutesRespectCycleBoundsFacingAndContinuousWrapAtBothRates() {
        for (int interval : new int[]{83, 125}) for (int budget : new int[]{400, 800, 1600})
            for (int[] bounds : new int[][]{{300, 56}, {40, 40}, {64, 240}, {2000, 2000}})
                for (int seed = 1; seed <= 8; seed++) {
                    WidgetTimeline.Frame[] frames = ResidentTimeline.create(seed, bounds[0], bounds[1], 123,
                            30, false, LocalDate.of(2026, 10, 12), interval, budget);
                    assertTrue(frames.length <= budget);
                    int stage = 0;
                    MotionEngine.Pose prior = MotionEngine.Pose.IDLE;
                    for (int i = 0; i < frames.length; i++) {
                        WidgetTimeline.Frame f = frames[i], next = frames[(i + 1) % frames.length];
                        assertTrue(f.x >= 0 && f.y >= 0 && f.x + f.size <= bounds[0] + .001
                                && f.y + f.size <= bounds[1] + .001);
                        assertTrue("including wrap, every step obeys walking speed", Math.hypot(next.x - f.x, next.y - f.y)
                                <= 30 * interval / 1000f + .001);
                        if (f.pose != prior) {
                            stage = (stage + 1) % 4;
                            assertTrue(stage == 0 || stage == 2 ? f.pose == MotionEngine.Pose.IDLE
                                    : stage == 1 ? f.pose == MotionEngine.Pose.WALK : f.pose.row >= 2);
                        }
                        if (next.pose != MotionEngine.Pose.WALK) assertEquals(f.left, next.left);
                        if (f.pose == MotionEngine.Pose.WALK && next.x != f.x) assertEquals(next.x < f.x, f.left);
                        prior = f.pose;
                    }
                    assertEquals(3, stage);
                    assertEquals(0, frames[frames.length - 1].noteAlpha);
                    assertEquals(0, frames[0].noteAlpha);
                    assertEquals(0, frames[0].sprite);
                }
    }

    @Test public void routesAreStableForReapplyButDifferentSeedsUseTheFullActivityPool() {
        EnumSet<MotionEngine.Pose> seen = EnumSet.noneOf(MotionEngine.Pose.class);
        boolean note = false;
        for (int seed = 1; seed <= 50; seed++) {
            var a = ResidentTimeline.create(seed, 300, 56, 123, 30, false, LocalDate.of(2000, 1, 1), 125, 800);
            var b = ResidentTimeline.create(seed, 300, 56, 123, 30, false, LocalDate.of(2000, 1, 1), 125, 800);
            assertEquals(a.length, b.length);
            for (int i = 0; i < a.length; i++) {
                seen.add(a[i].pose); note |= a[i].noteAlpha > 0;
                assertEquals(a[i].pose, b[i].pose); assertEquals(a[i].sprite, b[i].sprite);
                assertEquals(a[i].x, b[i].x, 0); assertEquals(a[i].y, b[i].y, 0);
                assertEquals(a[i].left, b[i].left); assertEquals(a[i].noteAlpha, b[i].noteAlpha);
            }
        }
        assertTrue(note);
        assertEquals(MotionEngine.Pose.values().length - 1, seen.size());
        var birthday = ResidentTimeline.create(8, 300, 56, 123, 30, true, LocalDate.of(2027, 1, 27), 125, 800);
        boolean party = false;
        for (var frame : birthday) party |= frame.pose == MotionEngine.Pose.BIRTHDAY;
        assertTrue(party);
    }
}
