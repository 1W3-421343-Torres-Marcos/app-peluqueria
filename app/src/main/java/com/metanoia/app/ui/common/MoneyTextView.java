package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

import com.metanoia.app.R;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Money;

/**
 * Vista de texto tabular para montos en pesos argentinos ({@code $ 12.345,50}).
 */
public class MoneyTextView extends AppCompatTextView {

    public MoneyTextView(@NonNull Context context) {
        this(context, null);
    }

    public MoneyTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public MoneyTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Metanoia_Money);
        long initialCents = 0L;
        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MoneyTextView);
            try {
                initialCents = array.getInteger(R.styleable.MoneyTextView_moneyCents, 0);
            } finally {
                array.recycle();
            }
        }
        setMoneyCents(initialCents);
    }

    public void setMoney(@NonNull Money money) {
        setText(Formats.formatMoney(money));
    }

    public void setMoneyCents(long cents) {
        setText(Formats.formatMoney(cents));
    }
}
