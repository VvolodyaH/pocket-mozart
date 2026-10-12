package it.mozart.pocket;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.widget.AdapterViewFlipper;
import android.widget.ViewFlipper;
import android.widget.ViewAnimator;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.time.Duration;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {31, 35}, shadows = ResidentPlaybackTest.FrameworkViewAnimator.class)
public class ResidentPlaybackTest {
    // Robolectric's default ShadowViewAnimator keeps a separate currentChild and does not
    // update Android's mWhichChild, which ViewFlipper reads on resume. Run the real framework.
    @Implements(ViewAnimator.class)
    public static class FrameworkViewAnimator extends ShadowViewGroup { }

    private static Bundle row() {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 300);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 56);
        return options;
    }

    private static RemoteViews transferred(Context c, Bundle bounds) {
        Parcel parcel = Parcel.obtain();
        try {
            NativeWidgetRenderer.create(c, 88, bounds).writeToParcel(parcel, 0);
            assertTrue("all responsive variants together leave room below Binder's 1 MiB limit: " + parcel.dataSize(),
                    parcel.dataSize() < 800 * 1024);
            parcel.setDataPosition(0);
            return RemoteViews.CREATOR.createFromParcel(parcel);
        } finally { parcel.recycle(); }
    }

    private static void assertVisible(ViewGroup flipper) {
        assertEquals(View.VISIBLE, current(flipper).getVisibility());
        assertEquals(1f, current(flipper).getAlpha(), 0);
        ImageView image = character(current(flipper));
        assertNotNull("every displayed item contains Mozart immediately, without running a load callback", image);
        assertNotNull(image.getDrawable());
        assertEquals(255, image.getImageAlpha());
    }

    private static View current(ViewGroup player) {
        return player instanceof AdapterViewFlipper adapter ? adapter.getCurrentView() : ((ViewFlipper) player).getCurrentView();
    }
    private static int position(ViewGroup player) {
        return player instanceof AdapterViewFlipper adapter ? adapter.getDisplayedChild() : ((ViewFlipper) player).getDisplayedChild();
    }
    private static void next(ViewGroup player) {
        if (player instanceof AdapterViewFlipper adapter) adapter.showNext(); else ((ViewFlipper) player).showNext();
    }

    private static ImageView character(View view) {
        // Android 35 wraps collection children in an AppWidgetHostView with its own ID namespace.
        if (view instanceof ImageView image && image.getId() == R.id.widget_frame) return image;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            ImageView image = character(group.getChildAt(i));
            if (image != null) return image;
        }
        return null;
    }

    @Test public void returningFromAdjacentPagesAndScreenOffNeverNeedsAnAsyncFrame() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity c = controller.get();
            Prefs.get(c).edit().putLong("widgetSeed.88", 8).apply();
            for (boolean eco : new boolean[]{true, false}) {
                Prefs.get(c).edit().putBoolean("eco", eco).apply();
                View root = transferred(c, row()).apply(c, new AppWidgetHostView(c));
                c.setContentView(root);
                ViewGroup flipper = root.findViewById(R.id.widget_animation);
                if (flipper instanceof AdapterViewFlipper adapter) {
                    assertNull("no service adapter to reconnect or evict frames", ReflectionHelpers.getField(adapter, "mRemoteViewsAdapter"));
                    assertNotNull(adapter.getAdapter());
                } else assertFalse(((ViewFlipper) flipper).getMeasureAllChildren());
                assertVisible(flipper);
                int count = flipper instanceof AdapterViewFlipper adapter ? adapter.getAdapter().getCount() : flipper.getChildCount();
                for (int i = 0; i < count + 10; i++) {
                    if (i % 53 == 0) {
                        int position = position(flipper);
                        root.dispatchWindowVisibilityChanged(View.GONE);
                        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
                        assertEquals(position, position(flipper));
                        root.dispatchWindowVisibilityChanged(View.VISIBLE);
                        assertVisible(flipper);
                    }
                    next(flipper);
                    assertVisible(flipper);
                }
                int position = position(flipper);
                transferred(c, row()).reapply(c, root);
                assertEquals(position, position(flipper));
                assertVisible(flipper);
                if (flipper instanceof AdapterViewFlipper adapter) adapter.stopFlipping();
                else ((ViewFlipper) flipper).stopFlipping();
            }
        }
    }

    @Test public void completeResidentParcelIsBoundedWithResponsiveAndUnusuallyManySizes() {
        Context c = RuntimeEnvironment.getApplication();
        Prefs.get(c).edit().putLong("widgetSeed.88", 8).apply();
        for (int count : new int[]{1, 2, 4, 16}) {
            Bundle bounds = row();
            ArrayList<SizeF> sizes = new ArrayList<>();
            for (int i = 0; i < count; i++) sizes.add(new SizeF(40 + i * 100, 40 + i * 30));
            bounds.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
            View root = transferred(c, bounds).apply(c, new AppWidgetHostView(c));
            ViewGroup flipper = root.findViewById(R.id.widget_animation);
            assertVisible(flipper);
        }
    }

    @Test public void thePreviousServicePlayerCanBlankWithoutAnyWarmupInvalidation() {
        Context c = RuntimeEnvironment.getApplication();
        WidgetWakeRegressionTest.LauncherCache old = new WidgetWakeRegressionTest.LauncherCache(c, 125);
        old.flipper.setDisplayedChild(440); // Uncached index, remote response has not arrived.
        assertNull("the old adapter immediately displays its empty loading item", character(old.flipper.getCurrentView()));
        old.flipper.stopFlipping();
        WidgetWakeRegressionTest.NotificationManager.invalidate = null;
    }

    @Test @Config(sdk = 31) public void android12StableChildrenKeepPositionAndRespectLockedTouches() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity c = controller.get();
            View root = transferred(c, row()).apply(c, new AppWidgetHostView(c));
            c.setContentView(root);
            ViewFlipper flipper = root.findViewById(R.id.widget_animation);
            flipper.setDisplayedChild(160);
            View cached = flipper.getCurrentView();
            tapCharacter(root, flipper);
            assertNotNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
            Prefs.get(c).edit().putBoolean("touchLock", true).apply();
            transferred(c, row()).reapply(c, root);
            assertEquals(160, flipper.getDisplayedChild());
            assertSame("stable children preserve the current image on reapply", cached, flipper.getCurrentView());
            tapCharacter(root, flipper);
            assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
            flipper.showNext();
            assertVisible(flipper);
            tapCharacter(root, flipper);
            assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
            Prefs.get(c).edit().putBoolean("touchLock", false).apply();
            transferred(c, row()).reapply(c, root);
            assertEquals(161, flipper.getDisplayedChild());
            tapCharacter(root, flipper);
            assertNotNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
        }
    }

    private static void tapCharacter(View root, ViewFlipper flipper) {
        float density = root.getResources().getDisplayMetrics().density;
        int width = Math.round(300 * density), height = Math.round(56 * density);
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        ImageView image = character(flipper.getCurrentView());
        float x = (image.getPaddingLeft() + width - image.getPaddingRight()) / 2f;
        float y = (image.getPaddingTop() + height - image.getPaddingBottom()) / 2f;
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0);
        root.dispatchTouchEvent(down); root.dispatchTouchEvent(up);
        down.recycle(); up.recycle();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
}
