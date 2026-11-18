package com.metanoia.app.ui.clients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.databinding.FragmentClientDetailBinding;
import com.metanoia.app.ui.agenda.detail.AppointmentDetailFragment;
import com.metanoia.app.ui.clients.color.ClientColorFormulasAdapter;
import com.metanoia.app.ui.clients.color.ClientColorFormulasAdapter.FormulaRowItem;
import com.metanoia.app.ui.clients.color.ColorFormulaDialogFragment;
import com.metanoia.app.ui.common.ConfirmDialog;
import com.metanoia.app.ui.waitlist.WaitlistFormDialogFragment;
import com.metanoia.domain.model.ColorFormula;
import com.metanoia.domain.rules.PhoneNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ficha de cliente: datos básicos, contador de ausencias, saldo pendiente y pestañas de historial.
 */
public class ClientDetailFragment extends Fragment {

    public static final String ARG_CLIENT_ID = "client_id";
    private static final String TAG_EDIT_CLIENT = "edit_client_dialog";
    private static final String TAG_COLOR_FORMULA = "color_formula_dialog";
    private static final String TAG_ADD_WAITLIST = "client_add_waitlist_dialog";

    private FragmentClientDetailBinding binding;
    private ClientsViewModel viewModel;
    private ClientColorFormulasAdapter colorAdapter;
    @Nullable
    private ClientEntity currentClient;
    private List<ColorFormula> latestColorFormulas = Collections.emptyList();
    private Set<String> closedFormulaIds = Collections.emptySet();
    private Map<String, String> professionalNames = Collections.emptyMap();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentClientDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String clientId = getArguments() == null ? null : getArguments().getString(ARG_CLIENT_ID);
        if (clientId == null || clientId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.client_detail_not_found, Toast.LENGTH_SHORT)
                    .show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        String trimmedClientId = clientId.trim();
        ClientVisitsAdapter visitsAdapter = new ClientVisitsAdapter(card -> {
            Bundle args = new Bundle();
            args.putString(AppointmentDetailFragment.ARG_APPOINTMENT_ID, card.id);
            NavHostFragment.findNavController(this).navigate(R.id.nav_appointment_detail, args);
        });
        binding.clientVisitsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.clientVisitsRecycler.setAdapter(visitsAdapter);

        ClientSpendingAdapter spendingAdapter = new ClientSpendingAdapter();
        binding.clientSpendingRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.clientSpendingRecycler.setAdapter(spendingAdapter);

        colorAdapter = new ClientColorFormulasAdapter(
                new ClientColorFormulasAdapter.OnFormulaActionListener() {
                    @Override
                    public void onEditOrView(@NonNull ColorFormula formula, boolean locked) {
                        ColorFormulaDialogFragment.newEditInstance(
                                trimmedClientId, formula.id(), locked)
                                .show(getChildFragmentManager(), TAG_COLOR_FORMULA);
                    }

                    @Override
                    public void onDuplicate(@NonNull ColorFormula formula) {
                        viewModel.duplicateColorFormula(formula.id());
                    }

                    @Override
                    public void onDelete(@NonNull ColorFormula formula) {
                        ConfirmDialog.show(
                                requireContext(),
                                R.string.color_formula_delete_confirm_title,
                                R.string.color_formula_delete_confirm_message,
                                R.string.color_formula_delete,
                                true,
                                () -> viewModel.deleteColorFormula(formula.id())
                        );
                    }
                });
        binding.clientColorRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.clientColorRecycler.setAdapter(colorAdapter);

