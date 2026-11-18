package com.metanoia.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuración global del negocio persistida en la base local (incluida en el backup).
 */
public final class Settings {

    public static final String DEFAULT_BUSINESS_NAME = "Metanoia";
    public static final String DEFAULT_SUBTITLE = "Hair Salon";
    public static final int DEFAULT_SLOT_MINUTES = 15;
    public static final String DEFAULT_OPERATOR = "Dueño";
    public static final int DEFAULT_WAITLIST_EXPIRY_DAYS = 30;
    public static final String DEFAULT_REMINDER_TEMPLATE =
            "Hola {nombre}, te recuerdo tu turno el {fecha} a las {hora} en {negocio}. ¡Te esperamos!";
    public static final String DEFAULT_BIRTHDAY_TEMPLATE =
            "¡Feliz cumpleaños, {nombre}! Te saludamos desde {negocio}.";
    public static final String DEFAULT_WAITLIST_OFFER_TEMPLATE =
            "Hola {nombre}, se liberó un turno el {fecha} a las {hora} en {negocio}. ¿Te gustaría reservarlo?";
    private static final int DEFAULT_OPEN_MIN = 9 * 60;
    private static final int DEFAULT_CLOSE_MIN = 20 * 60;

    private final String businessName;
    private final String subtitle;
    private final String address;
    private final String phone;
    private final String cuit;
    private final int slotMinutes;
    private final String currentOperator;
    private final List<String> operators;
    private final List<TimeRange> generalSchedule;
    private final int waitlistExpiryDays;
    private final String reminderTemplate;
    private final String birthdayTemplate;
    private final String waitlistOfferTemplate;

    private Settings(Builder builder) {
        this.businessName = builder.businessName;
        this.subtitle = builder.subtitle;
        this.address = builder.address;
        this.phone = builder.phone;
        this.cuit = builder.cuit;
        this.slotMinutes = builder.slotMinutes;
        this.currentOperator = builder.currentOperator;
        this.operators = Collections.unmodifiableList(new ArrayList<>(builder.operators));
        this.generalSchedule = Collections.unmodifiableList(new ArrayList<>(builder.generalSchedule));
        this.waitlistExpiryDays = builder.waitlistExpiryDays;
        this.reminderTemplate = builder.reminderTemplate;
        this.birthdayTemplate = builder.birthdayTemplate;
        this.waitlistOfferTemplate = builder.waitlistOfferTemplate;
    }

    public static Settings defaultSettings() {
        List<TimeRange> defaultHours = new ArrayList<>();
        for (int day = 1; day <= 6; day++) {
            defaultHours.add(new TimeRange(day, DEFAULT_OPEN_MIN, DEFAULT_CLOSE_MIN));
        }
        return new Builder()
                .businessName(DEFAULT_BUSINESS_NAME)
                .subtitle(DEFAULT_SUBTITLE)
                .slotMinutes(DEFAULT_SLOT_MINUTES)
                .currentOperator(DEFAULT_OPERATOR)
                .operators(Collections.singletonList(DEFAULT_OPERATOR))
                .generalSchedule(defaultHours)
                .waitlistExpiryDays(DEFAULT_WAITLIST_EXPIRY_DAYS)
                .build();
    }

    public String businessName() {
        return businessName;
    }

    public String subtitle() {
        return subtitle;
    }

    public String address() {
        return address;
    }

    public String phone() {
        return phone;
    }

    public String cuit() {
        return cuit;
    }

    public int slotMinutes() {
        return slotMinutes;
    }

    public String currentOperator() {
        return currentOperator;
    }

    public List<String> operators() {
        return operators;
    }

    public List<TimeRange> generalSchedule() {
        return generalSchedule;
    }

    public int waitlistExpiryDays() {
        return waitlistExpiryDays;
    }

    public String reminderTemplate() {
        return reminderTemplate;
    }

    public String birthdayTemplate() {
        return birthdayTemplate;
    }

    public String waitlistOfferTemplate() {
        return waitlistOfferTemplate;
    }

    public String businessDisplayName() {
        String name = businessName == null ? "" : businessName.trim();
        String sub = subtitle == null ? "" : subtitle.trim();
        if (sub.isEmpty()) {
            return name;
        }
        if (name.isEmpty()) {
            return sub;
        }
        return name + " " + sub;
    }

