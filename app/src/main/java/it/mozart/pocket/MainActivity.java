package it.mozart.pocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private ScrollView scroll;
    private PetSceneView preview;
    private TextView sizeLabel;
    private int savedScroll;
    private boolean waitingForWidget;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        waitingForWidget = state != null && state.getBoolean("waitingForWidget", false);
        Prefs.migrate(this);
    }

    @Override public void onResume() {
        super.onResume();
        boolean widgetPlaced = waitingForWidget && MozartWidgetProvider.ids(this).length > 0;
        waitingForWidget = false;
        render();
        if (widgetPlaced && Prefs.enabled(this)) goHome();
    }

    @Override public void onPause() {
        if (preview != null) preview.setRunning(false);
        if (scroll != null) savedScroll = scroll.getScrollY();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("waitingForWidget", waitingForWidget);
        super.onSaveInstanceState(state);
    }

    private void render() {
        if (preview != null) preview.setRunning(false);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(Ui.BG);
        LinearLayout page = Ui.column(this);
        page.setPadding(dp(22), dp(18), dp(22), dp(32));
        scroll.addView(page);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top; bottom = bars.bottom;
            } else { top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom(); }
            view.setPadding(0, top, 0, bottom);
            return insets;
        });
        scroll.requestApplyInsets();

        LinearLayout masthead = Ui.row(this);
        TextView music = Ui.text(this, "♫", 27, Ui.RED);
        masthead.addView(music, new LinearLayout.LayoutParams(dp(40), -2));
        TextView name = Ui.text(this, getString(R.string.brand_name), 20, Ui.INK);
        name.setTypeface(Typeface.create("serif", Typeface.BOLD));
        masthead.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView status = Ui.text(this, getString(MozartWidgetProvider.ids(this).length == 0 ? R.string.status_ready
                : Prefs.enabled(this) ? R.string.status_home : R.string.status_paused), 11,
                Prefs.enabled(this) ? Ui.GREEN : Ui.MUTED);
        masthead.addView(status);
        Ui.add(page, masthead, 0);
        Ui.add(page, Ui.label(this, getString(R.string.intro_label)), 31);
        TextView headline = Ui.text(this, getString(R.string.intro_title), 34, Ui.INK);
        headline.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        headline.setLineSpacing(dp(1), 1);
        Ui.add(page, headline, 12);
        Ui.add(page, Ui.text(this, getString(R.string.intro_description), 14, Ui.MUTED), 13);

        LinearLayout stage = Ui.column(this);
        stage.setPadding(dp(16), dp(15), dp(16), dp(14));
        GradientDrawable stageBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF0E7DD, 0xFFF3E3E2, 0xFFE8DED9});
        stageBackground.setCornerRadius(dp(22));
        stage.setBackground(stageBackground);
        LinearLayout stageLabel = Ui.row(this);
        stageLabel.addView(Ui.text(this, "●", 9, Ui.GREEN), new LinearLayout.LayoutParams(dp(17), -2));
        stageLabel.addView(Ui.label(this, getString(R.string.preview_label)), new LinearLayout.LayoutParams(0, -2, 1));
        stageLabel.addView(Ui.text(this, getString(R.string.preview_style), 10, Ui.MUTED));
        stage.addView(stageLabel);
        preview = new PetSceneView(this);
        preview.setPreview(true);
        stage.addView(preview, new LinearLayout.LayoutParams(-1, dp(195)));
        TextView stageCaption = Ui.text(this, getString(R.string.preview_caption), 11, Ui.MUTED);
        stageCaption.setGravity(Gravity.CENTER);
        stage.addView(stageCaption);
        Ui.add(page, stage, 24);

        boolean hasWidget = MozartWidgetProvider.ids(this).length > 0;
        Button primary = Ui.button(this, getString(!hasWidget ? R.string.add_widget
                : Prefs.enabled(this) ? R.string.pause_mozart : R.string.resume_mozart), true);
        primary.setOnClickListener(v -> toggle());
        Ui.add(page, primary, 19);
        Ui.add(page, Ui.text(this, getString(R.string.resize_hint), 12, Ui.MUTED), 10);
        if (hasWidget) {
            Button widget = Ui.button(this, getString(R.string.add_another_widget), false);
            widget.setOnClickListener(v -> addWidget());
            Ui.add(page, widget, 10);
        }
        settingSwitch(page, getString(R.string.touch_lock_title),
                getString(R.string.touch_lock_description), "touchLock", false);

        Ui.add(page, Ui.label(this, getString(R.string.activities_label)), 30);
        Ui.add(page, Ui.text(this, getString(R.string.activities_hint), 12, Ui.MUTED), 9);
        MotionEngine.Pose[] poses = MotionEngine.Pose.values();
        for (int row = 0; row < (poses.length + 2) / 3; row++) {
            LinearLayout chips = Ui.row(this);
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (index >= poses.length) break;
                Button chip = Ui.button(this, poses[index].label, false);
                chip.setTextSize(11); chip.setPadding(dp(4), dp(10), dp(4), dp(10));
                chip.setOnClickListener(v -> {
                    preview.demo(poses[index]);
                    scroll.smoothScrollTo(0, Math.max(0, stage.getTop() - dp(12)));
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
                if (col > 0) p.leftMargin = dp(6);
                chips.addView(chip, p);
            }
            Ui.add(page, chips, 9);
        }

        LinearLayout settings = Ui.card(this);
        settings.addView(Ui.text(this, getString(R.string.settings_title), 18, Ui.INK));
        sizeLabel = Ui.text(this, getString(R.string.size_label, Prefs.size(this)), 13, Ui.INK);
        Ui.add(settings, sizeLabel, 19);
        SeekBar size = new SeekBar(this); size.setMax(96); size.setProgress(Prefs.size(this) - 64);
        size.setContentDescription(getString(R.string.size_description));
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean user) {
                if (!user) return;
                Prefs.get(MainActivity.this).edit().putInt("size", progress + 64).apply();
                sizeLabel.setText(getString(R.string.size_label, progress + 64)); refresh();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        Ui.add(settings, size, 8);
        Ui.add(settings, Ui.text(this, getString(R.string.size_hint), 11, Ui.MUTED), 3);
        Ui.add(settings, Ui.text(this, getString(R.string.tempo_title), 13, Ui.INK), 21);
        LinearLayout tempos = Ui.row(this);
        String[] tempoNames = {getString(R.string.tempo_calm), getString(R.string.tempo_natural), getString(R.string.tempo_lively)};
        for (int i = 0; i < 3; i++) {
            final int selected = i;
            Button b = Ui.button(this, tempoNames[i], Prefs.tempo(this) == i);
            b.setTextSize(11);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) p.leftMargin = dp(5);
            tempos.addView(b, p);
            b.setOnClickListener(v -> {
                Prefs.get(this).edit().putInt("tempo", selected).apply(); refresh();
                for (int j = 0; j < tempos.getChildCount(); j++) {
                    Button child = (Button) tempos.getChildAt(j);
                    child.setBackground(Ui.rounded(this, j == selected ? Ui.RED : 0xFFF2E7E3, 14, false));
                    child.setTextColor(j == selected ? 0xFFFFFBF5 : Ui.RED);
                }
            });
        }
        Ui.add(settings, tempos, 10);
        settingSwitch(settings, getString(R.string.dates_title), getString(R.string.dates_description), "dates", true);
        settingSwitch(settings, getString(R.string.eco_title), getString(R.string.eco_description), "eco", true);
        settingSwitch(settings, getString(R.string.border_title), getString(R.string.border_description), "border", false);
        Ui.add(page, settings, 18);

        LinearLayout birthday = Ui.card(this);
        birthday.addView(Ui.label(this, getString(R.string.birthday_label)));
        TextView partyTitle = Ui.text(this, getString(R.string.birthday_title), 21, Ui.INK);
        partyTitle.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        Ui.add(birthday, partyTitle, 11);
        Ui.add(birthday, Ui.text(this, getString(R.string.birthday_description), 12, Ui.MUTED), 9);
        Ui.add(page, birthday, 18);

        TextView footer = Ui.text(this, getString(R.string.footer), 11, Ui.MUTED);
        footer.setGravity(Gravity.CENTER);
        Ui.add(page, footer, 25);
        scroll.post(() -> scroll.scrollTo(0, savedScroll));
    }

    private void settingSwitch(LinearLayout parent, String title, String subtitle, String key, boolean fallback) {
        LinearLayout row = Ui.row(this);
        LinearLayout words = Ui.column(this);
        words.addView(Ui.text(this, title, 13, Ui.INK));
        Ui.add(words, Ui.text(this, subtitle, 11, Ui.MUTED), 5);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.rightMargin = dp(12);
        row.addView(words, p);
        Switch toggle = new Switch(this);
        toggle.setContentDescription(title);
        toggle.setTextOn(getString(R.string.switch_on));
        toggle.setTextOff(getString(R.string.switch_off));
        toggle.setChecked(Prefs.get(this).getBoolean(key, fallback));
        toggle.setOnCheckedChangeListener((buttonView, checked) -> {
            Prefs.get(this).edit().putBoolean(key, checked).apply(); refresh();
        });
        row.addView(toggle);
        Ui.add(parent, row, 21);
    }

    private void refresh() {
        if (preview != null) preview.refresh();
        MozartWidgetProvider.updateAll(this);
    }

    private void addWidget() {
        Prefs.enabled(this, true);
        MozartWidgetProvider.updateAll(this);
        waitingForWidget = true;
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (manager.isRequestPinAppWidgetSupported()) {
            if (!MozartWidgetProvider.requestPin(this)) {
                cancelEmptyWidgetRequest();
                Toast.makeText(this, R.string.add_widget_hint, Toast.LENGTH_LONG).show();
            }
        } else new AlertDialog.Builder(this).setTitle(R.string.add_widget_title)
                .setMessage(R.string.add_widget_instructions)
                .setPositiveButton(R.string.go_home, (d, w) -> goHome())
                .setNegativeButton(R.string.cancel, (d, w) -> cancelEmptyWidgetRequest())
                .setOnCancelListener(d -> cancelEmptyWidgetRequest()).show();
    }

    private void cancelEmptyWidgetRequest() {
        waitingForWidget = false;
        render();
    }

    private void toggle() {
        if (MozartWidgetProvider.ids(this).length == 0) { addWidget(); return; }
        Prefs.enabled(this, !Prefs.enabled(this));
        MozartWidgetProvider.updateAll(this);
        render();
        if (Prefs.enabled(this)) goHome();
    }

    private void goHome() {
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private int dp(float n) { return Ui.dp(this, n); }
}