        viewModel = new ViewModelProvider(this).get(ClientsViewModel.class);
        viewModel.observeClientById(trimmedClientId).observe(getViewLifecycleOwner(), entity -> {
            currentClient = entity;
            if (entity != null) {
                bindClient(entity);
            }
        });
        viewModel.observeClientVisits(trimmedClientId).observe(getViewLifecycleOwner(), visits -> {
            boolean hasVisits = visits != null && !visits.isEmpty();
            visitsAdapter.submitList(hasVisits ? visits : Collections.emptyList());
            binding.clientVisitsRecycler.setVisibility(hasVisits ? View.VISIBLE : View.GONE);
            binding.clientTabVisitsEmpty.setVisibility(hasVisits ? View.GONE : View.VISIBLE);
            if (hasVisits) {
                binding.clientDetailVisitsCount.setVisibility(View.VISIBLE);
                binding.clientDetailVisitsCount.setText(
                        getString(R.string.client_detail_visits_count_format, visits.size()));
            } else {
                binding.clientDetailVisitsCount.setVisibility(View.GONE);
            }
        });
        viewModel.observeClientPendingBalanceCents(trimmedClientId).observe(
                getViewLifecycleOwner(),
                pendingCents -> binding.clientDetailBalanceValue.setMoneyCents(
                        pendingCents == null ? 0L : pendingCents));
        viewModel.observeClientTotalSpentCents(trimmedClientId).observe(
                getViewLifecycleOwner(),
                totalSpentCents -> binding.clientDetailTotalSpentValue.setMoneyCents(
                        totalSpentCents == null ? 0L : totalSpentCents));
        viewModel.observeClientMovements(trimmedClientId).observe(
                getViewLifecycleOwner(),
                movements -> {
                    boolean hasSpending = movements != null && !movements.isEmpty();
                    spendingAdapter.submitList(hasSpending ? movements : Collections.emptyList());
                    binding.clientSpendingRecycler.setVisibility(
                            hasSpending ? View.VISIBLE : View.GONE);
                    binding.clientTabSpendingEmpty.setVisibility(
                            hasSpending ? View.GONE : View.VISIBLE);
                });
        viewModel.observeClientColorFormulas(trimmedClientId).observe(
                getViewLifecycleOwner(),
                formulas -> {
                    latestColorFormulas = formulas == null ? Collections.emptyList() : formulas;
                    renderColorTab();
                });
        viewModel.observeClosedFormulaIdsByClient(trimmedClientId).observe(
                getViewLifecycleOwner(),
                ids -> {
                    closedFormulaIds = ids == null
                            ? Collections.emptySet()
                            : new HashSet<>(ids);
                    renderColorTab();
                });
        viewModel.observeProfessionals(true).observe(
                getViewLifecycleOwner(),
                professionals -> {
                    Map<String, String> names = new HashMap<>();
                    if (professionals != null) {
                        for (ProfessionalWithDetails item : professionals) {
                            names.put(item.professional.id, item.professional.name);
                        }
                    }
                    professionalNames = names;
                    renderColorTab();
                });
        binding.btnNewColorFormula.setOnClickListener(v ->
                ColorFormulaDialogFragment.newCreateInstance(trimmedClientId)
                        .show(getChildFragmentManager(), TAG_COLOR_FORMULA));
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });

        getChildFragmentManager().setFragmentResultListener(
                ClientFormDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String type = result.getString(ClientFormDialogFragment.KEY_RESULT_TYPE);
                    String existingId = result.getString(ClientFormDialogFragment.KEY_CLIENT_ID);
                    if (ClientFormDialogFragment.RESULT_OPEN_EXISTING.equals(type)
                            && existingId != null) {
                        openOtherClient(existingId);
                    }
                }
        );

        binding.btnEditClient.setOnClickListener(v -> {
            if (currentClient != null) {
                ClientFormDialogFragment.newEditInstance(currentClient)
                        .show(getChildFragmentManager(), TAG_EDIT_CLIENT);
            }
        });

        binding.btnToggleClientActive.setOnClickListener(v -> toggleActiveState());
        binding.btnClientAddWaitlist.setOnClickListener(v -> {
            if (currentClient != null) {
                WaitlistFormDialogFragment.newCreatePrefilledInstance(
                        currentClient.id, currentClient.name, null, null, null)
                        .show(getChildFragmentManager(), TAG_ADD_WAITLIST);
            }
        });
        setupTabs();
    }

    private void bindClient(@NonNull ClientEntity entity) {
        binding.clientDetailName.setText(entity.name);
        binding.clientDetailInactiveBadge.setVisibility(entity.active ? View.GONE : View.VISIBLE);
        binding.btnClientAddWaitlist.setVisibility(entity.active ? View.VISIBLE : View.GONE);
        binding.clientDetailPhone.setText(PhoneNormalizer.formatForDisplay(entity.phone));
        binding.clientDetailNoShowsValue.setText(String.valueOf(entity.noShowCount));
        binding.clientDetailBirthdayValue.setText(formatBirthday(entity));
        binding.clientDetailNotesValue.setText(
                entity.notes.isEmpty()
                        ? getString(R.string.client_detail_notes_empty)
                        : entity.notes);
        binding.btnToggleClientActive.setText(entity.active
                ? R.string.client_detail_deactivate
                : R.string.client_detail_reactivate);
    }

    private String formatBirthday(@NonNull ClientEntity entity) {
        if (entity.birthDay == null || entity.birthMonth == null) {
            return getString(R.string.client_detail_birthday_empty);
        }
        if (entity.birthYear != null) {
            return getString(R.string.client_detail_birthday_full,
                    entity.birthDay, entity.birthMonth, entity.birthYear);
        }
        return getString(R.string.client_detail_birthday_day_month,
                entity.birthDay, entity.birthMonth);
    }

    private void toggleActiveState() {
        if (currentClient == null) {
            return;
        }
        if (currentClient.active) {
            String targetId = currentClient.id;
            ConfirmDialog.show(
                    requireContext(),
                    R.string.client_deactivate_confirm_title,
                    R.string.client_deactivate_confirm_message,
                    R.string.client_detail_deactivate,
                    true,
                    () -> viewModel.setClientActive(targetId, false)
            );
        } else {
            viewModel.setClientActive(currentClient.id, true);
        }
    }

    private void setupTabs() {
        showTabPanel(0);
        binding.clientDetailTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTabPanel(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                showTabPanel(tab.getPosition());
            }
        });
    }

    private void renderColorTab() {
        if (binding == null || colorAdapter == null) {
            return;
        }
        boolean hasFormulas = !latestColorFormulas.isEmpty();
        if (!hasFormulas) {
            colorAdapter.submitList(Collections.emptyList());
            binding.clientColorRecycler.setVisibility(View.GONE);
            binding.clientTabColorEmpty.setVisibility(View.VISIBLE);
            return;
        }
        List<FormulaRowItem> rows = new ArrayList<>(latestColorFormulas.size());
        for (ColorFormula formula : latestColorFormulas) {
            String proName = formula.professionalId()
                    .map(id -> professionalNames.get(id))
                    .orElse(null);
            boolean locked = closedFormulaIds.contains(formula.id());
            rows.add(new FormulaRowItem(formula, proName, locked));
        }
        colorAdapter.submitList(rows);
        binding.clientColorRecycler.setVisibility(View.VISIBLE);
        binding.clientTabColorEmpty.setVisibility(View.GONE);
    }

    private void showTabPanel(int index) {
        binding.clientTabVisitsPanel.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        binding.clientTabSpendingPanel.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        binding.clientTabColorPanel.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void openOtherClient(@NonNull String clientId) {
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientId);
        NavHostFragment.findNavController(this).navigate(R.id.nav_client_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
