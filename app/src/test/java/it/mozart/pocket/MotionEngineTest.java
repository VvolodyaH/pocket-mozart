package it.mozart.pocket;

import org.junit.Test;
import java.time.LocalDate;
import java.util.EnumSet;
import static org.junit.Assert.*;

public class MotionEngineTest {
    @Test public void spriteStaysInsideAreaDuringWalkingAndRepeatedResizing() {
        MotionEngine engine = new MotionEngine(42);
        engine.setSpecialDates(false);
        float[][] areas = {{480, 320, 150}, {80, 80, 200}, {120, 400, 90}, {900, 100, 120}, {240, 200, 145}};
        for (int i = 0; i < 14000; i++) {
            float[] area = areas[(i / 37) % areas.length];
            engine.setBounds(area[0], area[1], area[2]);
            engine.update(1f / 24);
            assertTrue("left edge", engine.x() >= 0);
            assertTrue("top edge", engine.y() >= 0);
            assertTrue("right edge", engine.x() + engine.size() <= area[0] + 0.001f);
            assertTrue("bottom edge", engine.y() + engine.size() <= area[1] + 0.001f);
            assertTrue(engine.frame() >= 0 && engine.frame() < engine.pose().frameCount);
        }
    }

    @Test public void aTinyWidgetStillHasAutonomousActivities() {
        MotionEngine engine = new MotionEngine(7);
        engine.setBounds(80, 80, 200);
        engine.setSpecialDates(false);
        EnumSet<MotionEngine.Pose> seen = EnumSet.noneOf(MotionEngine.Pose.class);
        for (int i = 0; i < 15000; i++) {
            engine.update(0.1f);
            seen.add(engine.pose());
        }
        assertTrue(seen.contains(MotionEngine.Pose.IDLE));
        assertTrue(seen.contains(MotionEngine.Pose.SCORE));
        assertTrue(seen.contains(MotionEngine.Pose.PIANO));
        assertTrue(seen.contains(MotionEngine.Pose.BOW));
        assertTrue("a full cell walks in place",seen.contains(MotionEngine.Pose.WALK));
        assertFalse(seen.contains(MotionEngine.Pose.BIRTHDAY));
    }

    @Test public void birthdayCelebrationUsesTheDevicesCalendarDate() {
        MotionEngine engine = new MotionEngine(3);
        engine.setBounds(300, 200, 110);
        engine.setDate(LocalDate.of(2027, 1, 27));
        for (int i = 0; i < 1000 && engine.pose() != MotionEngine.Pose.BIRTHDAY; i++) engine.update(0.1f);
        assertEquals(MotionEngine.Pose.BIRTHDAY, engine.pose());
        assertEquals(5, engine.pose().row);
    }

    @Test public void datesToggleSuppressesBirthdayEvenOnJanuary27() {
        MotionEngine engine = new MotionEngine(11);
        engine.setBounds(300, 200, 110);
        engine.setDate(LocalDate.of(2027, 1, 27));
        engine.setSpecialDates(false);
        for (int i = 0; i < 5000; i++) {
            engine.update(0.1f);
            assertNotEquals(MotionEngine.Pose.BIRTHDAY, engine.pose());
        }
    }

    @Test public void lifecyclePauseNeverCreatesAHugeMovementJump() {
        MotionEngine engine = new MotionEngine(0);
        engine.setBounds(500, 300, 100);
        engine.setSpeed(30);
        engine.force(MotionEngine.Pose.WALK, 20);
        float x = engine.x(), y = engine.y();
        engine.update(3600);
        assertTrue(Math.hypot(engine.x() - x, engine.y() - y) <= 30 * 0.12 + 0.001);
        engine.update(Float.NaN);
        engine.update(-1);
        assertTrue(Float.isFinite(engine.x()));
        assertTrue(Float.isFinite(engine.y()));
    }

    @Test public void resizingBelowCharacterSizeShrinksItsEntireCell() {
        MotionEngine engine = new MotionEngine(4);
        engine.setBounds(600, 600, 180);
        engine.force(MotionEngine.Pose.WALK, 100);
        for (int i = 0; i < 40; i++) engine.update(0.1f);
        engine.setBounds(75, 50, 180);
        assertEquals(50, engine.size(), 0);
        assertTrue(engine.x() + engine.size() <= 75);
        assertTrue(engine.y() + engine.size() <= 50);
    }

    @Test public void historicalDatesAreSpecificAndRepeatEveryYear() {
        assertEquals(MotionEngine.Occasion.MAGIC_FLUTE, MotionEngine.occasion(LocalDate.of(2032, 9, 30)));
        assertEquals(MotionEngine.Occasion.DON_GIOVANNI, MotionEngine.occasion(LocalDate.of(2026, 10, 29)));
        assertEquals(MotionEngine.Occasion.REMEMBRANCE, MotionEngine.occasion(LocalDate.of(2026, 12, 5)));
        assertEquals(MotionEngine.Occasion.NONE, MotionEngine.occasion(LocalDate.of(2026, 1, 26)));
        assertEquals(MotionEngine.Occasion.NONE, MotionEngine.occasion(LocalDate.of(2028, 2, 29)));
    }

    @Test public void walkingUsesAllEightFootPosesAndTracksDistanceRatherThanElapsedTime() {
        MotionEngine slow = new MotionEngine(8), fast = new MotionEngine(8);
        for (MotionEngine engine : new MotionEngine[]{slow, fast}) {
            engine.setBounds(500, 300, 100);
            engine.setSpecialDates(false);
            engine.force(MotionEngine.Pose.WALK, 100);
        }
        slow.setSpeed(30); fast.setSpeed(60);
        java.util.Set<Integer> frames = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            slow.update(0.05f); fast.update(0.025f);
            assertEquals(slow.x(), fast.x(), 0.001f);
            assertEquals(slow.y(), fast.y(), 0.001f);
            assertEquals("same travelled distance, same foot pose", slow.frame(), fast.frame());
            frames.add(slow.frame());
        }
        assertEquals(8, frames.size());
    }
}
