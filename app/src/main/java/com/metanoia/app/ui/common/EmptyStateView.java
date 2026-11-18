package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.button.MaterialButton;
import com.metanoia.app.R;

/**
 * Vista estándar de estado vacío con ícono tijera de línea, mensaje y acción secundaria opcional.
 */
public class EmptyStateView extends LinearLayout {

    private final TextView messageView;
    private final MaterialButton actionButton;

    public EmptyStateView(@NonNull Context context) {
        this(context, null);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        int pad = getResources().getDimensionPixelSize(R.dimen.space_l);
        setPadding(pad, pad, pad, pad);

        LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true);
        messageView = findViewById(R.id.emptyStateMessage);
        actionButton = findViewById(R.id.emptyStateAction);

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.EmptyStateView);
            try {
                String message = array.getString(R.styleable.EmptyStateView_emptyMessage);
                if (message != null) {
                    setMessage(message);
                }
                String actionText = array.getString(R.styleable.EmptyStateView_emptyActionText);
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
