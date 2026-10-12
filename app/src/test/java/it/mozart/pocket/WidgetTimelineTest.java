package it.mozart.pocket;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetTimelineTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);
    private static WidgetTimeline timeline(long seed, float w, float h, int interval) {
        return new WidgetTimeline(seed, w, h, 123, 30, true, interval);
    }

    @Test public void randomChoicesAreReproducibleButIndependentForDifferentSeeds() {
        WidgetTimeline.Frame[] a = timeline(6, 350, 240, 125).next(DAY);
        WidgetTimeline.Frame[] b = timeline(6, 350, 240, 125).next(DAY);
        WidgetTimeline.Frame[] c = timeline(7, 350, 240, 125).next(DAY);
        boolean different = false;
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i].x, b[i].x, 0);
            assertEquals(a[i].y, b[i].y, 0);
            assertEquals(a[i].pose, b[i].pose);
            assertEquals(a[i].sprite, b[i].sprite);
            different |= a[i].pose != c[i].pose || a[i].x != c[i].x || a[i].y != c[i].y;
        }
        assertTrue(different);
    }

    @Test public void batchesContinueWithoutTeleportingOrRepeatingTheSameLoop() {
        WidgetTimeline timeline = timeline(15, 350, 240, 125);
        WidgetTimeline.Frame[] previous = timeline.next(DAY);
        for (int batch = 0; batch < 5; batch++) {
            WidgetTimeline.Frame[] next = timeline.next(DAY);
            WidgetTimeline.Frame last = previous[previous.length - 1];
            assertTrue(Math.hypot(next[0].x - last.x, next[0].y - last.y) <= 30 * 0.125 + 0.001);
            boolean different = false;
            for (int i = 0; i < next.length; i++) different |= next[i].pose != previous[i].pose
                    || next[i].x != previous[i].x || next[i].y != previous[i].y;
            assertTrue(different);
            previous = next;
        }
    }

    @Test public void allSpritesIncludingPropsStayInsideSmallAndLargeWidgetsAtBothRates() {
        for (int interval : new int[]{83, 125}) for (float[] bounds : new float[][]{{40, 40}, {65, 200}, {500, 250}}) {
            WidgetTimeline timeline = timeline(9, bounds[0], bounds[1], interval);
            for (int batch = 0; batch < 3; batch++) for (WidgetTimeline.Frame frame : timeline.next(DAY)) {
                assertTrue(frame.x >= 0 && frame.y >= 0);
                assertTrue(frame.x + frame.size <= bounds[0] + 0.001);
                assertTrue(frame.y + frame.size <= bounds[1] + 0.001);
                assertTrue(frame.sprite >= 0 && frame.sprite < frame.pose.frameCount);
            }
        }
    }

    @Test public void walkingReallyChangesFootFramesWhileMoving() {
        WidgetTimeline timeline = timeline(8, 500, 300, 125);
        Set<Integer> feet = new HashSet<>();
        int moves = 0;
        for (int batch = 0; batch < 3; batch++) {
            WidgetTimeline.Frame[] frames = timeline.next(DAY);
            for (int i = 1; i < frames.length; i++) if (frames[i].pose == MotionEngine.Pose.WALK
                    && Math.hypot(frames[i].x - frames[i-1].x, frames[i].y - frames[i-1].y) > 0.1) {
                feet.add(frames[i].sprite); moves++;
            }
        }
        assertTrue(moves > 40);
        assertEquals(8, feet.size());
    }

    @Test public void birthdayIsSelectedByTheDateOfTheNextBatchAndCanBeDisabled() {
        WidgetTimeline timeline = timeline(2, 250, 250, 125);
        assertFalse(hasBirthday(timeline.next(DAY)));
        assertTrue(hasBirthday(timeline.next(LocalDate.of(2027, 1, 27))));
        WidgetTimeline disabled = new WidgetTimeline(2, 250, 250, 123, 30, false, 125);
        assertFalse(hasBirthday(disabled.next(LocalDate.of(2027, 1, 27))));
    }

    private static boolean hasBirthday(WidgetTimeline.Frame[] frames) {
        for (WidgetTimeline.Frame frame : frames) if (frame.pose == MotionEngine.Pose.BIRTHDAY) return true;
        return false;
    }
}
