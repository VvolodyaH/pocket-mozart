package it.mozart.pocket;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterViewFlipper;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowAppWidgetManager;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import static org.junit.Assert.*;

/** Models service recreation while the launcher retains the flipper and its prefetched frames. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26, shadows = WidgetWakeRegressionTest.NotificationManager.class)
public class WidgetWakeRegressionTest {
    private static final int WIDGET_ID = 77, CURRENT = 400;

    @Implements(AppWidgetManager.class)
    public static class NotificationManager extends ShadowAppWidgetManager {
        static final List<Integer> notifications = new ArrayList<>();
        static Runnable invalidate;

        @Implementation protected void notifyAppWidgetViewDataChanged(int id, int viewId) {
            notifyAppWidgetViewDataChanged(new int[]{id}, viewId);
        }

        @Implementation protected void notifyAppWidgetViewDataChanged(int[] ids, int viewId) {
            assertEquals(R.id.widget_animation, viewId);
            for (int id : ids) notifications.add(id);
            if (invalidate != null) invalidate.run();
        }
    }

    @Before public void resetNotifications() {
        NotificationManager.notifications.clear();
        NotificationManager.invalidate = null;
    }

    private static WidgetFramesService.Factory factory(Context c, int interval) {
        return new WidgetFramesService.Factory(c, new Intent().putExtra("width", 300f)
                .putExtra("height", 56f).putExtra("seed", 8L).putExtra("size", 123f)
                .putExtra("interval", interval).putExtra("dates", false).putExtra("date", "2000-01-01")
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_ID));
    }

    static final class LauncherCache {
        final AdapterViewFlipper flipper;
        final Object cache;
        final RemoteViews current;

        LauncherCache(Context c, int interval) {
            Bundle bounds = new Bundle();
            bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300);
            bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300);
            bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56);
            bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 56);
            // Explicitly model the pre-2.7 service path, also used on Android 8–11.
            RemoteViews legacy = new RemoteViews(c.getPackageName(), R.layout.mozart_widget);
            legacy.setRemoteAdapter(R.id.widget_animation, new Intent(c, WidgetFramesService.class)
                    .setData(Uri.parse("mozart://legacy-cache/" + interval)));
            View root = legacy.apply(c, new AppWidgetHostView(c));
            flipper = root.findViewById(R.id.widget_animation);
            Object adapter = ReflectionHelpers.getField(flipper, "mRemoteViewsAdapter");
            cache = ReflectionHelpers.getField(adapter, "mCache");
            Object metadata = ReflectionHelpers.callInstanceMethod(cache, "getMetaData");
            WidgetFramesService.Factory previous = factory(c, interval);
            ReflectionHelpers.setField(metadata, "count", previous.getCount());
            ReflectionHelpers.setField(metadata, "viewTypeCount", 2);
            Object loading = ReflectionHelpers.callConstructor(classFor("android.widget.RemoteViewsAdapter$LoadingViewTemplate"),
                    ClassParameter.from(RemoteViews.class, previous.getLoadingView()), ClassParameter.from(Context.class, c));
            ReflectionHelpers.setField(metadata, "loadingTemplate", loading);
            current = previous.getViewAt(CURRENT);
            insert(CURRENT, current);
            insert(CURRENT + 1, previous.getViewAt(CURRENT + 1));
            previous.onDestroy();
            flipper.setAdapter((Adapter) adapter);
            flipper.setDisplayedChild(CURRENT);
            assertNotNull(firstImage(flipper.getCurrentView()));
            // AOSP invalidation retains/reloads the visible frame, but discards the upcoming ones.
            // Loading from the remote service is deliberately not completed before the next flip.
            NotificationManager.invalidate = () -> {
                ReflectionHelpers.callInstanceMethod(cache, "reset");
                insert(CURRENT, current);
            };
        }

        private void insert(int position, RemoteViews views) {
            ReflectionHelpers.callInstanceMethod(cache, "insert", ClassParameter.from(int.class, position),
                    ClassParameter.from(RemoteViews.class, views), ClassParameter.from(long.class, (long) position),
                    ClassParameter.from(int[].class, new int[]{CURRENT}));
        }

        void assertNextFrameRemainsVisible() {
            flipper.showNext();
            assertEquals(CURRENT + 1, flipper.getDisplayedChild());
            assertNotNull("waking must retain the prefetched next frame instead of showing the transparent loading view",
                    firstImage(flipper.getCurrentView()));
        }
    }

    @Test public void recreatingTheServiceAtAnExistingPositionDoesNotBlankTheNextFrame() {
        Context c = RuntimeEnvironment.getApplication();
        for (int interval : new int[]{125, 83}) {
            LauncherCache launcher = new LauncherCache(c, interval);
            WidgetFramesService.Factory resumed = factory(c, interval);
            resumed.getViewAt(CURRENT + 2); // The launcher asks for another frame after service recreation.
            resumed.getViewAt(0); // A later backward/prefetch request must not rearm the bootstrap.
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            launcher.assertNextFrameRemainsVisible();
            assertTrue("service reconnection must not invalidate an unchanged timeline", NotificationManager.notifications.isEmpty());
            resumed.onDestroy();
            NotificationManager.invalidate = null;
        }
    }

    @Test public void restoringThePositionCancelsAStartupWarmupQueuedByFrameZero() {
        Context c = RuntimeEnvironment.getApplication();
        LauncherCache launcher = new LauncherCache(c, 125);
        WidgetFramesService.Factory resumed = factory(c, 125);
        resumed.getViewAt(0); // Android may request the default item before restoring saved state.
        resumed.getViewAt(CURRENT + 2);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        launcher.assertNextFrameRemainsVisible();
        assertTrue(NotificationManager.notifications.isEmpty());
        resumed.onDestroy();
    }

    @Test public void anActualFirstStartupStillWarmsExactlyOnceAtBothFrameRates() {
        Context c = RuntimeEnvironment.getApplication();
        for (int interval : new int[]{125, 83}) {
            NotificationManager.notifications.clear();
            WidgetFramesService.Factory first = factory(c, interval);
            for (int i = 0; i <= 4; i++) {
                first.getViewAt(i);
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(interval));
            }
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(List.of(WIDGET_ID), NotificationManager.notifications);
            first.onDataSetChanged();
            first.getViewAt(0);
            first.getViewAt(CURRENT);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals("one warmup per initial playback, never every reconnect", List.of(WIDGET_ID), NotificationManager.notifications);
            first.onDestroy();
        }
    }

    @Test public void destroyingAFactoryCancelsItsPendingInvalidation() {
        Context c = RuntimeEnvironment.getApplication();
        WidgetFramesService.Factory first = factory(c, 125);
        first.getViewAt(0);
        first.onDestroy();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertTrue(NotificationManager.notifications.isEmpty());
    }

    @Test public void invalidatingAnUnchangedCacheCanReallyDisplayATransparentFrame() {
        Context c = RuntimeEnvironment.getApplication();
        LauncherCache launcher = new LauncherCache(c, 125);
        AppWidgetManager.getInstance(c).notifyAppWidgetViewDataChanged(WIDGET_ID, R.id.widget_animation);
        assertEquals(List.of(WIDGET_ID), NotificationManager.notifications);
        launcher.flipper.showNext();
        assertEquals(CURRENT + 1, launcher.flipper.getDisplayedChild());
        assertNull("the framework applies the transparent loading layout when the next item has been discarded",
                firstImage(launcher.flipper.getCurrentView()));
    }

    private static Class<?> classFor(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException ex) { throw new AssertionError(ex); }
    }

    private static ImageView firstImage(View v) {
        if (v instanceof ImageView image) return image;
        if (v instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            ImageView image = firstImage(group.getChildAt(i));
            if (image != null) return image;
        }
        return null;
    }
}
