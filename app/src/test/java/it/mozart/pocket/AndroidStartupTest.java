package it.mozart.pocket;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
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
            findButton(activity.findViewById(android.R.id.content), "＋ Add Mozart to home").performClick();
            assertEquals(1, MozartWidgetProvider.ids(activity).length);
            assertTrue(Prefs.enabled(activity));
            assertNull(Shadows.shadowOf(activity.getApplication()).getNextStartedService());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
            controller.pause().resume();
            assertNotNull(findButton(activity.findViewById(android.R.id.content), "Pause Mozart"));
            Intent home = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(Intent.ACTION_MAIN, home.getAction());
            assertTrue(home.hasCategory(Intent.CATEGORY_HOME));
        }
    }

    @Test public void cancellationLeavesNoWindowOrServiceAndTheAddButtonRemainsAvailable() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible()) {
            MainActivity activity = controller.get();
            Shadows.shadowOf(AppWidgetManager.getInstance(activity)).setRequestPinAppWidgetSupported(false);
            findButton(activity.findViewById(android.R.id.content), "＋ Add Mozart to home").performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(dialog);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertEquals(0, MozartWidgetProvider.ids(activity).length);
            assertNotNull(findButton(activity.findViewById(android.R.id.content), "＋ Add Mozart to home"));
            assertNull(Shadows.shadowOf(activity.getApplication()).getNextStartedService());
        }
    }

    @Test @Config(qualifiers = "it-rIT")
    public void englishInterfaceAndPlacementHelpRemainEnglishOnAnItalianPhone() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible()) {
            MainActivity activity = controller.get();
            assertEquals("it", activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
            View root = activity.findViewById(android.R.id.content);
            assertNotNull(findButton(root, "Sheet music"));
            assertNotNull(findButton(root, "Pocket watch"));
            assertNotNull(findButton(root, "Birthday"));
            assertNotNull(findText(root, "LIVE PREVIEW"));
            assertNotNull(findText(root, "Special dates"));
            assertEquals("Mozart’s size", find(root, SeekBar.class).getContentDescription());
            Switch lock = find(root, Switch.class);
            assertEquals("Lock widget taps", lock.getContentDescription());
            assertEquals("On", lock.getTextOn());
            assertEquals("Off", lock.getTextOff());
            View widgetPreview = activity.getLayoutInflater().inflate(R.layout.mozart_widget_preview, null);
            assertEquals("Mozart’s movement area", widgetPreview.getContentDescription());

            Shadows.shadowOf(AppWidgetManager.getInstance(activity)).setRequestPinAppWidgetSupported(false);
            findButton(root, "＋ Add Mozart to home").performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertEquals("Add Pocket Mozart", Shadows.shadowOf(dialog).getTitle());
            assertTrue(Shadows.shadowOf(dialog).getMessage().toString().contains("Widgets → Pocket Mozart"));
            assertEquals("Go to home screen", dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText());
            assertEquals("Cancel", dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText());
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertNotNull(findButton(activity.findViewById(android.R.id.content), "＋ Add Mozart to home"));
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
    private static TextView findText(View root, String text) {
        if (root instanceof TextView t && t.getText().toString().equals(text)) return t;
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            TextView result = findText(group.getChildAt(i), text);
            if (result != null) return result;
        }
        return null;
    }
}
