# Project-specific ProGuard / R8 rules for Metanoia.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room: entidades, DAOs, proyecciones, POJOs, AppDatabase e implementaciones generadas
-keep class com.metanoia.app.data.db.** { *; }

# Navigation Fragments y DialogFragments
-keep class * extends androidx.fragment.app.Fragment {
    public <init>();
}
-keepclassmembers class com.metanoia.app.ui.** extends androidx.fragment.app.Fragment {
    public <init>();
}

# ViewModels instanciados vía reflexión por ViewModelProvider
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Custom Views infladas desde XML y attrs personalizadas
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# WorkManager Workers locales
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Strip de cualquier llamada residual a android.util.Log en builds release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