    public Builder toBuilder() {
        return new Builder()
                .businessName(businessName)
                .subtitle(subtitle)
                .address(address)
                .phone(phone)
                .cuit(cuit)
                .slotMinutes(slotMinutes)
                .currentOperator(currentOperator)
                .operators(operators)
                .generalSchedule(generalSchedule)
                .waitlistExpiryDays(waitlistExpiryDays)
                .reminderTemplate(reminderTemplate)
                .birthdayTemplate(birthdayTemplate)
                .waitlistOfferTemplate(waitlistOfferTemplate);
    }

    public static final class Builder {
        private String businessName = DEFAULT_BUSINESS_NAME;
        private String subtitle = DEFAULT_SUBTITLE;
        private String address = "";
        private String phone = "";
        private String cuit = "";
        private int slotMinutes = DEFAULT_SLOT_MINUTES;
        private String currentOperator = DEFAULT_OPERATOR;
        private List<String> operators = Collections.singletonList(DEFAULT_OPERATOR);
        private List<TimeRange> generalSchedule = Collections.emptyList();
        private int waitlistExpiryDays = DEFAULT_WAITLIST_EXPIRY_DAYS;
        private String reminderTemplate = DEFAULT_REMINDER_TEMPLATE;
        private String birthdayTemplate = DEFAULT_BIRTHDAY_TEMPLATE;
        private String waitlistOfferTemplate = DEFAULT_WAITLIST_OFFER_TEMPLATE;

        public Builder businessName(String businessName) {
            this.businessName = Objects.requireNonNull(businessName, "businessName");
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle == null ? "" : subtitle;
            return this;
        }

        public Builder address(String address) {
            this.address = address == null ? "" : address;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone == null ? "" : phone;
            return this;
        }

        public Builder cuit(String cuit) {
            this.cuit = cuit == null ? "" : cuit;
            return this;
        }

        public Builder slotMinutes(int slotMinutes) {
            this.slotMinutes = slotMinutes;
            return this;
        }

        public Builder currentOperator(String currentOperator) {
            this.currentOperator = Objects.requireNonNull(currentOperator, "currentOperator");
            return this;
        }

        public Builder operators(List<String> operators) {
            this.operators = new ArrayList<>(Objects.requireNonNull(operators, "operators"));
            return this;
        }

        public Builder generalSchedule(List<TimeRange> generalSchedule) {
            this.generalSchedule = new ArrayList<>(
                    Objects.requireNonNull(generalSchedule, "generalSchedule"));
            return this;
        }

        public Builder waitlistExpiryDays(int waitlistExpiryDays) {
            this.waitlistExpiryDays = waitlistExpiryDays;
            return this;
        }

        public Builder reminderTemplate(String reminderTemplate) {
            this.reminderTemplate = reminderTemplate == null || reminderTemplate.trim().isEmpty()
                    ? DEFAULT_REMINDER_TEMPLATE
                    : reminderTemplate.trim();
            return this;
        }

        public Builder birthdayTemplate(String birthdayTemplate) {
            this.birthdayTemplate = birthdayTemplate == null || birthdayTemplate.trim().isEmpty()
                    ? DEFAULT_BIRTHDAY_TEMPLATE
                    : birthdayTemplate.trim();
            return this;
        }

        public Builder waitlistOfferTemplate(String waitlistOfferTemplate) {
            this.waitlistOfferTemplate = waitlistOfferTemplate == null
                    || waitlistOfferTemplate.trim().isEmpty()
                    ? DEFAULT_WAITLIST_OFFER_TEMPLATE
                    : waitlistOfferTemplate.trim();
            return this;
        }

        public Settings build() {
            return new Settings(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Settings)) {
            return false;
        }
        Settings settings = (Settings) o;
        return slotMinutes == settings.slotMinutes
                && waitlistExpiryDays == settings.waitlistExpiryDays
                && businessName.equals(settings.businessName)
                && subtitle.equals(settings.subtitle)
                && address.equals(settings.address)
                && phone.equals(settings.phone)
                && cuit.equals(settings.cuit)
                && currentOperator.equals(settings.currentOperator)
                && operators.equals(settings.operators)
                && generalSchedule.equals(settings.generalSchedule)
                && reminderTemplate.equals(settings.reminderTemplate)
                && birthdayTemplate.equals(settings.birthdayTemplate)
                && waitlistOfferTemplate.equals(settings.waitlistOfferTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessName, subtitle, address, phone, cuit, slotMinutes,
                currentOperator, operators, generalSchedule, waitlistExpiryDays,
                reminderTemplate, birthdayTemplate, waitlistOfferTemplate);
    }
}
