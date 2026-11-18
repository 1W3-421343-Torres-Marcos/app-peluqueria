package com.metanoia.app.ui.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.metanoia.app.R;
import com.metanoia.app.databinding.FragmentTemplatesBinding;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.rules.TemplateRenderer;
import com.metanoia.domain.rules.TemplateRenderer.RenderResult;
import com.metanoia.domain.rules.TemplateRenderer.TemplateContext;

/**
 * Editor de plantillas de WhatsApp con vista previa en tiempo real y aviso de variables desconocidas.
 */
public class TemplatesFragment extends Fragment {

    private FragmentTemplatesBinding binding;
    private SettingsViewModel viewModel;
    private Settings loadedSettings;
    private boolean populated;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentTemplatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        attachPreviewWatcher(
                binding.inputReminderTemplate,
                binding.previewReminderTemplate,
                binding.warningReminderUnknown);
        attachPreviewWatcher(
                binding.inputBirthdayTemplate,
                binding.previewBirthdayTemplate,
                binding.warningBirthdayUnknown);
        attachPreviewWatcher(
                binding.inputWaitlistTemplate,
                binding.previewWaitlistTemplate,
                binding.warningWaitlistUnknown);

        viewModel.observeSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings == null) {
                return;
            }
            loadedSettings = settings;
            if (!populated) {
                populated = true;
                binding.inputReminderTemplate.setText(settings.reminderTemplate());
                binding.inputBirthdayTemplate.setText(settings.birthdayTemplate());
                binding.inputWaitlistTemplate.setText(settings.waitlistOfferTemplate());
            }
            refreshAllPreviews();
        });

        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnRestoreTemplateDefaults.setOnClickListener(v -> {
            binding.inputReminderTemplate.setText(Settings.DEFAULT_REMINDER_TEMPLATE);
            binding.inputBirthdayTemplate.setText(Settings.DEFAULT_BIRTHDAY_TEMPLATE);
            binding.inputWaitlistTemplate.setText(Settings.DEFAULT_WAITLIST_OFFER_TEMPLATE);
            refreshAllPreviews();
        });

        binding.btnSaveTemplates.setOnClickListener(v -> saveTemplates());
    }

    private void attachPreviewWatcher(
            @NonNull TextInputEditText input,
            @NonNull TextView previewView,
            @NonNull TextView warningView) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview(s == null ? "" : s.toString(), previewView, warningView);
            }
        });
    }

    private void refreshAllPreviews() {
        updatePreview(textOf(binding.inputReminderTemplate),
                binding.previewReminderTemplate, binding.warningReminderUnknown);
        updatePreview(textOf(binding.inputBirthdayTemplate),
                binding.previewBirthdayTemplate, binding.warningBirthdayUnknown);
        updatePreview(textOf(binding.inputWaitlistTemplate),
                binding.previewWaitlistTemplate, binding.warningWaitlistUnknown);
    }

    private void updatePreview(
            @NonNull String templateText,
            @NonNull TextView previewView,
            @NonNull TextView warningView) {
        TemplateContext context = TemplateRenderer.sampleContext(loadedSettings);
        RenderResult result = TemplateRenderer.render(templateText, context);
        previewView.setText(result.renderedText());
        if (result.unknownVariables().isEmpty()) {
            warningView.setVisibility(View.GONE);
        } else {
            warningView.setText(getString(
                    R.string.templates_unknown_vars_warning,
                    String.join(", ", result.unknownVariables())));
            warningView.setVisibility(View.VISIBLE);
        }
    }

    private void saveTemplates() {
        String reminder = textOf(binding.inputReminderTemplate);
        String birthday = textOf(binding.inputBirthdayTemplate);
        String waitlist = textOf(binding.inputWaitlistTemplate);
        if (reminder.isEmpty() || birthday.isEmpty() || waitlist.isEmpty()) {
            Toast.makeText(requireContext(), R.string.templates_error_empty, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Settings base = loadedSettings != null ? loadedSettings : Settings.defaultSettings();
        Settings updated = base.toBuilder()
                .reminderTemplate(reminder)
                .birthdayTemplate(birthday)
                .waitlistOfferTemplate(waitlist)
                .build();
        viewModel.save(updated);
    }

    @NonNull
    private static String textOf(@NonNull TextInputEditText input) {
        CharSequence cs = input.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
