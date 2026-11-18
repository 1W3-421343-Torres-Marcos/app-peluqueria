package com.metanoia.app.ui.reminders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.BirthdayReminderProjection;
import com.metanoia.app.data.db.projection.TomorrowReminderProjection;
import com.metanoia.app.databinding.FragmentRemindersBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.app.util.WhatsAppLauncher;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.rules.TemplateRenderer;
import com.metanoia.domain.rules.TemplateRenderer.TemplateContext;

/**
 * Pantalla de recordatorios por WhatsApp con pestañas Mañana y Cumpleaños.
 */
public class RemindersFragment extends Fragment {

    private FragmentRemindersBinding binding;
    private RemindersViewModel viewModel;
    private TomorrowRemindersAdapter tomorrowAdapter;
    private BirthdayRemindersAdapter birthdayAdapter;
    @Nullable
    private Settings latestSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRemindersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);
        viewModel.refreshSettings();

        binding.remindersTomorrowHeader.setText(getString(
                R.string.reminders_tomorrow_header_format,
                Formats.formatDayWithShortWeekday(viewModel.tomorrowEpochDay())));

        tomorrowAdapter = new TomorrowRemindersAdapter(new TomorrowRemindersAdapter.Listener() {
            @Override
            public void onSendClicked(@NonNull TomorrowReminderProjection item) {
                sendTomorrowReminder(item);
            }

            @Override
            public void onUndoClicked(@NonNull String reminderId) {
                viewModel.undoReminder(reminderId);
            }
        });
        binding.remindersTomorrowRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.remindersTomorrowRecycler.setAdapter(tomorrowAdapter);

        birthdayAdapter = new BirthdayRemindersAdapter(
                viewModel.currentYear(),
                new BirthdayRemindersAdapter.Listener() {
                    @Override
                    public void onGreetClicked(@NonNull BirthdayReminderProjection item) {
                        sendBirthdayGreeting(item);
                    }

                    @Override
                    public void onUndoClicked(@NonNull String reminderId) {
                        viewModel.undoReminder(reminderId);
                    }
                });
        binding.remindersBirthdaysRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.remindersBirthdaysRecycler.setAdapter(birthdayAdapter);

        setupTabs();

        viewModel.observeSettings().observe(getViewLifecycleOwner(), settings ->
                latestSettings = settings);

        viewModel.observeTomorrowReminders().observe(getViewLifecycleOwner(), items -> {
            boolean empty = items == null || items.isEmpty();
            binding.remindersTomorrowRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.remindersTomorrowEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            tomorrowAdapter.submitList(items);
        });

        viewModel.observeTodayBirthdays().observe(getViewLifecycleOwner(), items -> {
            boolean empty = items == null || items.isEmpty();
            binding.remindersBirthdaysRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.remindersBirthdaysEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            birthdayAdapter.submitList(items);
        });

        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer msgRes = event == null ? null : event.getContentIfNotHandled();
            if (msgRes != null) {
                Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendTomorrowReminder(@NonNull TomorrowReminderProjection item) {
        Settings settings = latestSettings != null ? latestSettings : Settings.defaultSettings();
        TemplateContext context = new TemplateContext(
                item.clientName,
                item.day,
                item.startMin,
                item.servicesSummary,
                item.professionalName,
                settings.businessDisplayName());
        String message = TemplateRenderer.render(settings.reminderTemplate(), context)
                .renderedText();
        if (WhatsAppLauncher.openChat(requireContext(), item.clientPhone, message)) {
            viewModel.markAppointmentReminded(item.appointmentId, item.clientId, reminder -> {
                if (binding == null) {
                    return;
                }
                Snackbar.make(binding.getRoot(), R.string.reminder_marked_sent, Snackbar.LENGTH_LONG)
                        .setAction(R.string.reminder_undo_action,
                                v -> viewModel.undoReminder(reminder.id()))
                        .show();
            });
        }
    }

    private void sendBirthdayGreeting(@NonNull BirthdayReminderProjection item) {
        Settings settings = latestSettings != null ? latestSettings : Settings.defaultSettings();
        TemplateContext context = new TemplateContext(
                item.clientName,
                viewModel.todayEpochDay(),
                600,
                "",
                "",
                settings.businessDisplayName());
        String message = TemplateRenderer.render(settings.birthdayTemplate(), context)
                .renderedText();
        if (WhatsAppLauncher.openChat(requireContext(), item.clientPhone, message)) {
            viewModel.markBirthdayGreeted(item.clientId, reminder -> {
                if (binding == null) {
                    return;
                }
                Snackbar.make(
                                binding.getRoot(),
                                R.string.reminder_birthday_marked_sent,
                                Snackbar.LENGTH_LONG)
                        .setAction(R.string.reminder_undo_action,
                                v -> viewModel.undoReminder(reminder.id()))
                        .show();
            });
        }
    }

    private void setupTabs() {
        showTab(0);
        binding.remindersTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }
        });
    }

    private void showTab(int index) {
        binding.remindersTomorrowPanel.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        binding.remindersBirthdaysPanel.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
