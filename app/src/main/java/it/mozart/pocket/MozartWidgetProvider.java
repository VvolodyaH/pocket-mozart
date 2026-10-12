package it.mozart.pocket;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public final class MozartWidgetProvider extends AppWidgetProvider {
    private static final String PINNED = "it.mozart.pocket.WIDGET_PINNED";
    static final String IGNORE_TOUCH = "it.mozart.pocket.IGNORE_WIDGET_TOUCH";

    static boolean requestPin(Context c) {
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        Bundle extras = new Bundle();
        extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                new RemoteViews(c.getPackageName(), R.layout.mozart_widget_preview));
        PendingIntent done = PendingIntent.getBroadcast(c, 0,
                new Intent(c, MozartWidgetProvider.class).setAction(PINNED),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return manager.requestPinAppWidget(new ComponentName(c, MozartWidgetProvider.class), extras, done);
    }

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, MozartWidgetProvider.class));
    }

    static void updateAll(Context c) {
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) update(c, manager, id);
    }

    static void update(Context c, AppWidgetManager manager, int id) {
        Prefs.migrate(c);
        manager.updateAppWidget(id, NativeWidgetRenderer.create(c, id, manager.getAppWidgetOptions(id)));
    }

    @Override public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(c, manager, id);
    }

    @Override public void onReceive(Context c, Intent intent) {
        if (IGNORE_TOUCH.equals(intent.getAction())) return;
        if (PINNED.equals(intent.getAction())) { updateAll(c); return; }
        super.onReceive(c, intent);
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager manager, int id, Bundle options) {
        Prefs.migrate(c);
        manager.updateAppWidget(id, NativeWidgetRenderer.create(c, id, options));
    }

    @Override public void onEnabled(Context c) {
        Prefs.migrate(c);
        Prefs.enabled(c, true);
    }

    @Override public void onDeleted(Context c, int[] ids) {
        var edit = Prefs.get(c).edit();
        for (int id : ids) edit.remove("widgetSeed." + id).remove("clipState." + id);
        edit.apply();
    }
}
