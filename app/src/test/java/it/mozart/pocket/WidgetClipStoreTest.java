package it.mozart.pocket;

import java.time.LocalDate;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetClipStoreTest {
    private static WidgetClipStore row(int interval) {
        return new WidgetClipStore(8, 300, 56, 123, 30, true, interval, LocalDate.of(2026, 10, 11));
    }

    @Test public void rowMotionIsContinuousAcrossClipsAtBothFrameRates() {
        for (int interval : new int[]{125, 83}) {
            WidgetClipStore clips = row(interval);
            WidgetTimeline.Frame previous = null;
            int moving = 0;
            for (int i = 0; i < 100; i++) for (WidgetTimeline.Frame frame : clips.get(i)) {
                assertTrue(frame.x >= 4 && frame.y >= 4);
                assertTrue(frame.x + frame.size <= 296.001);
                assertTrue(frame.y + frame.size <= 52.001);
                if (previous != null) {
                    double distance = Math.hypot(frame.x - previous.x, frame.y - previous.y);
                    assertTrue("continuous movement in a 3×1 row", distance <= 30 * interval / 1000.0 + 0.001);
                    if (distance > 0.1) moving++;
                }
                previous = frame;
            }
            assertTrue(moving > 100);
        }
    }

    @Test public void randomOrderAndEvictionCannotRewriteClipPositionsOrFootPoses() {
        WidgetClipStore expected = row(125), actual = row(125);
        Random requests = new Random(20);
        for (int i = 0; i < 150; i++) {
            int position = requests.nextInt(200);
            same(expected.get(position), actual.get(position));
        }
        actual.get(500); // Evict the original clip and retain only later checkpoints.
        same(expected.get(0), actual.get(0));
        same(expected.get(88), actual.get(88));
    }

    @Test public void aCheckpointRestoresTheSameTripAfterTheServiceIsRecreated() throws Exception {
        WidgetClipStore original = row(83);
        original.get(500);
        byte[] saved = original.checkpoint();
        assertTrue(saved.length < 4096);
        WidgetClipStore restored = row(83);
        restored.restore(saved);
        for (int position : new int[]{485, 500, 501, 530, 2, 600}) same(original.get(position), restored.get(position));
    }

    @Test public void corruptCheckpointsAreRejectedWithoutChangingTheJourney() throws Exception {
        WidgetClipStore clips = row(125);
        WidgetTimeline.Frame[] before = clips.get(10);
        try {
            clips.restore(new byte[]{1, 2, 3});
            fail("invalid state must be rejected");
        } catch (java.io.IOException expected) { }
        same(before, clips.get(10));
    }

    @Test public void checkpointableRandomMatchesJavaRandomAndCopiesEveryEffect() {
        // Motion choices use java.util.Random's generator; effects use independent entropy.
        Random random = new Random(8);
        MotionEngine engine = new MotionEngine(8);
        engine.setBounds(300, 56, 48); engine.setSpecialDates(false);
        float targetX = random.nextFloat() * (300-48);
        float targetY = random.nextFloat() * (56-48);
        for (int i = 0; i < 31; i++) engine.update(0.1f);
        assertEquals(MotionEngine.Pose.WALK,engine.pose());
        while (engine.pose() == MotionEngine.Pose.WALK) engine.update(.1f);
        assertEquals(targetX,engine.x(),.0001f);
        assertEquals(targetY,engine.y(),.0001f);
        MotionEngine copied = engine.copy();
        for (int i = 0; i < 500; i++) {
            engine.update(0.1f); copied.update(0.1f);
            assertEquals(engine.x(), copied.x(), 0);
            assertEquals(engine.y(), copied.y(), 0);
            assertEquals(engine.pose(), copied.pose());
            assertEquals(engine.frame(), copied.frame());
            assertEquals(engine.noteAlpha(), copied.noteAlpha());
            assertEquals(engine.noteVariant(), copied.noteVariant());
            assertEquals(engine.noteX(), copied.noteX(), 0);
            assertEquals(engine.noteY(), copied.noteY(), 0);
        }
    }

    private static void same(WidgetTimeline.Frame[] expected, WidgetTimeline.Frame[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].x, actual[i].x, 0);
            assertEquals(expected[i].y, actual[i].y, 0);
            assertEquals(expected[i].size, actual[i].size, 0);
            assertEquals(expected[i].pose, actual[i].pose);
            assertEquals(expected[i].sprite, actual[i].sprite);
            assertEquals(expected[i].left, actual[i].left);
            assertEquals(expected[i].noteAlpha, actual[i].noteAlpha);
            assertEquals(expected[i].noteVariant, actual[i].noteVariant);
            assertEquals(expected[i].noteX, actual[i].noteX, 0);
            assertEquals(expected[i].noteY, actual[i].noteY, 0);
            assertEquals(expected[i].noteSize, actual[i].noteSize, 0);
        }
    }
}
