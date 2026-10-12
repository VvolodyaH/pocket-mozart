package it.mozart.pocket;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterViewFlipper;
import android.widget.RemoteViews;
import android.widget.Switch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public class WidgetTouchLockTest {
    private static final String LABEL = "Blocca tocchi sul widget";

    private static void lock(Context c, boolean value) {
        Prefs.get(c).edit().putBoolean("touchLock", value).apply();
    }

    private static RemoteViews widget(Context c) {
        Bundle bounds = new Bundle();
        bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300);
        bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300);
        bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56);
        bounds.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 56);
        Parcel parcel = Parcel.obtain();
        try {
            NativeWidgetRenderer.create(c, 19, bounds).writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return RemoteViews.CREATOR.createFromParcel(parcel);
        } finally { parcel.recycle(); }
    }

    private static void assertAppOpened() {
        Intent opened = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertNotNull("an unlocked widget opens the app", opened);
        assertEquals(MainActivity.class.getName(), opened.getComponent().getClassName());
    }

    private static void assertNoAppOpened() {
        assertNull("a locked widget must never launch an activity",
                Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
    }

    @Test public void lockingAndUnlockingKeepCachedAndUpcomingFramesAndChangeBothClickRoutes() {
        Context c = RuntimeEnvironment.getApplication();
        ServiceController<WidgetFramesService> controller = Robolectric.buildService(WidgetFramesService.class).create();
        try {
            IBinder binder = controller.get().onBind(new Intent(c, WidgetFramesService.class)
                    .setData(Uri.parse("mozart://touch-lock-test"))
                    .putExtra("width", 300f).putExtra("height", 56f).putExtra("seed", 8L).putExtra("size", 123f));
            View root = widget(c).apply(c, new AppWidgetHostView(c));
            AdapterViewFlipper flipper = root.findViewById(R.id.widget_animation);
            Object adapter;
            if (Build.VERSION.SDK_INT >= 31) {
                adapter = flipper.getAdapter();
                assertNotNull(adapter);
            } else {
            adapter = ReflectionHelpers.getField(flipper, "mRemoteViewsAdapter");
            Object cache = ReflectionHelpers.getField(adapter, "mCache");
            Object metadata = ReflectionHelpers.callInstanceMethod(cache, "getMetaData");
            ReflectionHelpers.setField(metadata, "count", 3);
            ReflectionHelpers.setField(metadata, "viewTypeCount", 2);
            for (int i = 0; i < 3; i++) {
                // Use the real service adapter: it marks each RemoteViews as a collection item.
                RemoteViews frame = ReflectionHelpers.callInstanceMethod(binder, "getViewAt", ClassParameter.from(int.class, i));
                ReflectionHelpers.callInstanceMethod(cache, "insert", ClassParameter.from(int.class, i),
                        ClassParameter.from(RemoteViews.class, frame), ClassParameter.from(long.class, (long) i),
                        ClassParameter.from(int[].class, new int[]{0}));
            }
            flipper.setAdapter((Adapter) adapter);
            }
            View cached = flipper.getCurrentView();
            root.performClick();
            assertAppOpened();
            flipper.performItemClick(cached, 0, 0);
            assertAppOpened();

            lock(c, true);
            widget(c).reapply(c, root);
            assertSame("changing the touch setting must preserve the adapter", adapter,
                    flipper.getAdapter());
            assertSame("the already visible frame stays in place", cached, flipper.getCurrentView());
            root.performClick();
            flipper.performItemClick(cached, 0, 0);
            assertNoAppOpened();
            flipper.showNext();
            assertEquals(1, flipper.getDisplayedChild());
            flipper.performItemClick(flipper.getCurrentView(), 1, 1);
            assertNoAppOpened();
            assertTrue("animation remains enabled while touches are locked", Prefs.enabled(c));

            lock(c, false);
            widget(c).reapply(c, root);
            assertEquals("unlocking must not reset playback", 1, flipper.getDisplayedChild());
            root.performClick();
            assertAppOpened();
            flipper.performItemClick(flipper.getCurrentView(), 1, 1);
            assertAppOpened();
        } finally { controller.destroy(); }
    }

    @Test public void aPausedWidgetAlsoClearsItsExistingClickAndCanBeUnlocked() {
        Context c = RuntimeEnvironment.getApplication();
        Prefs.enabled(c, false);
        View root = widget(c).apply(c, new AppWidgetHostView(c));
        root.performClick();
        assertAppOpened();
        lock(c, true);
        widget(c).reapply(c, root);
        root.performClick();
        assertNoAppOpened();
        assertNull(root.findViewById(R.id.widget_animation));
        lock(c, false);
        widget(c).reapply(c, root);
        root.performClick();
        assertAppOpened();
    }

    @Test public void theVisibleTogglePersistsAndImmediatelyUpdatesAllExistingWidgets() {
        Context c = RuntimeEnvironment.getApplication();
        var manager = Shadows.shadowOf(AppWidgetManager.getInstance(c));
        int[] ids = manager.createWidgets(MozartWidgetProvider.class, R.layout.mozart_widget, 2);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Switch toggle = findToggle(activity.findViewById(android.R.id.content));
            assertNotNull(toggle);
            assertFalse("upgrades keep the existing unlocked default", toggle.isChecked());
            for (int id : ids) {
                manager.getViewFor(id).performClick();
                assertAppOpened();
            }
            toggle.setChecked(true);
            assertTrue(Prefs.touchLocked(c));
            for (int id : ids) {
                manager.getViewFor(id).performClick();
                assertNoAppOpened();
            }
            controller.pause().resume();
            toggle = findToggle(activity.findViewById(android.R.id.content));
            assertTrue("the lock survives reopening the settings", toggle.isChecked());
            toggle.setChecked(false);
            for (int id : ids) {
                manager.getViewFor(id).performClick();
                assertAppOpened();
            }
        }
    }

    private static Switch findToggle(View root) {
        if (root instanceof Switch toggle && LABEL.contentEquals(toggle.getContentDescription())) return toggle;
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            Switch toggle = findToggle(group.getChildAt(i));
            if (toggle != null) return toggle;
        }
        return null;
    }
}
