package it.mozart.pocket;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class IdleBreathingTest {
    @Test public void idleCompletesABreathDuringAShortPauseWithoutMovingItsCell() {
        MotionEngine engine = new MotionEngine(8);
        engine.setBounds(300,56,123); engine.force(MotionEngine.Pose.IDLE,5);
        float x = engine.x(),y = engine.y();
        List<Integer> frames = new ArrayList<>(); frames.add(engine.frame());
        for(int i=0;i<36;i++) {
            engine.update(.0625f);
            assertEquals(MotionEngine.Pose.IDLE,engine.pose());
            assertEquals(x,engine.x(),0); assertEquals(y,engine.y(),0);
            if(engine.frame()!=frames.get(frames.size()-1)) frames.add(engine.frame());
        }
        assertEquals(List.of(0,1,2,3,0),frames);
    }
}
