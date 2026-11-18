package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.metanoia.app.R;

/**
 * Banner de alerta en color acento para avisos de backup vencido y stock bajo.
 */
public class AlertBanner extends LinearLayout {

    private final TextView messageView;
    private final MaterialButton actionButton;

    public AlertBanner(@NonNull Context context) {
        this(context, null);
    }

    public AlertBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlertBanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padH = getResources().getDimensionPixelSize(R.dimen.space_m);
        int padV = getResources().getDimensionPixelSize(R.dimen.space_s);
        setPadding(padH, padV, padH, padV);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(getResources().getDimension(R.dimen.radius_m));
        background.setColor(ContextCompat.getColor(context, R.color.accent_container));
        setBackground(background);

        LayoutInflater.from(context).inflate(R.layout.view_alert_banner, this, true);
        messageView = findViewById(R.id.alertBannerMessage);
        actionButton = findViewById(R.id.alertBannerAction);

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AlertBanner);
            try {
                String message = array.getString(R.styleable.AlertBanner_bannerMessage);
                if (message != null) {
                    setMessage(message);
                }
                String actionText = array.getString(R.styleable.AlertBanner_bannerActionText);
                if (actionText != null && !actionText.isEmpty()) {
                    setAction(actionText, null);
                }
            } finally {
                array.recycle();
            }
        }
    }

    public void setMessage(@StringRes int resId) {
        messageView.setText(resId);
    }

    public void setMessage(@NonNull CharSequence text) {
        messageView.setText(text);
    }

    public void setAction(@StringRes int resId, @Nullable OnClickListener listener) {
        setAction(getContext().getString(resId), listener);
    }

    public void setAction(@Nullable CharSequence text, @Nullable OnClickListener listener) {
        if (text == null || text.length() == 0) {
            actionButton.setVisibility(View.GONE);
            actionButton.setOnClickListener(null);
        } else {
            actionButton.setText(text);
            actionButton.setVisibility(View.VISIBLE);
            actionButton.setOnClickListener(listener);
        }
    }
}
