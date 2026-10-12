package it.mozart.pocket;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {26, 35})
public class AndroidStartupTest {
    @Test public void firstLaunchLoadsTheRealSpriteAtlasWithoutRequestingAnyPermission() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible()) {
            MainActivity activity = controller.get();
            assertNotNull(find(activity.findViewById(android.R.id.content), PetSceneView.class));
            assertTrue(Prefs.enabled(activity));
            assertNull(Shadows.shadowOf(activity.getApplication()).getNextStartedService());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            var info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS | PackageManager.GET_SERVICES);
            assertTrue(info.requestedPermissions == null || info.requestedPermissions.length == 0);
            assertEquals(1, info.services.length);
            assertEquals(WidgetFramesService.class.getName(), info.services[0].name);
            assertEquals("android.permission.BIND_REMOTEVIEWS", info.services[0].permission);
        }
    }

    @Test public void oneTapPinsAndEnablesTheWidgetWithoutAnyPermissionOrSetupPage() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible()) {
            MainActivity activity = controller.get();
            Shadows.shadowOf(AppWidgetManager.getInstance(activity)).setRequestPinAppWidgetSupported(true);
            findButton(activity.findViewById(android.R.id.content), "＋  Aggiungi Mozart alla Home").performClick();
            assertEquals(1, MozartWidgetProvider.ids(activity).length);
            assertTrue(Prefs.enabled(activity));
            assertNull(Shadows.shadowOf(activity.getApplication()).getNextStartedService());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            controller.pause().resume();
            assertNotNull(findButton(activity.findViewById(android.R.id.content), "Metti Mozart in pausa"));
            Intent home = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(Intent.ACTION_MAIN, home.getAction());
            assertTrue(home.hasCategory(Intent.CATEGORY_HOME));
        }
    }

    @Test public void cancellationLeavesNoWindowOrServiceAndTheAddButtonRemainsAvailable() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible()) {
            MainActivity activity = controller.get();
            Shadows.shadowOf(AppWidgetManager.getInstance(activity)).setRequestPinAppWidgetSupported(false);
            findButton(activity.findViewById(android.R.id.content), "＋  Aggiungi Mozart alla Home").performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(dialog);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertEquals(0, MozartWidgetProvider.ids(activity).length);
            assertNotNull(findButton(activity.findViewById(android.R.id.content), "＋  Aggiungi Mozart alla Home"));
            assertNull(Shadows.shadowOf(activity.getApplication()).getNextStartedService());
        }
    }

    @Test public void upgradeRemovesOldAreaOverridesAndStartsTheNativeWidget() {
        var context = org.robolectric.RuntimeEnvironment.getApplication();
        Prefs.get(context).edit().putBoolean("enabled", false).putBoolean("manual", true)
                .putInt("left", 900).putInt("width", 10).putInt("size", 128).apply();
        Prefs.migrate(context);
        assertTrue(Prefs.enabled(context));
        assertFalse(Prefs.get(context).contains("manual"));
        assertFalse(Prefs.get(context).contains("left"));
        assertFalse(Prefs.get(context).contains("width"));
        assertEquals(128, Prefs.size(context));
        Prefs.enabled(context, false);
        Prefs.migrate(context);
        assertFalse("migration does not undo a later pause", Prefs.enabled(context));
    }

    private static <T extends View> T find(View root, Class<T> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            T result = find(group.getChildAt(i), type);
            if (result != null) return result;
        }
        return null;
    }
    private static Button findButton(View root, String text) {
        if (root instanceof Button b && b.getText().toString().equals(text)) return b;
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            Button result = findButton(group.getChildAt(i), text);
            if (result != null) return result;
        }
        return null;
    }
}
