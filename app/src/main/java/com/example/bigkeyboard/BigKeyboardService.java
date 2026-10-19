package com.example.bigkeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple on-screen keyboard with very large keys, meant for car head units.
 * The whole UI is built in code, so there are no layout XML files.
 */
public class BigKeyboardService extends InputMethodService {

    static final String PREFS = "bigkb";
    static final String PREF_HEIGHT_DP = "key_height_dp";
    static final int DEFAULT_HEIGHT_DP = 64;

    private static final int LAYER_LETTERS = 0;
    private static final int LAYER_SYMBOLS = 1;

    // Special key codes are upper-case words; everything else is the literal character.
    private static final String[][] LETTERS = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
            {"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"},
            {"SYM", "SWITCH", ",", "SPACE", ".", "ENTER"}
    };

    private static final String[][] SYMBOLS = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"@", "#", "$", "%", "&", "-", "+", "(", ")", "/"},
            {"*", "\"", "'", ":", ";", "!", "?", "=", "_"},
            {"~", "`", "|", "\\", "[", "]", "{", "}", "DEL"},
            {"ABC", "SWITCH", ",", "SPACE", ".", "ENTER"}
    };

    private static final int COLOR_BG = 0xFF1B1D1F;
    private static final int COLOR_KEY = 0xFF3F4347;
    private static final int COLOR_KEY_SPECIAL = 0xFF2B2E31;
    private static final int COLOR_PRESSED = 0xFF6AA9F0;
    private static final int COLOR_SHIFT_ON = 0xFF2F6FBF;
    private static final int COLOR_CAPS_ON = 0xFFC77B1E;

    private LinearLayout root;
    private int layer = LAYER_LETTERS;
    private boolean shift = false;
    private boolean capsLock = false;
    private long lastShiftTap = 0;
    private int keyHeightDp = DEFAULT_HEIGHT_DP;

    private final List<TextView> letterKeys = new ArrayList<TextView>();
    private TextView shiftKey;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable repeatDelete = new Runnable() {
        @Override
        public void run() {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            handler.postDelayed(this, 50);
        }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    public View onCreateInputView() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        int pad = dp(6);
        root.setPadding(pad, pad, pad, pad);
        loadPrefs();
        buildKeys();
        return root;
    }

    /** Never take over the whole screen in landscape; keep the app visible. */
    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        loadPrefs();
        shift = false;
        capsLock = false;
        int cls = (info == null) ? 0 : (info.inputType & InputType.TYPE_MASK_CLASS);
        layer = (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE)
                ? LAYER_SYMBOLS : LAYER_LETTERS;
        if (root != null) {
            buildKeys();
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        handler.removeCallbacks(repeatDelete);
        super.onFinishInputView(finishingInput);
    }

    private void loadPrefs() {
        keyHeightDp = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(PREF_HEIGHT_DP, DEFAULT_HEIGHT_DP);
    }

    // ---------------------------------------------------------------- building the UI

    private void buildKeys() {
        root.removeAllViews();
        letterKeys.clear();
        shiftKey = null;

        String[][] rows = (layer == LAYER_LETTERS) ? LETTERS : SYMBOLS;
        for (String[] row : rows) {
            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.HORIZONTAL);
            rowView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(keyHeightDp)));

            float total = 0f;
            for (String code : row) {
                total += weightFor(code);
            }
            float sidePad = (10f - total) / 2f;
            if (sidePad > 0.01f) {
                rowView.addView(spacer(sidePad));
            }
            for (String code : row) {
                rowView.addView(makeKey(code));
            }
            if (sidePad > 0.01f) {
                rowView.addView(spacer(sidePad));
            }
            root.addView(rowView);
        }
        refreshShiftState();
    }

    private View spacer(float weight) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, weight));
        return v;
    }

    private TextView makeKey(final String code) {
        boolean special = isSpecial(code);
        final TextView tv = new TextView(this);
        tv.setTag(code);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                special ? keyHeightDp * 0.28f : keyHeightDp * 0.42f);
        tv.setText(labelFor(code));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, weightFor(code));
        int m = dp(3);
        lp.setMargins(m, m, m, m);
        tv.setLayoutParams(lp);
        tv.setBackground(keyBackground(special ? COLOR_KEY_SPECIAL : COLOR_KEY));

        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        handleKey(code);
                        if ("DEL".equals(code)) {
                            handler.removeCallbacks(repeatDelete);
                            handler.postDelayed(repeatDelete, 400);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        handler.removeCallbacks(repeatDelete);
                        return true;
                    default:
                        return true;
                }
            }
        });

        if ("SHIFT".equals(code)) {
            shiftKey = tv;
        } else if (layer == LAYER_LETTERS && code.length() == 1
                && Character.isLetter(code.charAt(0))) {
            letterKeys.add(tv);
        }
        return tv;
    }

    private Drawable keyBackground(int color) {
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(color);
        normal.setCornerRadius(dp(8));
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(COLOR_PRESSED);
        pressed.setCornerRadius(dp(8));
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    private void refreshShiftState() {
        boolean upper = shift || capsLock;
        for (TextView tv : letterKeys) {
            String code = (String) tv.getTag();
            tv.setText(upper ? code.toUpperCase(Locale.ROOT) : code);
        }
        if (shiftKey != null) {
            shiftKey.setText(labelFor("SHIFT"));
            int color = capsLock ? COLOR_CAPS_ON : (shift ? COLOR_SHIFT_ON : COLOR_KEY_SPECIAL);
            shiftKey.setBackground(keyBackground(color));
        }
    }

    private boolean isSpecial(String code) {
        return code.length() > 1;
    }

    private float weightFor(String code) {
        switch (code) {
            case "SHIFT":
            case "DEL":
            case "SYM":
            case "ABC":
            case "ENTER":
                return 1.5f;
            case "SPACE":
                return 4f;
            default:
                return 1f;
        }
    }

    private String labelFor(String code) {
        switch (code) {
            case "SHIFT":
                return capsLock ? "CAPS" : "Shift";
            case "DEL":
                return "Del";
            case "ENTER":
                return "Enter";
            case "SPACE":
                return "";
            case "SYM":
                return "?123";
            case "ABC":
                return "ABC";
            case "SWITCH":
                return "IME";
            default:
                return code;
        }
    }

    // ---------------------------------------------------------------- key handling

    private void handleKey(String code) {
        switch (code) {
            case "SHIFT": {
                long now = SystemClock.uptimeMillis();
                if (capsLock) {
                    capsLock = false;
                    shift = false;
                } else if (shift && now - lastShiftTap < 400) {
                    capsLock = true;
                } else {
                    shift = !shift;
                }
                lastShiftTap = now;
                refreshShiftState();
                return;
            }
            case "SYM":
                switchLayer(LAYER_SYMBOLS);
                return;
            case "ABC":
                switchLayer(LAYER_LETTERS);
                return;
            case "SWITCH":
                switchKeyboard();
                return;
            default:
                break;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }

        switch (code) {
            case "DEL":
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                break;
            case "ENTER":
                handleEnter(ic);
                break;
            case "SPACE":
                ic.commitText(" ", 1);
                break;
            default:
                String out = code;
                if (layer == LAYER_LETTERS && (shift || capsLock)
                        && code.length() == 1 && Character.isLetter(code.charAt(0))) {
                    out = code.toUpperCase(Locale.ROOT);
                }
                ic.commitText(out, 1);
                if (shift && !capsLock) {
                    shift = false;
                    refreshShiftState();
                }
                break;
        }
    }

    private void handleEnter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null && (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (action != EditorInfo.IME_ACTION_NONE
                    && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action);
                return;
            }
        }
        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
    }

    private void switchLayer(int newLayer) {
        layer = newLayer;
        handler.post(new Runnable() {
            @Override
            public void run() {
                buildKeys();
            }
        });
    }

    /** Go back to the car's original keyboard (or let the user pick one). */
    private void switchKeyboard() {
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                if (switchToNextInputMethod(false)) {
                    return;
                }
            } catch (Throwable ignored) {
                // fall through to the picker
            }
        }
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}
