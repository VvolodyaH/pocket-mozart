package it.mozart.pocket;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDate;

final class NativeWidgetRenderer {
    // Resource-only items. Keep the complete responsive parcel comfortably below Binder's 1 MiB.
    static final int RESIDENT_FRAME_BUDGET = 1600;
    static RemoteViews create(Context c, int id, Bundle options) {
        long seed = Prefs.widgetSeed(c, id);
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
            if (sizes != null && !sizes.isEmpty() && sizes.size() <= 4) {
                Map<SizeF, RemoteViews> views = new LinkedHashMap<>();
                for (SizeF size : sizes) {
                    if (size != null && Float.isFinite(size.getWidth()) && Float.isFinite(size.getHeight())
                            && size.getWidth() > 0 && size.getHeight() > 0)
                        views.put(size, sized(c, id, size.getWidth(), size.getHeight(), seed,
                                RESIDENT_FRAME_BUDGET / sizes.size()));
                }
                if (!views.isEmpty()) return new RemoteViews(views);
            }
        }
        float minW = dimension(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160));
        float minH = dimension(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160));
        float maxW = dimension(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, (int) minW));
        float maxH = dimension(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, (int) minH));
        if (minW == maxW && minH == maxH)
            return sized(c, id, minW, minH, seed, RESIDENT_FRAME_BUDGET);
        return new RemoteViews(sized(c, id, maxW, minH, seed, RESIDENT_FRAME_BUDGET / 2),
                sized(c, id, minW, maxH, seed, RESIDENT_FRAME_BUDGET / 2));
    }

    private static float dimension(float value) { return Float.isFinite(value) ? Math.max(16, Math.min(2000, value)) : 160; }

    private static RemoteViews sized(Context c, int id, float width, float height, long seed, int budget) {
        width = dimension(width); height = dimension(height);
        int layout = !Prefs.enabled(c) ? R.layout.mozart_widget_preview
                : Build.VERSION.SDK_INT >= 31 && Build.VERSION.SDK_INT <= 32
                    ? (Prefs.widgetInterval(c) == 125 ? R.layout.mozart_widget_stored_12 : R.layout.mozart_widget_stored_12_smooth)
                : Build.VERSION.SDK_INT >= 33
                    ? (Prefs.widgetInterval(c) == 125 ? R.layout.mozart_widget_stored : R.layout.mozart_widget_stored_smooth)
                    : (Prefs.widgetInterval(c) == 125 ? R.layout.mozart_widget : R.layout.mozart_widget_smooth);
        // A separate layout also replaces a pre-2.7 host view on upgrade, dropping its old
        // service adapter and any pending reconnection callbacks.
        RemoteViews views = new RemoteViews(c.getPackageName(), layout);
        views.setContentDescription(R.id.widget_area, "Mozart · widget " + id);
        views.setInt(R.id.widget_area, "setBackgroundResource", Prefs.border(c) ? R.drawable.widget_border : android.R.color.transparent);
        boolean locked = Prefs.touchLocked(c);
        PendingIntent click = locked ? null : PendingIntent.getActivity(c, id,
                new Intent(c, MainActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Always apply the action, including null, to remove an existing root click listener.
        views.setOnClickPendingIntent(R.id.widget_area, click);
        if (Prefs.enabled(c)) {
            String date = (Build.VERSION.SDK_INT >= 31 || Prefs.dates(c)
                    ? LocalDate.now() : LocalDate.of(2000, 1, 1)).toString();
            if (Build.VERSION.SDK_INT >= 31) {
                WidgetTimeline.Frame[] timeline = ResidentTimeline.create(seed, width, height,
                        Prefs.size(c) * 1.23f, 30 * (Prefs.tempo(c) == 0 ? .65f : Prefs.tempo(c) == 2 ? 1.5f : 1),
                        Prefs.dates(c), LocalDate.parse(date), Prefs.widgetInterval(c), budget);
                if (Build.VERSION.SDK_INT <= 32) {
                    // Android 12 serializes ApplicationInfo again for every collection item.
                    // Nested stable RemoteViews share it. One flat ViewFlipper (one clock),
                    // with all images installed locally, has neither Binder gaps nor timer drift.
                    views.removeAllViews(R.id.widget_animation);
                    for (int i = 0; i < timeline.length; i++)
                        views.addStableView(R.id.widget_animation, frame(c, timeline[i], width, height, false), i);
                } else {
                    RemoteViews.RemoteCollectionItems.Builder items = new RemoteViews.RemoteCollectionItems.Builder()
                            .setHasStableIds(true).setViewTypeCount(1);
                    for (int i = 0; i < timeline.length; i++)
                        items.addItem(i, frame(c, timeline[i], width, height));
                    // Android 13+ uses a synchronous, resident adapter. No remote loading view, cache miss,
                    // service reconnection or delayed data-set invalidation can blank the next image.
                    views.setRemoteAdapter(R.id.widget_animation, items.build());
                }
            } else {
                Intent frames = new Intent(c, WidgetFramesService.class)
                        .putExtra("width", width).putExtra("height", height).putExtra("seed", seed)
                        .putExtra("size", Prefs.size(c) * 1.23f).putExtra("tempo", Prefs.tempo(c))
                        .putExtra("dates", Prefs.dates(c)).putExtra("interval", Prefs.widgetInterval(c)).putExtra("date", date)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                frames.setData(new Uri.Builder().scheme("mozart").authority("widget").appendPath(Integer.toString(id))
                        .appendQueryParameter("w", Float.toString(width)).appendQueryParameter("h", Float.toString(height))
                        .appendQueryParameter("seed", Long.toString(seed)).appendQueryParameter("version", "2.4")
                        .appendQueryParameter("size", Integer.toString(Prefs.size(c)))
                        .appendQueryParameter("tempo", Integer.toString(Prefs.tempo(c)))
                        .appendQueryParameter("interval", Integer.toString(Prefs.widgetInterval(c)))
                        .appendQueryParameter("dates", Boolean.toString(Prefs.dates(c)))
                        .appendQueryParameter("date", date).build());
                views.setRemoteAdapter(R.id.widget_animation, frames);
            }
            // Older Android collection handlers dereference a null template. A no-op broadcast
            // also blocks taps on already cached items, without replacing/restarting the adapter.
            PendingIntent template = locked ? PendingIntent.getBroadcast(c, id,
                    new Intent(c, MozartWidgetProvider.class).setAction(MozartWidgetProvider.IGNORE_TOUCH),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE) : click;
            if (Build.VERSION.SDK_INT < 31 || Build.VERSION.SDK_INT >= 33)
                views.setPendingIntentTemplate(R.id.widget_animation, template);
        }
        return views;
    }

    static RemoteViews frame(Context c, WidgetTimeline.Frame frame, float width, float height) {
        return frame(c, frame, width, height, true);
    }

    private static RemoteViews frame(Context c, WidgetTimeline.Frame frame, float width, float height, boolean collection) {
        RemoteViews view = new RemoteViews(c.getPackageName(), R.layout.mozart_widget_frame);
        // Mirror the drawable inside its cell; scaling the full ImageView would move Mozart.
        view.setImageViewResource(R.id.widget_frame, PixelSprites.drawable(frame.pose, frame.sprite, false));
        view.setInt(R.id.widget_frame, "setLayoutDirection", frame.left ? android.view.View.LAYOUT_DIRECTION_RTL : android.view.View.LAYOUT_DIRECTION_LTR);
        // The main image's default alpha is always 255; no action ever changes it.
        place(c, view, R.id.widget_frame, frame.x, frame.y, frame.size, width, height);
        view.setInt(R.id.widget_note, "setImageAlpha", frame.noteAlpha);
        if (frame.noteAlpha > 0) {
            view.setImageViewResource(R.id.widget_note, PixelSprites.note(frame.noteVariant));
            place(c, view, R.id.widget_note, frame.noteX, frame.noteY, frame.noteSize, width, height);
        }
        if (collection) view.setOnClickFillInIntent(R.id.widget_frame_area, new Intent());
        return view;
    }

    private static void place(Context c, RemoteViews view, int id, float x, float y, float size, float width, float height) {
        float density = c.getResources().getDisplayMetrics().density;
        view.setViewPadding(id, Math.round(x * density), Math.round(y * density),
                Math.max(0, Math.round((width - x - size) * density)),
                Math.max(0, Math.round((height - y - size) * density)));
    }
}
