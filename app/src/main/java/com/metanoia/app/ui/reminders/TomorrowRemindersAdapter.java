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
import com.metanoia.app.data.db.projection.TomorrowReminderProjection;
import com.metanoia.app.databinding.ItemReminderRowBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.app.util.WhatsAppLauncher;

import java.util.Objects;

/**
 * Adaptador para listar los turnos de mañana pendientes de recordatorio o ya enviados.
 */
public class TomorrowRemindersAdapter extends
        ListAdapter<TomorrowReminderProjection, TomorrowRemindersAdapter.Holder> {

    public interface Listener {
        void onSendClicked(@NonNull TomorrowReminderProjection item);

        void onUndoClicked(@NonNull String reminderId);
    }

    private final Listener listener;

    public TomorrowRemindersAdapter(@NonNull Listener listener) {
        super(DIFF);
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
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemReminderRowBinding binding;

        Holder(@NonNull ItemReminderRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull TomorrowReminderProjection item, @NonNull Listener listener) {
            Context context = binding.getRoot().getContext();
            binding.reminderClientName.setText(item.clientName);
            binding.reminderDetails.setText(context.getString(
                    R.string.reminder_row_meta_format,
                    Formats.formatTime(item.startMin),
                    item.servicesSummary,
                    item.professionalName));

            boolean hasPhone = WhatsAppLauncher.hasValidPhone(item.clientPhone);
            binding.reminderNoPhoneHint.setVisibility(hasPhone ? View.GONE : View.VISIBLE);
            binding.btnReminderSendWhatsApp.setEnabled(hasPhone);

            if (item.isSent() && item.sentAt != null && item.reminderId != null) {
                binding.reminderSentStatus.setText(context.getString(
                        R.string.reminder_sent_badge_format,
                        Formats.formatDateTime(item.sentAt)));
                binding.reminderSentStatus.setVisibility(View.VISIBLE);
                binding.btnReminderSendWhatsApp.setText(R.string.reminder_resend_whatsapp_btn);
                binding.btnReminderUndo.setVisibility(View.VISIBLE);
                String reminderId = item.reminderId;
                binding.btnReminderUndo.setOnClickListener(v -> listener.onUndoClicked(reminderId));
            } else {
                binding.reminderSentStatus.setVisibility(View.GONE);
                binding.btnReminderSendWhatsApp.setText(R.string.reminder_send_whatsapp_btn);
                binding.btnReminderUndo.setVisibility(View.GONE);
                binding.btnReminderUndo.setOnClickListener(null);
            }

            binding.btnReminderSendWhatsApp.setOnClickListener(v -> listener.onSendClicked(item));
        }
    }

    private static final DiffUtil.ItemCallback<TomorrowReminderProjection> DIFF =
            new DiffUtil.ItemCallback<TomorrowReminderProjection>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull TomorrowReminderProjection oldItem,
                        @NonNull TomorrowReminderProjection newItem) {
                    return oldItem.appointmentId.equals(newItem.appointmentId);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull TomorrowReminderProjection oldItem,
                        @NonNull TomorrowReminderProjection newItem) {
                    return Objects.equals(oldItem.reminderId, newItem.reminderId)
                            && Objects.equals(oldItem.sentAt, newItem.sentAt)
                            && oldItem.startMin == newItem.startMin
                            && oldItem.clientName.equals(newItem.clientName)
                            && oldItem.clientPhone.equals(newItem.clientPhone)
                            && oldItem.servicesSummary.equals(newItem.servicesSummary)
                            && oldItem.professionalName.equals(newItem.professionalName);
                }
            };
}
