package com.example.bigkeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Launcher screen: enable the keyboard, pick it, adjust key height, and test it.
 * Everything is large so it is easy to hit on a car touchscreen.
 */
public class SettingsActivity extends Activity {

    private static final int MIN_HEIGHT_DP = 40;
    private static final int MAX_HEIGHT_DP = 140;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sp =
                getSharedPreferences(BigKeyboardService.PREFS, Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(col);

        col.addView(label("Big Keyboard setup", 30));

        col.addView(bigButton("1. Enable keyboard", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this,
                            "Can't open keyboard settings on this system",
                            Toast.LENGTH_LONG).show();
                }
            }
        }));

        col.addView(bigButton("2. Switch to Big Keyboard", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            }
        }));

        int current = sp.getInt(BigKeyboardService.PREF_HEIGHT_DP,
                BigKeyboardService.DEFAULT_HEIGHT_DP);
        final TextView heightLabel = label("Key height: " + current + " dp", 22);
        heightLabel.setPadding(0, dp(24), 0, 0);
        col.addView(heightLabel);

        SeekBar bar = new SeekBar(this);
        bar.setMax(MAX_HEIGHT_DP - MIN_HEIGHT_DP);
        bar.setProgress(current - MIN_HEIGHT_DP);
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                int value = progress + MIN_HEIGHT_DP;
                sp.edit().putInt(BigKeyboardService.PREF_HEIGHT_DP, value).apply();
                heightLabel.setText("Key height: " + value + " dp");
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        col.addView(bar);

        TextView testLabel = label("Test field (tap to type):", 20);
        testLabel.setPadding(0, dp(24), 0, dp(8));
        col.addView(testLabel);

        EditText test = new EditText(this);
        test.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        test.setHint("Type here");
        col.addView(test);

        setContentView(scroll);
    }

    private TextView label(String text, int sizeDp) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        return tv;
    }

    private Button bigButton(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(80));
        lp.topMargin = dp(16);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
