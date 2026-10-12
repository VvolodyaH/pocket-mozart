package it.mozart.pocket;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFFF8F4EF, PAPER = 0xFFFFFCF8, INK = 0xFF302B36,
            MUTED = 0xFF847D83, RED = 0xFFAE373C, LINE = 0xFFE6DED5, GREEN = 0xFF507B61;
    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    static TextView text(Context c, String text, float size, int color) {
        TextView t = new TextView(c); t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setIncludeFontPadding(false); t.setLineSpacing(dp(c, 3), 1); return t;
    }
    static TextView label(Context c, String text) {
        TextView t = text(c, text, 10, MUTED);
        t.setLetterSpacing(0.14f); t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return t;
    }
    static GradientDrawable rounded(Context c, int color, float radius, boolean stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius));
        if (stroke) d.setStroke(dp(c, 1), LINE); return d;
    }
    static LinearLayout card(Context c) {
        LinearLayout l = column(c); l.setPadding(dp(c, 18), dp(c, 18), dp(c, 18), dp(c, 18));
        l.setBackground(rounded(c, PAPER, 22, true)); return l;
    }
    static Button button(Context c, String text, boolean primary) {
        Button b = new Button(c); b.setText(text); b.setAllCaps(false); b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(primary ? 0xFFFFFBF5 : RED); b.setMinHeight(dp(c, 48));
        b.setMinimumHeight(dp(c, 48)); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        b.setBackground(rounded(c, primary ? RED : 0xFFF2E7E3, 14, false)); return b;
    }
    static void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(parent.getContext(), top); parent.addView(child, p);
    }
    static void gap(LinearLayout parent, int height) {
        parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(1, dp(parent.getContext(), height)));
    }
}
