package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;

/**
 * Buscador reutilizable con fondo {@code colorSurfaceVariant}, radio 8 dp y debounce de 250 ms
 * ({@code docs/design.md} §7).
 */
public class SearchField extends LinearLayout {

    public static final long DEFAULT_DEBOUNCE_MILLIS = 250L;

    public interface OnQueryChangeListener {
        void onQueryChanged(@NonNull String query);
    }

    private final EditText inputView;
    private final ImageButton clearButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long debounceMillis = DEFAULT_DEBOUNCE_MILLIS;
    @Nullable
    private OnQueryChangeListener listener;
    @Nullable
    private Runnable pendingDispatch;

    public SearchField(@NonNull Context context) {
        this(context, null);
    }

    public SearchField(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchField(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        applyBackground();

        LayoutInflater.from(context).inflate(R.layout.view_search_field, this, true);
        inputView = findViewById(R.id.searchFieldInput);
        clearButton = findViewById(R.id.searchFieldClear);

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SearchField);
            try {
                String hint = array.getString(R.styleable.SearchField_searchHint);
                if (hint != null) {
                    inputView.setHint(hint);
                }
            } finally {
                array.recycle();
            }
        }

        clearButton.setOnClickListener(v -> clearQuery());
        inputView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                dispatchImmediately();
                return true;
            }
            return false;
        });
        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s == null ? "" : s.toString();
                clearButton.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
                scheduleDispatch(text);
            }
        });
    }

    private void applyBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(getResources().getDimension(R.dimen.radius_s));
        int fillColor = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurfaceVariant, Color.LTGRAY);
        int outlineColor = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutline, Color.GRAY);
        bg.setColor(fillColor);
        bg.setStroke(getResources().getDimensionPixelSize(R.dimen.stroke_width), outlineColor);
        setBackground(bg);
    }

    public void setHint(@StringRes int hintRes) {
        inputView.setHint(hintRes);
    }

    public void setDebounceMillis(long debounceMillis) {
        this.debounceMillis = Math.max(0L, debounceMillis);
    }

    public void setOnQueryChangeListener(@Nullable OnQueryChangeListener listener) {
        this.listener = listener;
    }

    @NonNull
    public String getQuery() {
        CharSequence cs = inputView.getText();
        return cs == null ? "" : cs.toString();
    }

    public void clearQuery() {
        cancelPending();
        inputView.setText("");
        if (listener != null) {
            listener.onQueryChanged("");
        }
    }

    private void dispatchImmediately() {
        cancelPending();
        if (listener != null) {
            listener.onQueryChanged(getQuery());
        }
    }

    private void scheduleDispatch(@NonNull String text) {
        cancelPending();
        if (debounceMillis == 0L) {
            if (listener != null) {
                listener.onQueryChanged(text);
            }
            return;
        }
        pendingDispatch = () -> {
            if (listener != null) {
                listener.onQueryChanged(text);
            }
        };
        handler.postDelayed(pendingDispatch, debounceMillis);
    }

    private void cancelPending() {
        if (pendingDispatch != null) {
            handler.removeCallbacks(pendingDispatch);
            pendingDispatch = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelPending();
        super.onDetachedFromWindow();
    }
}
