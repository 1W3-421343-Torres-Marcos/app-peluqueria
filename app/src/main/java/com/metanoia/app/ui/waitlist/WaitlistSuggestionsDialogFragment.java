package com.metanoia.app.ui.waitlist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogWaitlistSuggestionsBinding;
import com.metanoia.app.databinding.ItemWaitlistSuggestionBinding;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailViewModel;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailViewModel.WaitlistSuggestionsState;
import com.metanoia.app.util.Formats;
import com.metanoia.app.util.WhatsAppLauncher;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.rules.WaitlistMatcher.FreedSlot;
import com.metanoia.domain.rules.WaitlistMatcher.MatchedCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Diálogo post-cancelación / post-reprogramación para ofrecer o asignar un hueco liberado.
 */
public class WaitlistSuggestionsDialogFragment extends BottomSheetDialogFragment {

    private DialogWaitlistSuggestionsBinding binding;
    private AppointmentDetailViewModel viewModel;
    private SuggestionsAdapter adapter;

    public static WaitlistSuggestionsDialogFragment newInstance() {
        return new WaitlistSuggestionsDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogWaitlistSuggestionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment())
                .get(AppointmentDetailViewModel.class);

        adapter = new SuggestionsAdapter(new SuggestionActionListener() {
            @Override
            public void onOffer(@NonNull MatchedCandidate candidate) {
                WaitlistSuggestionsState state =
                        viewModel.observeWaitlistSuggestionsState().getValue();
                if (state == null) {
                    return;
                }
                FreedSlot slot = state.freedSlot;
                viewModel.prepareWaitlistOffer(candidate.entry().id(), slot, preview -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (WhatsAppLauncher.openChat(
                            requireContext(), preview.clientPhone(), preview.renderedMessage())) {
                        viewModel.markWaitlistOfferSent(candidate.entry().id(), slot);
                    }
                });
            }

            @Override
            public void onAssign(@NonNull MatchedCandidate candidate) {
                WaitlistSuggestionsState state =
                        viewModel.observeWaitlistSuggestionsState().getValue();
                if (state == null) {
                    return;
                }
                viewModel.assignWaitlistEntry(
                        candidate.entry().id(), state.freedSlot, () -> {
                            if (isAdded()) {
                                dismiss();
                            }
                        });
            }
        });

        binding.waitlistSuggestionsRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.waitlistSuggestionsRecycler.setAdapter(adapter);
        binding.btnIgnoreWaitlistSuggestions.setOnClickListener(v -> dismiss());

        viewModel.observeWaitlistSuggestionsState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                adapter.submitList(Collections.emptyList());
                return;
            }
            binding.txtSuggestionsSlotSummary.setText(getString(
                    R.string.waitlist_suggestions_slot_format,
                    Formats.formatDayWithShortWeekday(state.freedSlot.dayEpoch()),
                    Formats.formatTime(state.freedSlot.startMin()),
                    Formats.formatTime(state.freedSlot.endMin()),
                    state.professionalName));
            List<SuggestionItem> items = new ArrayList<>(state.candidates.size());
            for (MatchedCandidate candidate : state.candidates) {
                boolean offered = state.offeredEntryIds.contains(candidate.entry().id());
                items.add(new SuggestionItem(candidate, offered));
            }
            adapter.submitList(items);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface SuggestionActionListener {
        void onOffer(@NonNull MatchedCandidate candidate);

        void onAssign(@NonNull MatchedCandidate candidate);
    }

    private static final class SuggestionItem {
        final MatchedCandidate candidate;
        final boolean offered;

        SuggestionItem(@NonNull MatchedCandidate candidate, boolean offered) {
            this.candidate = candidate;
            this.offered = offered;
        }
    }

    private static final class SuggestionsAdapter
            extends ListAdapter<SuggestionItem, SuggestionsAdapter.ViewHolder> {

        private final SuggestionActionListener listener;

        SuggestionsAdapter(@NonNull SuggestionActionListener listener) {
            super(DIFF_CALLBACK);
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemWaitlistSuggestionBinding rowBinding = ItemWaitlistSuggestionBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(rowBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position), listener);
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {

            private final ItemWaitlistSuggestionBinding binding;

            ViewHolder(@NonNull ItemWaitlistSuggestionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(@NonNull SuggestionItem item, @NonNull SuggestionActionListener listener) {
                Context context = binding.getRoot().getContext();
                MatchedCandidate candidate = item.candidate;

                binding.txtSuggestionClientName.setText(candidate.client().name());
                binding.txtSuggestionExactProBadge.setVisibility(
                        candidate.exactProfessionalMatch() ? View.VISIBLE : View.GONE);

                List<String> serviceNames = new ArrayList<>(candidate.services().size());
                for (SalonService svc : candidate.services()) {
                    serviceNames.add(svc.name());
                }
                binding.txtSuggestionServices.setText(context.getString(
                        R.string.waitlist_suggestion_services_format,
                        String.join(" + ", serviceNames),
                        candidate.requiredDurationMin()));

                int noShows = candidate.client().noShowCount();
                if (noShows > 0) {
                    binding.txtSuggestionNoShows.setVisibility(View.VISIBLE);
                    binding.txtSuggestionNoShows.setText(context.getResources().getQuantityString(
                            R.plurals.client_no_shows_count, noShows, noShows));
                } else {
                    binding.txtSuggestionNoShows.setVisibility(View.GONE);
                }

                boolean hasPhone = WhatsAppLauncher.hasValidPhone(candidate.client().phone());
                binding.txtSuggestionNoPhone.setVisibility(hasPhone ? View.GONE : View.VISIBLE);
                binding.btnSuggestionOffer.setEnabled(hasPhone && !item.offered);
                binding.btnSuggestionOffer.setText(item.offered
                        ? R.string.waitlist_suggestion_offered_btn
                        : R.string.waitlist_suggestion_offer_btn);

                binding.btnSuggestionOffer.setOnClickListener(v -> listener.onOffer(candidate));
                binding.btnSuggestionAssign.setOnClickListener(v -> listener.onAssign(candidate));
            }
        }

        private static final DiffUtil.ItemCallback<SuggestionItem> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<SuggestionItem>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull SuggestionItem oldItem,
                            @NonNull SuggestionItem newItem
                    ) {
                        return oldItem.candidate.entry().id()
                                .equals(newItem.candidate.entry().id());
                    }

                    @Override
                    public boolean areContentsTheSame(
                            @NonNull SuggestionItem oldItem,
                            @NonNull SuggestionItem newItem
                    ) {
                        return oldItem.offered == newItem.offered
                                && Objects.equals(
                                oldItem.candidate.entry(), newItem.candidate.entry());
                    }
                };
    }
}
