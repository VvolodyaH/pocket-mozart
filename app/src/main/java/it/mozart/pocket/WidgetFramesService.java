package it.mozart.pocket;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.io.IOException;
import java.time.LocalDate;

/** Delivers immutable numbered frames regardless of the launcher's request or prefetch order. */
public final class WidgetFramesService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) { return new Factory(this, intent); }

    static final class Factory implements RemoteViewsFactory {
        // At the first playback only: 0..4 precede the delayed prefetch bootstrap.
        private static final int STARTUP_LAST_FRAME = 4;
        private final Context context;
        private final float width, height;
        private final WidgetClipStore clips;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final int widgetId, interval;
        private final int framesPerClip;
        private final Runnable warmCache;
        private boolean warmupQueued, warmupComplete;
        private final String fingerprint, checkpointKey;
        private int savedRevision;

        Factory(Context c, Intent intent) {
            context = c.getApplicationContext();
            width = Math.max(16, intent.getFloatExtra("width", 160));
            height = Math.max(16, intent.getFloatExtra("height", 160));
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            interval = intent.getIntExtra("interval", 125) == 83 ? 83 : 125;
            framesPerClip = WidgetClipStore.frameCount(interval);
            warmCache = () -> {
                synchronized (this) {
                    if (warmupComplete) return;
                    warmupComplete = true;
                    AppWidgetManager.getInstance(context)
                            .notifyAppWidgetViewDataChanged(widgetId, R.id.widget_animation);
                }
            };
            int tempo = Math.max(0, Math.min(2, intent.getIntExtra("tempo", 1)));
            LocalDate date;
            try { date = LocalDate.parse(intent.getStringExtra("date")); }
            catch (RuntimeException ex) { date = LocalDate.now(); }
            long seed = intent.getLongExtra("seed", 8);
            float size = intent.getFloatExtra("size", 123);
            boolean dates = intent.getBooleanExtra("dates", true);
            clips = new WidgetClipStore(seed, width, height, size, 30 * new float[]{0.65f, 1f, 1.5f}[tempo],
                    dates, interval, date);
            fingerprint = "2.4|" + seed + "|" + width + "|" + height + "|" + size + "|"
                    + tempo + "|" + dates + "|" + interval + "|" + date;
            checkpointKey = "clipState." + widgetId;
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                String saved = Prefs.get(context).getString(checkpointKey, "");
                String prefix = fingerprint + "\n";
                if (saved.startsWith(prefix)) try {
                    clips.restore(Base64.decode(saved.substring(prefix.length()), Base64.NO_WRAP));
                } catch (IOException | IllegalArgumentException ex) {
                    Prefs.get(context).edit().remove(checkpointKey).apply();
                }
            }
            savedRevision = clips.revision();
        }

        @Override public void onCreate() { }
        @Override public void onDataSetChanged() { } // Refreshing a cache never changes clip identities.
        @Override public synchronized void onDestroy() {
            warmupComplete = true;
            handler.removeCallbacks(warmCache);
        }
        @Override public int getCount() { return WidgetClipStore.COUNT * framesPerClip; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return true; }
        @Override public RemoteViews getLoadingView() {
            // A missing item must never draw a second character at an unrelated position.
            return new RemoteViews(context.getPackageName(), R.layout.mozart_widget_loading);
        }

        @Override public synchronized RemoteViews getViewAt(int position) {
            if (position < 0 || position >= getCount()) return null;
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !warmupComplete) {
                if (position > STARTUP_LAST_FRAME) {
                    // A restored position or an already working prefetch needs no bootstrap.
                    // notifyAppWidgetViewDataChanged would discard the launcher's ready frames
                    // after service recreation (e.g. screen wake), briefly showing loading views.
                    warmupComplete = true;
                    handler.removeCallbacks(warmCache);
                } else if (!warmupQueued) {
                    warmupQueued = true;
                    handler.postDelayed(warmCache, interval * 4L + 50);
                }
            }
            WidgetTimeline.Frame frame = clips.get(position / framesPerClip)[position % framesPerClip];
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (savedRevision != clips.revision()) try {
                    String saved = fingerprint + "\n" + Base64.encodeToString(clips.checkpoint(), Base64.NO_WRAP);
                    Prefs.get(context).edit().putString(checkpointKey, saved).apply();
                    savedRevision = clips.revision();
                } catch (IOException ex) {
                    // Playback can continue using memory if the optional checkpoint fails.
                }
            }
            return NativeWidgetRenderer.frame(context, frame, width, height);
        }
    }
}
