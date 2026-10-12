package it.mozart.pocket;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.SizeF;
import android.widget.AdapterViewFlipper;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public class AndroidNativeWidgetTest {
    @Test public void responsiveRemoteViewsInflateInsideAWidgetHostAndPauseAsAnImage() {
        Context context = RuntimeEnvironment.getApplication();
        Bundle sizes = new Bundle();
        sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 40);
        sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 40);
        sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 240);
        sizes.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 200);
        if (Build.VERSION.SDK_INT >= 31)
            sizes.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    new ArrayList<>(List.of(new SizeF(40, 40), new SizeF(240, 200))));
        AppWidgetHostView host = new AppWidgetHostView(context);
        RemoteViews active = NativeWidgetRenderer.create(context, 71, sizes);
        View root = active.apply(context, host);
        assertNotNull(root.findViewById(R.id.widget_animation));
        assertEquals("Mozart · widget 71", root.getContentDescription());
        Parcel parcel = Parcel.obtain();
        try {
            active.writeToParcel(parcel, 0);
            assertTrue(parcel.dataSize() < (Build.VERSION.SDK_INT >= 31 ? 800 * 1024 : 16384));
        } finally { parcel.recycle(); }
        Prefs.enabled(context, false);
        View paused = NativeWidgetRenderer.create(context, 71, sizes).apply(context, host);
        assertNull(paused.findViewById(R.id.widget_animation));
        assertEquals(1, ((ViewGroup) paused).getChildCount());
        assertTrue(((ViewGroup) paused).getChildAt(0) instanceof ImageView);
    }

    private static WidgetFramesService.Factory factory(Context context, int width, int height) {
        return new WidgetFramesService.Factory(context, new Intent().putExtra("width", (float) width)
                .putExtra("height", (float) height).putExtra("seed", 8L).putExtra("size", 123f));
    }

    @Test public void nativeFlipperPlaysAndStopsWhenItsWindowIsHiddenWithoutCrossfades() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity activity = controller.get();
            View root = LayoutInflater.from(activity).inflate(R.layout.mozart_widget, null);
            AdapterViewFlipper flipper = root.findViewById(R.id.widget_animation);
            WidgetFramesService.Factory factory = factory(activity, 350, 250);
            activity.setContentView(root);
            flipper.setAdapter(new BaseAdapter() {
                public int getCount() { return factory.getCount(); }
                public Object getItem(int position) { return position; }
                public long getItemId(int position) { return position; }
                public View getView(int position, View reused, ViewGroup parent) {
                    return factory.getViewAt(position).apply(activity, parent);
                }
            });
            root.dispatchWindowVisibilityChanged(View.VISIBLE);
            assertTrue(flipper.isAutoStart());
            assertTrue(flipper.isFlipping());
            assertEquals(0, flipper.getInAnimation().getDuration());
            assertEquals(0, flipper.getOutAnimation().getDuration());
            assertEquals(0, ((ColorDrawable) root.getBackground()).getColor());
            int first = flipper.getDisplayedChild();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000));
            assertEquals(first + 8, flipper.getDisplayedChild());
            int current = flipper.getDisplayedChild();
            root.dispatchWindowVisibilityChanged(View.GONE);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000));
            assertEquals(current, flipper.getDisplayedChild());
            root.dispatchWindowVisibilityChanged(View.VISIBLE);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(125));
            assertEquals(current + 1, flipper.getDisplayedChild());
        }
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void framesUseTransparentPixelResourcesAndFitIncludingTheSmallestWidget() {
        Context context = RuntimeEnvironment.getApplication();
        for (int[] bounds : new int[][]{{40, 40}, {64, 240}, {350, 250}}) {
            WidgetFramesService.Factory factory = factory(context, bounds[0], bounds[1]);
            for (int position : new int[]{0, 8, 20, 60}) {
                RemoteViews frame = factory.getViewAt(position);
                ImageView image = frame.apply(context, new FrameLayout(context)).findViewById(R.id.widget_frame);
                float density = context.getResources().getDisplayMetrics().density;
                int w = Math.round(bounds[0] * density), h = Math.round(bounds[1] * density);
                image.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                image.layout(0, 0, w, h);
                assertTrue(w - image.getPaddingLeft() - image.getPaddingRight() > 0);
                assertTrue(h - image.getPaddingTop() - image.getPaddingBottom() > 0);
                BitmapDrawable pixels = (BitmapDrawable) image.getDrawable();
                assertTrue(pixels.getBitmap().hasAlpha());
                assertFalse(pixels.getPaint().isFilterBitmap());
                Parcel parcel = Parcel.obtain();
                try {
                    frame.writeToParcel(parcel, 0);
                    assertTrue("frames send resource IDs, not pixel buffers", parcel.dataSize() < 4096);
                } finally { parcel.recycle(); }
            }
        }
        for (MotionEngine.Pose pose : MotionEngine.Pose.values()) for (int i = 0; i < pose.frameCount; i++) {
            assertNotNull(context.getDrawable(PixelSprites.drawable(pose, i, false)));
            assertNotNull(context.getDrawable(PixelSprites.drawable(pose, i, true)));
        }
    }

    @Test public void cacheRefreshPreservesClipIdentityAndNumbering() {
        WidgetFramesService.Factory factory = factory(RuntimeEnvironment.getApplication(), 350, 250);
        RemoteViews first = factory.getViewAt(40);
        int[] before = imagePadding(first, RuntimeEnvironment.getApplication());
        factory.onDataSetChanged();
        factory.getViewAt(80);
        assertArrayEquals(before, imagePadding(factory.getViewAt(40), RuntimeEnvironment.getApplication()));
        assertTrue(factory.hasStableIds());
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void recycledNativeFramesStayOpaqueAndApplyDirectionAndClearNotesInsideAOneRowWidget() {
        Context c = RuntimeEnvironment.getApplication();
        MotionEngine engine = new MotionEngine(8);
        engine.setBounds(292,48,123); engine.setSpecialDates(false);
        engine.request(MotionEngine.Pose.VIOLIN,4);
        View reused = null;
        boolean mirrored = false, unmirrored = false, noteVisible = false, noteCleared = false;
        float density = c.getResources().getDisplayMetrics().density;
        for (int i = 0; i < 600; i++) {
            WidgetTimeline.Frame frame = new WidgetTimeline.Frame(engine,4);
            RemoteViews remote = NativeWidgetRenderer.frame(c,frame,300,56);
            if (reused == null) reused = remote.apply(c,new AppWidgetHostView(c));
            else remote.reapply(c,reused);
            ImageView main = reused.findViewById(R.id.widget_frame);
            ImageView note = reused.findViewById(R.id.widget_note);
            assertEquals("the character never fades",255,main.getImageAlpha());
            assertEquals("one character and one note layer",2,((ViewGroup)reused).getChildCount());
            assertEquals(frame.left ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR,main.getLayoutDirection());
            assertTrue(main.getDrawable().isAutoMirrored());
            mirrored |= frame.left; unmirrored |= !frame.left;
            assertEquals(frame.noteAlpha,note.getImageAlpha());
            if (frame.noteAlpha > 0) {
                noteVisible = true;
                assertNotNull(note.getDrawable());
                assertTrue(note.getPaddingLeft() >= main.getPaddingLeft());
                assertTrue(note.getPaddingRight() >= main.getPaddingRight());
                assertTrue(note.getPaddingTop() >= main.getPaddingTop());
                assertTrue(note.getPaddingBottom() >= main.getPaddingBottom());
                assertTrue(Math.round(56*density) - note.getPaddingTop() - note.getPaddingBottom() > 0);
            } else if (noteVisible) noteCleared = true;
            engine.update(.1f);
        }
        assertTrue("native recycling supports both directions",mirrored && unmirrored);
        assertTrue("notes become visible",noteVisible);
        assertTrue("recycling clears notes instead of leaving a ghost",noteCleared);
    }

    @Test public void androidCollectionCachePrefetchKeepsTheSameNumberedClips() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        WidgetFramesService.Factory factory = factory(context, 350, 250);
        Class<?> cacheClass = Class.forName("android.widget.RemoteViewsAdapter$FixedSizeRemoteViewsCache");
        Object cache = Build.VERSION.SDK_INT < 28
                ? ReflectionHelpers.callConstructor(cacheClass, ClassParameter.from(int.class, 40))
                : ReflectionHelpers.callConstructor(cacheClass, ClassParameter.from(int.class, 40),
                        ClassParameter.from(Configuration.class, context.getResources().getConfiguration()));
        Object metadata = ReflectionHelpers.callInstanceMethod(cache, "getMetaData");
        ReflectionHelpers.setField(metadata, "count", factory.getCount());
        WidgetClipStore expected = new WidgetClipStore(8, 350, 250, 123, 30, true, 125, java.time.LocalDate.now());
        for (int position = 0; position < 1000; position++) {
                RemoteViews cached = ReflectionHelpers.callInstanceMethod(cache, "getRemoteViewsAt",
                        ClassParameter.from(int.class, position));
                if (cached == null) {
                    ReflectionHelpers.callInstanceMethod(cache, "queueRequestedPositionToLoad", ClassParameter.from(int.class, position));
                } else {
                    ReflectionHelpers.callInstanceMethod(cache, "queuePositionsToBePreloadedFromRequestedPosition", ClassParameter.from(int.class, position));
                }
                int next;
                while ((next = ReflectionHelpers.callInstanceMethod(cache, "getNextIndexToLoad")) >= 0) {
                    ReflectionHelpers.callInstanceMethod(cache, "insert",
                            ClassParameter.from(int.class, next), ClassParameter.from(RemoteViews.class, factory.getViewAt(next)),
                            ClassParameter.from(long.class, (long) next), ClassParameter.from(int[].class, new int[]{position}));
                }
                // After a few displayed frames Android refreshes the visible item, starting prefetch.
                if (position == 4) {
                    ReflectionHelpers.callInstanceMethod(cache, "reset");
                    factory.onDataSetChanged();
                    ReflectionHelpers.callInstanceMethod(cache, "insert",
                            ClassParameter.from(int.class, position), ClassParameter.from(RemoteViews.class, factory.getViewAt(position)),
                            ClassParameter.from(long.class, (long) position), ClassParameter.from(int[].class, new int[]{position}));
                    ReflectionHelpers.callInstanceMethod(cache, "queuePositionsToBePreloadedFromRequestedPosition",
                            ClassParameter.from(int.class, position));
                    while ((next = ReflectionHelpers.callInstanceMethod(cache, "getNextIndexToLoad")) >= 0) {
                        ReflectionHelpers.callInstanceMethod(cache, "insert",
                                ClassParameter.from(int.class, next), ClassParameter.from(RemoteViews.class, factory.getViewAt(next)),
                                ClassParameter.from(long.class, (long) next), ClassParameter.from(int[].class, new int[]{position}));
                    }
                    assertNotNull(ReflectionHelpers.callInstanceMethod(cache, "getRemoteViewsAt", ClassParameter.from(int.class, 10)));
                }
                RemoteViews visible = ReflectionHelpers.callInstanceMethod(cache, "getRemoteViewsAt",
                        ClassParameter.from(int.class, position));
                assertNotNull(visible);
                WidgetTimeline.Frame frame = expected.get(position / 32)[position % 32];
                float density = context.getResources().getDisplayMetrics().density;
                int[] padding = imagePadding(visible, context);
                assertEquals(Math.round(frame.x * density), padding[0]);
                assertEquals(Math.round(frame.y * density), padding[1]);
        }
    }

    private static int[] imagePadding(RemoteViews remote, Context context) {
        ImageView image = remote.apply(context, new AppWidgetHostView(context)).findViewById(R.id.widget_frame);
        return new int[]{image.getPaddingLeft(), image.getPaddingTop(), image.getPaddingRight(), image.getPaddingBottom()};
    }
}
