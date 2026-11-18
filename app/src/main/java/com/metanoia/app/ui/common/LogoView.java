package com.metanoia.app.ui.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;

/**
 * Vista de marca de Metanoia con soporte para logo vectorial reemplazable.
 *
 * <p>Slot reemplazable: al agregar {@code app/src/main/res/drawable/logo_metanoia.xml}
 * (VectorDrawable derivado de {@code assets/branding/logo.svg}), esta vista lo carga
 * automáticamente con tinte {@code ?attr/colorOnSurface} sin modificar código Java.
 * Mientras ese recurso no exista, muestra el placeholder tipográfico de {@code docs/design.md} §6.
 */
public class LogoView extends FrameLayout {

    public static final String LOGO_DRAWABLE_NAME = "logo_metanoia";
    public static final int SIZE_HEADER = 0;
    public static final int SIZE_SPLASH = 1;

    private static final float HEADER_TITLE_SP = 18f;
    private static final float SPLASH_TITLE_SP = 28f;
    private static final float SUBTITLE_RATIO = 0.35f;
    private static final float TITLE_LETTER_SPACING = 0.12f;

    public LogoView(@NonNull Context context) {
        this(context, null);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int sizeVariant = readSizeVariant(context, attrs);
        init(context, sizeVariant);
    }

    private int readSizeVariant(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return SIZE_HEADER;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LogoView);
        try {
            return typedArray.getInt(R.styleable.LogoView_logoSize, SIZE_HEADER);
        } finally {
            typedArray.recycle();
        }
    }

    private void init(Context context, int sizeVariant) {
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.logo_min_height));
        setContentDescription(context.getString(R.string.cd_metanoia_logo));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        int onSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        int logoResId = resolveLogoDrawableId(context);

        if (logoResId != 0) {
            addVectorLogo(context, logoResId, sizeVariant, onSurface);
        } else {
            addTypographicPlaceholder(context, sizeVariant, onSurface);
        }
    }

    @SuppressLint("DiscouragedApi")
    private int resolveLogoDrawableId(Context context) {
        // Permite reemplazar el logo únicamente colocando res/drawable/logo_metanoia.xml.
        return context.getResources().getIdentifier(
                LOGO_DRAWABLE_NAME, "drawable", context.getPackageName());
    }

    private void addVectorLogo(Context context, int logoResId, int sizeVariant, int tintColor) {
        ImageView imageView = new ImageView(context);
        imageView.setImageResource(logoResId);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        imageView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tintColor));

        LayoutParams params = new LayoutParams(
                sizeVariant == SIZE_SPLASH
                        ? getResources().getDimensionPixelSize(R.dimen.logo_splash_width)
                        : ViewGroup.LayoutParams.WRAP_CONTENT,
                sizeVariant == SIZE_SPLASH
                        ? getResources().getDimensionPixelSize(R.dimen.logo_splash_width)
                        : getResources().getDimensionPixelSize(R.dimen.logo_header_height)
        );
        params.gravity = Gravity.CENTER;
        addView(imageView, params);
    }

    private void addTypographicPlaceholder(Context context, int sizeVariant, int textColor) {
        float titleSp = sizeVariant == SIZE_SPLASH ? SPLASH_TITLE_SP : HEADER_TITLE_SP;
        float subtitleSp = titleSp * SUBTITLE_RATIO;

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        TextView titleView = new TextView(context);
        titleView.setText(R.string.brand_wordmark);
        titleView.setTextColor(textColor);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp);
        titleView.setLetterSpacing(TITLE_LETTER_SPACING);
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(Gravity.CENTER);
        if (!isInEditMode()) {
            titleView.setTypeface(ResourcesCompat.getFont(context, R.font.montserrat_bold));
        }

        TextView subtitleView = new TextView(context);
        subtitleView.setText(R.string.brand_subtitle);
        subtitleView.setTextColor(textColor);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSp);
        subtitleView.setIncludeFontPadding(false);
        subtitleView.setGravity(Gravity.CENTER);
        if (!isInEditMode()) {
            subtitleView.setTypeface(ResourcesCompat.getFont(context, R.font.inter_light));
        }

        container.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(subtitleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LayoutParams containerParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        containerParams.gravity = Gravity.CENTER;
        addView(container, containerParams);
    }
}
