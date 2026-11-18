package com.metanoia.app.ui.reminders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.BirthdayReminderProjection;
import com.metanoia.app.databinding.ItemReminderRowBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.app.util.WhatsAppLauncher;

import java.util.Objects;

/**
 * Adaptador para listar clientes que cumplen años hoy y su estado de saludo anual.
 */
public class BirthdayRemindersAdapter extends
        ListAdapter<BirthdayReminderProjection, BirthdayRemindersAdapter.Holder> {

    public interface Listener {
        void onGreetClicked(@NonNull BirthdayReminderProjection item);

        void onUndoClicked(@NonNull String reminderId);
    }

    private final int currentYear;
    private final Listener listener;

    public BirthdayRemindersAdapter(int currentYear, @NonNull Listener listener) {
        super(DIFF);
        this.currentYear = currentYear;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderRowBinding binding = ItemReminderRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), currentYear, listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemReminderRowBinding binding;

        Holder(@NonNull ItemReminderRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull BirthdayReminderProjection item,
                int currentYear,
                @NonNull Listener listener
        ) {
            Context context = binding.getRoot().getContext();
            binding.reminderClientName.setText(item.clientName);
            if (item.birthYear != null && item.birthYear > 1900 && item.birthYear <= currentYear) {
                int age = currentYear - item.birthYear;
                binding.reminderDetails.setText(context.getString(
                        R.string.reminder_birthday_detail_age_format,
                        item.birthDay, item.birthMonth, age));
            } else {
                binding.reminderDetails.setText(context.getString(
                        R.string.reminder_birthday_detail_format,
                        item.birthDay, item.birthMonth));
            }

            boolean hasPhone = WhatsAppLauncher.hasValidPhone(item.clientPhone);
            binding.reminderNoPhoneHint.setVisibility(hasPhone ? View.GONE : View.VISIBLE);
            binding.btnReminderSendWhatsApp.setEnabled(hasPhone);

            if (item.isSent() && item.sentAt != null && item.reminderId != null) {
                binding.reminderSentStatus.setText(context.getString(
                        R.string.reminder_greeted_badge_format,
                        Formats.formatDateTime(item.sentAt)));
                binding.reminderSentStatus.setVisibility(View.VISIBLE);
                binding.btnReminderSendWhatsApp.setText(R.string.reminder_regreet_whatsapp_btn);
                binding.btnReminderUndo.setVisibility(View.VISIBLE);
                String reminderId = item.reminderId;
                binding.btnReminderUndo.setOnClickListener(v -> listener.onUndoClicked(reminderId));
            } else {
                binding.reminderSentStatus.setVisibility(View.GONE);
                binding.btnReminderSendWhatsApp.setText(R.string.reminder_greet_whatsapp_btn);
                binding.btnReminderUndo.setVisibility(View.GONE);
                binding.btnReminderUndo.setOnClickListener(null);
            }

            binding.btnReminderSendWhatsApp.setOnClickListener(v -> listener.onGreetClicked(item));
        }
    }

    private static final DiffUtil.ItemCallback<BirthdayReminderProjection> DIFF =
            new DiffUtil.ItemCallback<BirthdayReminderProjection>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull BirthdayReminderProjection oldItem,
                        @NonNull BirthdayReminderProjection newItem) {
                    return oldItem.clientId.equals(newItem.clientId);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull BirthdayReminderProjection oldItem,
                        @NonNull BirthdayReminderProjection newItem) {
                    return Objects.equals(oldItem.reminderId, newItem.reminderId)
                            && Objects.equals(oldItem.sentAt, newItem.sentAt)
                            && Objects.equals(oldItem.birthYear, newItem.birthYear)
                            && oldItem.birthDay == newItem.birthDay
                            && oldItem.birthMonth == newItem.birthMonth
                            && oldItem.clientName.equals(newItem.clientName)
                            && oldItem.clientPhone.equals(newItem.clientPhone);
                }
            };
}
