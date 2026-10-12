package it.mozart.pocket;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.ImageView;
import android.widget.RemoteViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public class WidgetTeleportRegressionTest {
    private static WidgetFramesService.Factory row(Context c) {
        return new WidgetFramesService.Factory(c, new Intent().putExtra("width", 300f)
                .putExtra("height", 56f).putExtra("seed", 8L).putExtra("size", 123f));
    }

    @Test public void aLoadingItemNeverDrawsAnotherMozartAtTheCenterOfTheRow() {
        Context c = RuntimeEnvironment.getApplication();
        View loading = row(c).getLoadingView().apply(c, new AppWidgetHostView(c));
        assertNull("a cache miss must not insert a centered character", firstImage(loading));
    }

    @Test public void requestingAheadAndThenBackDoesNotRewriteAnAlreadyServedPosition() {
        Context c = RuntimeEnvironment.getApplication();
        WidgetFramesService.Factory factory = row(c);
        int[] original = padding(factory.getViewAt(0), c);
        factory.getViewAt(359);
        factory.getViewAt(20);
        assertArrayEquals("position zero is immutable, even after prefetch and backward requests",
                original, padding(factory.getViewAt(0), c));
    }

    @Test public void ordinaryWidgetUpdatesReuseTheAdapterInsteadOfStartingAnotherJourney() {
        Context c = RuntimeEnvironment.getApplication();
        if (Build.VERSION.SDK_INT < 31) {
            assertEquals(adapterKey(c), adapterKey(c));
            return;
        }
        View root = NativeWidgetRenderer.create(c, 37, new Bundle()).apply(c, new AppWidgetHostView(c));
        AdapterViewFlipper flipper = root.findViewById(R.id.widget_animation);
        flipper.setDisplayedChild(130);
        Object adapter = flipper.getAdapter();
        ImageView image = firstImage(flipper.getCurrentView());
        int[] before = {image.getPaddingLeft(), image.getPaddingTop(), image.getPaddingRight(), image.getPaddingBottom()};
        NativeWidgetRenderer.create(c, 37, new Bundle()).reapply(c, root);
        assertSame(adapter, flipper.getAdapter());
        assertEquals(130, flipper.getDisplayedChild());
        image = firstImage(flipper.getCurrentView());
        assertArrayEquals(before, new int[]{image.getPaddingLeft(), image.getPaddingTop(), image.getPaddingRight(), image.getPaddingBottom()});
    }

    private static String adapterKey(Context c) {
        View root = NativeWidgetRenderer.create(c, 37, new Bundle()).apply(c, new AppWidgetHostView(c));
        AdapterViewFlipper flipper = root.findViewById(R.id.widget_animation);
        Object adapter = ReflectionHelpers.getField(flipper, "mRemoteViewsAdapter");
        Intent intent = ReflectionHelpers.getField(adapter, "mIntent");
        return intent.getDataString();
    }
    private static int[] padding(RemoteViews remote, Context c) {
        ImageView image = firstImage(remote.apply(c, new AppWidgetHostView(c)));
        assertNotNull(image);
        return new int[]{image.getPaddingLeft(), image.getPaddingTop(), image.getPaddingRight(), image.getPaddingBottom()};
    }
    private static ImageView firstImage(View view) {
        if (view instanceof ImageView image) return image;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            ImageView result = firstImage(group.getChildAt(i));
            if (result != null) return result;
        }
        return null;
    }
}
