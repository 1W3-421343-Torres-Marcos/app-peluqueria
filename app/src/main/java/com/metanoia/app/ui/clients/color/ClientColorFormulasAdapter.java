package com.metanoia.app.ui.clients.color;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.databinding.ItemColorFormulaRowBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.model.ColorFormulaComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador para listar el historial cronológico de fórmulas de coloración de un cliente.
 */
public class ClientColorFormulasAdapter
        extends ListAdapter<ClientColorFormulasAdapter.FormulaRowItem,
                ClientColorFormulasAdapter.FormulaViewHolder> {

    public interface OnFormulaActionListener {
        void onEditOrView(@NonNull ColorFormula formula, boolean locked);

        void onDuplicate(@NonNull ColorFormula formula);

        void onDelete(@NonNull ColorFormula formula);
    }

    private final OnFormulaActionListener listener;

    public ClientColorFormulasAdapter(@NonNull OnFormulaActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    @NonNull
    @Override
    public FormulaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemColorFormulaRowBinding binding = ItemColorFormulaRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FormulaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FormulaViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class FormulaViewHolder extends RecyclerView.ViewHolder {

        private final ItemColorFormulaRowBinding binding;

        FormulaViewHolder(@NonNull ItemColorFormulaRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull FormulaRowItem item, @NonNull OnFormulaActionListener listener) {
            Context context = binding.getRoot().getContext();
            ColorFormula formula = item.formula;

            binding.colorFormulaRowDate.setText(Formats.formatDate(formula.day()));
            binding.colorFormulaRowLockedBadge.setVisibility(
                    item.locked ? View.VISIBLE : View.GONE);
            binding.colorFormulaRowShades.setText(formula.mainShadesSummary());

            String meta = buildMetaLine(context, formula, item.professionalName);
            if (meta.isEmpty()) {
                binding.colorFormulaRowMeta.setVisibility(View.GONE);
            } else {
                binding.colorFormulaRowMeta.setVisibility(View.VISIBLE);
                binding.colorFormulaRowMeta.setText(meta);
            }

            binding.colorFormulaRowComponentsDetail.setText(buildComponentsDetail(formula));

            if (formula.notes().isEmpty()) {
                binding.colorFormulaRowNotes.setVisibility(View.GONE);
            } else {
                binding.colorFormulaRowNotes.setVisibility(View.VISIBLE);
                binding.colorFormulaRowNotes.setText(
                        context.getString(R.string.color_formula_notes_prefix, formula.notes()));
            }

            if (formula.result().isPresent()) {
                binding.colorFormulaRowResult.setVisibility(View.VISIBLE);
                binding.colorFormulaRowResult.setText(context.getString(
                        R.string.color_formula_result_prefix, formula.result().get()));
            } else {
                binding.colorFormulaRowResult.setVisibility(View.GONE);
            }

            binding.btnFormulaEdit.setText(item.locked
                    ? R.string.color_formula_view
                    : R.string.color_formula_edit);
            binding.btnFormulaEdit.setOnClickListener(
                    v -> listener.onEditOrView(formula, item.locked));
            binding.btnFormulaDuplicate.setOnClickListener(v -> listener.onDuplicate(formula));

            binding.btnFormulaDelete.setVisibility(item.locked ? View.GONE : View.VISIBLE);
            binding.btnFormulaDelete.setOnClickListener(v -> listener.onDelete(formula));
        }

        private static String buildMetaLine(
                @NonNull Context context,
                @NonNull ColorFormula formula,
                @Nullable String professionalName
        ) {
            List<String> parts = new ArrayList<>();
            if (professionalName != null && !professionalName.trim().isEmpty()) {
                parts.add(professionalName.trim());
            }
            if (formula.exposureMin() > 0) {
                parts.add(context.getString(
                        R.string.color_formula_meta_exposure, formula.exposureMin()));
            }
            if (formula.developerVolume().isPresent()) {
                parts.add(formula.developerVolume().get());
            }
            return String.join(" · ", parts);
        }

        private static String buildComponentsDetail(@NonNull ColorFormula formula) {
            StringBuilder sb = new StringBuilder();
            List<ColorFormulaComponent> components = formula.components();
            for (int i = 0; i < components.size(); i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                ColorFormulaComponent comp = components.get(i);
                sb.append(comp.brand());
                if (comp.productLine().isPresent()) {
                    sb.append(" · ").append(comp.productLine().get());
                }
                sb.append(" · ").append(comp.summaryPart());
            }
            return sb.toString();
        }
    }

    /**
     * Modelo liviano de fila combinando fórmula, nombre de profesional resuelto y estado de bloqueo.
     */
    public static final class FormulaRowItem {
        public final ColorFormula formula;
        @Nullable
        public final String professionalName;
        public final boolean locked;

        public FormulaRowItem(
                @NonNull ColorFormula formula,
                @Nullable String professionalName,
                boolean locked
        ) {
            this.formula = Objects.requireNonNull(formula, "formula");
            this.professionalName = professionalName;
            this.locked = locked;
        }
    }

    private static final DiffUtil.ItemCallback<FormulaRowItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FormulaRowItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull FormulaRowItem oldItem,
                        @NonNull FormulaRowItem newItem
                ) {
                    return oldItem.formula.id().equals(newItem.formula.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull FormulaRowItem oldItem,
                        @NonNull FormulaRowItem newItem
                ) {
                    return oldItem.locked == newItem.locked
                            && Objects.equals(oldItem.professionalName, newItem.professionalName)
                            && oldItem.formula.equals(newItem.formula);
                }
            };
}
