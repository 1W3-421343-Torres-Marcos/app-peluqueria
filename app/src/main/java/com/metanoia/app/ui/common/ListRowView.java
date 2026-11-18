package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.R;

/**
 * Ítem estándar de lista (≥ 64 dp) con título, subtítulo, valor derecho, chevron y divisor.
 */
public class ListRowView extends LinearLayout {

    private final TextView titleView;
    private final TextView subtitleView;
    private final TextView trailingView;
    private final ImageView chevronView;
    private final View dividerView;

    public ListRowView(@NonNull Context context) {
        this(context, null);
    }

    public ListRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListRowView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setClickable(true);
        setFocusable(true);
        applySelectableBackground(context);

        LayoutInflater.from(context).inflate(R.layout.view_list_row, this, true);
        titleView = findViewById(R.id.listRowTitle);
        subtitleView = findViewById(R.id.listRowSubtitle);
        trailingView = findViewById(R.id.listRowTrailing);
        chevronView = findViewById(R.id.listRowChevron);
        dividerView = findViewById(R.id.listRowDivider);

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ListRowView);
            try {
                String title = array.getString(R.styleable.ListRowView_rowTitle);
                if (title != null) {
                    setTitle(title);
                }
                setSubtitle(array.getString(R.styleable.ListRowView_rowSubtitle));
                setTrailingText(array.getString(R.styleable.ListRowView_rowTrailingText));
                setShowChevron(array.getBoolean(R.styleable.ListRowView_rowShowChevron, false));
                setShowDivider(array.getBoolean(R.styleable.ListRowView_rowShowDivider, true));
            } finally {
                array.recycle();
            }
        }
    }

    private void applySelectableBackground(Context context) {
        TypedValue outValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            setBackgroundResource(outValue.resourceId);
        }
    }

    public void setTitle(@NonNull CharSequence title) {
        titleView.setText(title);
    }

    public void setSubtitle(@Nullable CharSequence subtitle) {
        if (subtitle == null || subtitle.length() == 0) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }
    }

    public void setTrailingText(@Nullable CharSequence trailing) {
        if (trailing == null || trailing.length() == 0) {
            trailingView.setVisibility(View.GONE);
        } else {
            trailingView.setText(trailing);
            trailingView.setVisibility(View.VISIBLE);
        }
    }

    public void setShowChevron(boolean showChevron) {
        chevronView.setVisibility(showChevron ? View.VISIBLE : View.GONE);
    }

    public void setShowDivider(boolean showDivider) {
        dividerView.setVisibility(showDivider ? View.VISIBLE : View.GONE);
    }
}
