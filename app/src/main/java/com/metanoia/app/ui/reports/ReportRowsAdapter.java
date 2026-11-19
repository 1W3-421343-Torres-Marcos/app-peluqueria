package com.metanoia.app.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.databinding.ItemReportRowBinding;

import java.util.Objects;

/**
 * Adaptador de filas tabulares reutilizable para los 6 reportes de Metanoia.
 */
public final class ReportRowsAdapter
        extends ListAdapter<ReportRowsAdapter.ReportRowItem, ReportRowsAdapter.RowViewHolder> {

    public static final class ReportRowItem {
        @NonNull
        public final String key;
        @NonNull
        public final String title;
        @Nullable
        public final String subtitle;
        @Nullable
        public final String badge;
        @NonNull
        public final String primaryValue;
        @Nullable
        public final String secondaryValue;
        public final boolean mutedHighlight;

        public ReportRowItem(
                @NonNull String key,
                @NonNull String title,
                @Nullable String subtitle,
                @Nullable String badge,
                @NonNull String primaryValue,
                @Nullable String secondaryValue,
                boolean mutedHighlight
        ) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.badge = badge;
            this.primaryValue = primaryValue;
            this.secondaryValue = secondaryValue;
            this.mutedHighlight = mutedHighlight;
        }
    }

    private static final DiffUtil.ItemCallback<ReportRowItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ReportRowItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ReportRowItem oldItem,
                        @NonNull ReportRowItem newItem
                ) {
                    return oldItem.key.equals(newItem.key);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ReportRowItem oldItem,
                        @NonNull ReportRowItem newItem
                ) {
                    return oldItem.mutedHighlight == newItem.mutedHighlight
                            && Objects.equals(oldItem.title, newItem.title)
                            && Objects.equals(oldItem.subtitle, newItem.subtitle)
                            && Objects.equals(oldItem.badge, newItem.badge)
                            && Objects.equals(oldItem.primaryValue, newItem.primaryValue)
                            && Objects.equals(oldItem.secondaryValue, newItem.secondaryValue);
                }
            };

    public ReportRowsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportRowBinding binding = ItemReportRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RowViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class RowViewHolder extends RecyclerView.ViewHolder {

        private final ItemReportRowBinding binding;

        RowViewHolder(@NonNull ItemReportRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ReportRowItem item) {
            binding.reportRowTitle.setText(item.title);
            binding.reportRowPrimaryValue.setText(item.primaryValue);

            if (item.subtitle != null && !item.subtitle.isEmpty()) {
                binding.reportRowSubtitle.setVisibility(View.VISIBLE);
                binding.reportRowSubtitle.setText(item.subtitle);
            } else {
                binding.reportRowSubtitle.setVisibility(View.GONE);
            }

            if (item.badge != null && !item.badge.isEmpty()) {
                binding.reportRowBadge.setVisibility(View.VISIBLE);
                binding.reportRowBadge.setText(item.badge);
            } else {
                binding.reportRowBadge.setVisibility(View.GONE);
            }

            if (item.secondaryValue != null && !item.secondaryValue.isEmpty()) {
                binding.reportRowSecondaryValue.setVisibility(View.VISIBLE);
                binding.reportRowSecondaryValue.setText(item.secondaryValue);
            } else {
                binding.reportRowSecondaryValue.setVisibility(View.GONE);
            }
        }
    }
}
