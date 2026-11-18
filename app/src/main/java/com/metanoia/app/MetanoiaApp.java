package com.metanoia.app;

import android.app.Application;
import android.content.Context;

/**
 * Punto de entrada de la aplicación; inicializa el contenedor de dependencias.
 */
public class MetanoiaApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }

    public static AppContainer container(Context context) {
        MetanoiaApp app = (MetanoiaApp) context.getApplicationContext();
        return app.getContainer();
    }
}
