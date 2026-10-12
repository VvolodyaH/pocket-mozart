package it.mozart.pocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Immutable, numbered clips: requesting one never changes any other clip's content. */
final class WidgetClipStore {
    static final int COUNT = 1_000_000;
    private static final int CACHE_SIZE = 64, CHECKPOINT_STEP = 16, CHECKPOINTS = 8;
    private final int interval, frames;
    private final float margin;
    private final TreeMap<Integer, MotionEngine> checkpoints = new TreeMap<>();
    private final LinkedHashMap<Integer, WidgetTimeline.Frame[]> clips = new LinkedHashMap<>(64, 0.75f, true);
    private int revision;

    WidgetClipStore(long seed, float width, float height, float size, float speed, boolean dates, int interval, LocalDate date) {
        this.interval = interval;
        frames = frameCount(interval);
        margin = Math.min(4, Math.min(width, height) / 4);
        MotionEngine engine = new MotionEngine(seed);
        engine.setBounds(width - 2 * margin, height - 2 * margin, size);
        engine.setSpeed(speed); engine.setSpecialDates(dates); engine.setDate(date);
        checkpoints.put(0, engine);
    }

    static int frameCount(int interval) { return Math.max(2, Math.round(4000f / interval)); }
    static int duration(int interval) { return frameCount(interval) * interval; }
    int revision() { return revision; }

    WidgetTimeline.Frame[] get(int index) {
        if (index < 0 || index >= COUNT) throw new IllegalArgumentException("Invalid clip");
        WidgetTimeline.Frame[] cached = clips.get(index);
        if (cached != null) return cached;
        Map.Entry<Integer, MotionEngine> checkpoint = checkpoints.floorEntry(index);
        MotionEngine engine = checkpoint.getValue().copy();
        for (int cursor = checkpoint.getKey(); cursor <= index; cursor++) {
            WidgetTimeline.Frame[] clip = WidgetTimeline.frames(engine, margin, frames, interval);
            clips.put(cursor, clip);
            while (clips.size() > CACHE_SIZE) clips.remove(clips.keySet().iterator().next());
            int next = cursor + 1;
            if (next % CHECKPOINT_STEP == 0 && !checkpoints.containsKey(next)) {
                checkpoints.put(next, engine.copy());
                while (checkpoints.size() > CHECKPOINTS + 1) checkpoints.remove(checkpoints.higherKey(0));
                revision++;
            }
        }
        return clips.get(index);
    }

    byte[] checkpoint() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(3); out.writeInt(interval); out.writeInt(checkpoints.size());
            for (Map.Entry<Integer, MotionEngine> entry : checkpoints.entrySet()) {
                out.writeInt(entry.getKey()); entry.getValue().writeState(out);
            }
        }
        return bytes.toByteArray();
    }

    void restore(byte[] bytes) throws IOException {
        if (bytes.length > 4096) throw new IOException("Invalid checkpoint size");
        TreeMap<Integer, MotionEngine> restored = new TreeMap<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != 3 || in.readInt() != interval) throw new IOException("Wrong checkpoint version");
            int count = in.readInt();
            if (count < 1 || count > CHECKPOINTS + 1) throw new IOException("Invalid checkpoint count");
            for (int i = 0; i < count; i++) {
                int index = in.readInt();
                if (index < 0 || index > COUNT || index % CHECKPOINT_STEP != 0)
                    throw new IOException("Invalid checkpoint index");
                restored.put(index, MotionEngine.readState(in));
            }
            if (!restored.containsKey(0) || in.available() != 0) throw new IOException("Incomplete checkpoint");
        }
        checkpoints.clear(); checkpoints.putAll(restored); clips.clear();
    }
}
