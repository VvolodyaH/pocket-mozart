package it.mozart.pocket;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnimationFlowTest {
    @Test public void autonomousPlaybackAlwaysRepeatsIdleWalkIdleActionAndKeepsItsFacing() {
        MotionEngine e = new MotionEngine(41);
        e.setBounds(300,56,123); e.setSpecialDates(false);
        EnumSet<MotionEngine.Pose> seen = EnumSet.noneOf(MotionEngine.Pose.class);
        MotionEngine.Pose previous = e.pose();
        boolean previousLeft = e.facingLeft();
        int phase = 0, cycles = 0, noteFrames = 0, clearFrames = 0;
        for (int i = 0; i < 60000; i++) {
            e.update(.1f); seen.add(e.pose());
            if (e.pose() != previous) {
                switch (phase) {
                    case 0: assertEquals("idle must lead to a walk",MotionEngine.Pose.WALK,e.pose()); break;
                    case 1: assertEquals("walk must lead to idle",MotionEngine.Pose.IDLE,e.pose()); break;
                    case 2: assertTrue("second idle must lead to an activity",e.pose().row >= 2); break;
                    case 3: assertEquals("activity must restart from idle",MotionEngine.Pose.IDLE,e.pose()); cycles++; break;
                }
                phase = (phase + 1) % 4;
            }
            if (!(previous == MotionEngine.Pose.IDLE && e.pose() == MotionEngine.Pose.WALK))
                assertEquals("idle and all activities keep the last walk direction",previousLeft,e.facingLeft());
            if (e.noteAlpha() > 0) noteFrames++; else clearFrames++;
            assertTrue(e.noteX() >= e.x()); assertTrue(e.noteY() >= e.y());
            assertTrue(e.noteX() + e.noteSize() <= e.x() + e.size() + .001f);
            assertTrue(e.noteY() + e.noteSize() <= e.y() + e.size() + .001f);
            previous = e.pose(); previousLeft = e.facingLeft();
        }
        assertTrue(cycles > 100);
        for (MotionEngine.Pose p : MotionEngine.Pose.values()) if (p.row >= 6)
            assertTrue("activity never chosen: " + p,seen.contains(p));
        assertTrue(noteFrames > 100); assertTrue("notes are occasional",clearFrames > noteFrames * 2);
    }

    @Test public void everyPreviewChoiceWaitsForTheCompleteWalkAndBothIdlePauses() {
        for (MotionEngine.Pose next : MotionEngine.Pose.values()) if (next.row >= 6) {
            MotionEngine e = new MotionEngine(5);
            e.setBounds(300,56,123); e.force(MotionEngine.Pose.PIANO,.3f); e.request(next,7);
            List<MotionEngine.Pose> sequence = new ArrayList<>(); sequence.add(e.pose());
            float postWalkIdle = 0;
            for (int i = 0; i < 2000 && e.pose() != next; i++) {
                e.update(.025f);
                if (e.pose() != sequence.get(sequence.size()-1)) sequence.add(e.pose());
                if (sequence.size() == 4 && e.pose() == MotionEngine.Pose.IDLE) postWalkIdle += .025f;
            }
            assertEquals(List.of(MotionEngine.Pose.PIANO,MotionEngine.Pose.IDLE,
                    MotionEngine.Pose.WALK,MotionEngine.Pose.IDLE,next),sequence);
            assertTrue("arrival includes a visible rest",postWalkIdle >= 1-.025f);
        }
    }

    @Test public void rapidPreviewTapsReplaceThePendingActionWithoutChangingTheWalkingTarget() {
        MotionEngine e = new MotionEngine(9);
        e.setBounds(300,56,123); e.request(MotionEngine.Pose.VIOLIN,7);
        for (int i = 0; i < 100 && e.pose() != MotionEngine.Pose.WALK; i++) e.update(.1f);
        assertEquals(MotionEngine.Pose.WALK,e.pose());
        MotionEngine unchanged = e.copy();
        e.request(MotionEngine.Pose.FLUTE,7); e.request(MotionEngine.Pose.MUSIC_BOX,7);
        while (e.pose() == MotionEngine.Pose.WALK) {
            e.update(.1f); unchanged.update(.1f);
            assertEquals(unchanged.x(),e.x(),0); assertEquals(unchanged.y(),e.y(),0);
            assertEquals(unchanged.facingLeft(),e.facingLeft());
        }
        assertEquals(MotionEngine.Pose.IDLE,e.pose());
        for (int i = 0; i < 100; i++) {
            e.update(.1f);
            assertNotEquals(MotionEngine.Pose.FLUTE,e.pose());
            assertNotEquals(MotionEngine.Pose.VIOLIN,e.pose());
            if (e.pose() == MotionEngine.Pose.MUSIC_BOX) return;
        }
        fail("latest activity did not start after arrival and idle");
    }

    @Test public void birthdayAlsoWaitsForIdleWalkIdle() {
        MotionEngine e = new MotionEngine(4);
        e.setBounds(300,56,123); e.setDate(LocalDate.of(2027,1,27));
        List<MotionEngine.Pose> sequence = new ArrayList<>(); sequence.add(e.pose());
        for (int i = 0; i < 1000 && e.pose() != MotionEngine.Pose.BIRTHDAY; i++) {
            e.update(.1f);
            if (e.pose() != sequence.get(sequence.size()-1)) sequence.add(e.pose());
        }
        assertEquals(List.of(MotionEngine.Pose.IDLE,MotionEngine.Pose.WALK,
                MotionEngine.Pose.IDLE,MotionEngine.Pose.BIRTHDAY),sequence);
    }

    @Test public void smallestAreaStillCompletesTheCycleWithAnAnimatedWalkInPlace() {
        MotionEngine e = new MotionEngine(7);
        e.setBounds(40,40,123); e.setSpecialDates(false);
        EnumSet<MotionEngine.Pose> seen = EnumSet.noneOf(MotionEngine.Pose.class);
        java.util.Set<Integer> feet = new java.util.HashSet<>();
        for (int i = 0; i < 1500; i++) {
            e.update(.05f); seen.add(e.pose());
            assertEquals(0,e.x(),0); assertEquals(0,e.y(),0);
            if (e.pose() == MotionEngine.Pose.WALK) feet.add(e.frame());
        }
        assertTrue(seen.contains(MotionEngine.Pose.IDLE)); assertTrue(seen.contains(MotionEngine.Pose.WALK));
        assertTrue(seen.stream().anyMatch(p -> p.row >= 2)); assertEquals(8,feet.size());
    }

    @Test public void previewIdleAndWalkButtonsCannotInsertAnExtraWalkBetweenArrivalAndAction() {
        MotionEngine e = new MotionEngine(42);
        e.setBounds(300,56,123); e.setSpecialDates(false); e.request(MotionEngine.Pose.WALK,7);
        for (int i = 0; i < 1000 && e.pose() != MotionEngine.Pose.WALK; i++) e.update(.1f);
        while (e.pose() == MotionEngine.Pose.WALK) e.update(.1f);
        assertEquals(MotionEngine.Pose.IDLE,e.pose());
        e.request(MotionEngine.Pose.IDLE,2); e.request(MotionEngine.Pose.WALK,7);
        for (int i = 0; i < 100 && e.pose() == MotionEngine.Pose.IDLE; i++) e.update(.1f);
        assertTrue("second idle still leads to an activity",e.pose().row >= 2);
    }

    @Test public void nativeFrameSamplingNeverDropsAWalkEvenWhenOnlyOnePixelOfTravelFits() {
        for (int interval : new int[]{125,83}) for (float[] bounds : new float[][]{{40,41},{40,40},{300,56}}) {
            WidgetTimeline timeline = new WidgetTimeline(8,bounds[0],bounds[1],123,30,false,interval);
            MotionEngine.Pose previous = MotionEngine.Pose.IDLE;
            int phase = 0, completed = 0;
            for (int batch = 0; batch < 3; batch++) for (WidgetTimeline.Frame frame : timeline.next(LocalDate.of(2026,10,11))) {
                if (frame.pose == previous) continue;
                switch (phase) {
                    case 0: assertEquals("walk must be visible in native frames",MotionEngine.Pose.WALK,frame.pose); break;
                    case 1: assertEquals(MotionEngine.Pose.IDLE,frame.pose); break;
                    case 2: assertTrue(frame.pose.row >= 2); break;
                    case 3: assertEquals(MotionEngine.Pose.IDLE,frame.pose); completed++; break;
                }
                phase = (phase + 1) % 4; previous = frame.pose;
            }
            assertTrue(completed > 3);
        }
    }
}
