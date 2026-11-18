package com.metanoia.app.ui.services;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.ui.common.MoneyTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador agrupado por categoría para mostrar los servicios del salón.
 */
public class ServicesAdapter extends ListAdapter<ServicesAdapter.Item, RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;

    public interface Listener {
        void onServiceClicked(SalonServiceEntity service);
    }

    private final Listener listener;

    public ServicesAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void submitGrouped(@NonNull List<SalonServiceEntity> entities) {
        List<Item> items = new ArrayList<>();
        String lastCategory = null;
        for (SalonServiceEntity entity : entities) {
            if (!entity.category.equalsIgnoreCase(lastCategory)) {
                lastCategory = entity.category;
                items.add(Item.header(entity.category));
            }
            items.add(Item.row(entity));
        }
        submitList(items);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isHeader() ? TYPE_HEADER : TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_service_header, parent, false);
            return new HeaderHolder(view);
        }
        View view = inflater.inflate(R.layout.item_service_row, parent, false);
        return new RowHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(item.category);
        } else if (holder instanceof RowHolder) {
            ((RowHolder) holder).bind(item.service, listener);
        }
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.serviceCategoryHeader);
        }

        void bind(String category) {
            titleView.setText(category);
        }
    }

    static final class RowHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView metaView;
        private final MoneyTextView priceView;

        RowHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.serviceRowName);
            metaView = itemView.findViewById(R.id.serviceRowMeta);
            priceView = itemView.findViewById(R.id.serviceRowPrice);
        }

        void bind(SalonServiceEntity entity, Listener listener) {
            Context context = itemView.getContext();
            nameView.setText(entity.name);
            nameView.setAlpha(entity.active ? 1f : 0.55f);

            StringBuilder meta = new StringBuilder(
                    context.getString(R.string.service_duration_minutes, entity.durationMin));
            if (entity.colorService) {
                meta.append(" · ").append(context.getString(R.string.service_color_tag));
            }
            if (!entity.active) {
                meta.append(" · ").append(context.getString(R.string.service_inactive_tag));
            }
            metaView.setText(meta.toString());
            priceView.setMoneyCents(entity.priceCents);
            priceView.setAlpha(entity.active ? 1f : 0.55f);
            itemView.setOnClickListener(v -> listener.onServiceClicked(entity));
        }
    }

    public static final class Item {
        final String category;
        final SalonServiceEntity service;

        private Item(String category, SalonServiceEntity service) {
            this.category = category;
            this.service = service;
        }

        static Item header(String category) {
            return new Item(category, null);
        }

        static Item row(SalonServiceEntity service) {
            return new Item(null, service);
        }

        boolean isHeader() {
            return service == null;
        }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override
        public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            if (oldItem.isHeader() != newItem.isHeader()) {
                return false;
            }
            if (oldItem.isHeader()) {
                return oldItem.category.equalsIgnoreCase(newItem.category);
            }
            return oldItem.service.id.equals(newItem.service.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            if (oldItem.isHeader()) {
                return Objects.equals(oldItem.category, newItem.category);
            }
            SalonServiceEntity a = oldItem.service;
            SalonServiceEntity b = newItem.service;
            return a.name.equals(b.name)
                    && a.category.equals(b.category)
                    && a.priceCents == b.priceCents
                    && a.durationMin == b.durationMin
                    && a.colorService == b.colorService
                    && a.active == b.active;
        }
    };
}
