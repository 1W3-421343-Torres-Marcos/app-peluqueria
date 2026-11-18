package com.metanoia.app;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.metanoia.app.databinding.ActivityMainBinding;

/**
 * Actividad única que aloja la navegación principal de cuatro destinos.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
    }

    private void setupNavigation() {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHost == null) {
            return;
        }
        NavController navController = navHost.getNavController();
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
