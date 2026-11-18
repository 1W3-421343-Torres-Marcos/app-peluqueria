package com.metanoia.app;

import android.app.Application;
import android.content.Context;

/**
 * Punto de entrada de la aplicación; inicializa el contenedor de dependencias y aplica el tema.
 */
public class MetanoiaApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        container.devicePrefs().applySavedTheme();
    }

    public AppContainer getContainer() {
        return container;
    }

    public static AppContainer container(Context context) {
        MetanoiaApp app = (MetanoiaApp) context.getApplicationContext();
        return app.getContainer();
    }
}
