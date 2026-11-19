package com.metanoia.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.metanoia.app.databinding.ActivityMainBinding;
import com.metanoia.app.ui.lock.LockScreenDialogFragment;

/**
 * Actividad única que aloja la navegación principal de cuatro destinos.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_REMINDERS = "extra_open_reminders";

    private ActivityMainBinding binding;
    @Nullable
    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureLockScreenIfNeeded();
    }

    private void ensureLockScreenIfNeeded() {
        AppContainer container = MetanoiaApp.container(this);
        if (container.lockController().checkNeedsLock(container.clock().nowMillis())
                && !getSupportFragmentManager().isStateSaved()
                && getSupportFragmentManager().findFragmentByTag(LockScreenDialogFragment.TAG) == null) {
            LockScreenDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), LockScreenDialogFragment.TAG);
        }
    }

    @Override
    protected void onNewIntent(@Nullable Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(@Nullable Intent intent) {
        if (intent == null || navController == null) {
            return;
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_REMINDERS, false)) {
            intent.removeExtra(EXTRA_OPEN_REMINDERS);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.nav_reminders) {
                navController.navigate(R.id.nav_reminders);
            }
        }
    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHost == null) {
            return;
        }
        navController = navHost.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        binding.topBarBack.setOnClickListener(v -> navController.navigateUp());
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean isHome = id == R.id.nav_agenda;
            boolean isTopLevel = id == R.id.nav_agenda
                    || id == R.id.nav_clients
                    || id == R.id.nav_cash
                    || id == R.id.nav_more;

            binding.topBarBack.setVisibility(isTopLevel ? View.GONE : View.VISIBLE);
            binding.topBarLogo.setVisibility(isHome ? View.VISIBLE : View.GONE);
            binding.topBarTitle.setVisibility(isHome ? View.GONE : View.VISIBLE);
            if (!isHome) {
                binding.topBarTitle.setText(destination.getLabel());
            }
            binding.bottomNav.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);
        });
    }
}
