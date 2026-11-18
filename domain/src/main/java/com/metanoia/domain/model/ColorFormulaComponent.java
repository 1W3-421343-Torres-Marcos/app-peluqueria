package com.metanoia.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Componente individual de una fórmula de color (marca, línea, tono y cantidad en décimas).
 */
public final class ColorFormulaComponent {

    public static final int MAX_BRAND_LENGTH = 80;
    public static final int MAX_PRODUCT_LINE_LENGTH = 80;
    public static final int MAX_SHADE_LENGTH = 40;
    public static final int MAX_AMOUNT_TENTHS = 99_990;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^(?:\\d+(?:[.,]\\d)?|[.,]\\d)$");

    private final String id;
    private final String formulaId;
    private final String brand;
    private final String productLine;
    private final String shade;
    private final int amountTenths;
    private final ColorComponentUnit unit;
    private final int position;

    private ColorFormulaComponent(Builder builder) {
        this.id = requireNonBlank(builder.id, "id");
        this.formulaId = Objects.requireNonNull(builder.formulaId, "formulaId");
        this.brand = requireMaxLength(requireNonBlank(builder.brand, "brand"),
                MAX_BRAND_LENGTH, "brand");
        this.productLine = normalizeOptional(builder.productLine, MAX_PRODUCT_LINE_LENGTH,
                "productLine");
        this.shade = requireMaxLength(requireNonBlank(builder.shade, "shade"),
                MAX_SHADE_LENGTH, "shade");
        if (builder.amountTenths <= 0 || builder.amountTenths > MAX_AMOUNT_TENTHS) {
            throw new IllegalArgumentException(
                    "amountTenths fuera de rango (1.." + MAX_AMOUNT_TENTHS + "): "
                            + builder.amountTenths);
        }
        this.amountTenths = builder.amountTenths;
        this.unit = Objects.requireNonNull(builder.unit, "unit");
        if (builder.position < 0) {
            throw new IllegalArgumentException("position negativa: " + builder.position);
        }
        this.position = builder.position;
    }

    public String id() {
        return id;
    }

    public String formulaId() {
        return formulaId;
    }

    public String brand() {
        return brand;
    }

    public Optional<String> productLine() {
        return Optional.ofNullable(productLine);
    }

    public String shade() {
        return shade;
    }

    public int amountTenths() {
        return amountTenths;
    }

    public ColorComponentUnit unit() {
        return unit;
    }

    public int position() {
        return position;
    }

    public String formattedAmount() {
        int whole = amountTenths / 10;
        int tenths = amountTenths % 10;
        if (tenths == 0) {
            return String.valueOf(whole);
        }
        return whole + "," + tenths;
    }

    public String summaryPart() {
        return shade + " (" + formattedAmount() + " " + unit.label() + ")";
    }

    public static Optional<Integer> parseAmountTenths(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        try {
            BigDecimal value = new BigDecimal(trimmed.replace(',', '.'));
            int tenths = value.multiply(BigDecimal.TEN).intValueExact();
            if (tenths <= 0 || tenths > MAX_AMOUNT_TENTHS) {
                return Optional.empty();
            }
            return Optional.of(tenths);
        } catch (ArithmeticException | NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .formulaId(formulaId)
                .brand(brand)
                .productLine(productLine)
                .shade(shade)
                .amountTenths(amountTenths)
                .unit(unit)
                .position(position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColorFormulaComponent)) {
            return false;
        }
        ColorFormulaComponent that = (ColorFormulaComponent) o;
        return amountTenths == that.amountTenths
                && position == that.position
                && id.equals(that.id)
                && formulaId.equals(that.formulaId)
                && brand.equals(that.brand)
                && Objects.equals(productLine, that.productLine)
                && shade.equals(that.shade)
                && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, formulaId, brand, productLine, shade, amountTenths, unit,
                position);
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " obligatorio");
        }
        return value.trim();
    }

    private static String requireMaxLength(String value, int maxLen, String field) {
        if (value.length() > maxLen) {
            throw new IllegalArgumentException(field + " excede " + maxLen + " caracteres");
        }
        return value;
    }

    private static String normalizeOptional(String value, int maxLen, String field) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLen) {
            throw new IllegalArgumentException(field + " excede " + maxLen + " caracteres");
        }
        return trimmed;
    }

    public static final class Builder {
        private String id;
        private String formulaId = "";
        private String brand;
        private String productLine;
        private String shade;
        private int amountTenths;
        private ColorComponentUnit unit = ColorComponentUnit.GRAMS;
        private int position;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder formulaId(String formulaId) {
            this.formulaId = formulaId;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder productLine(String productLine) {
            this.productLine = productLine;
            return this;
        }

        public Builder shade(String shade) {
            this.shade = shade;
            return this;
        }

        public Builder amountTenths(int amountTenths) {
            this.amountTenths = amountTenths;
            return this;
        }

        public Builder unit(ColorComponentUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder position(int position) {
            this.position = position;
            return this;
        }

        public ColorFormulaComponent build() {
            return new ColorFormulaComponent(this);
        }
    }
}
