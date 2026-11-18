package com.metanoia.app;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ejecutores compartidos para operaciones de E/S, cómputo y despacho al hilo principal.
 */
public final class AppExecutors {

    private final ExecutorService diskIo;
    private final ExecutorService computation;
    private final Executor mainThread;

    public AppExecutors() {
        this(
                Executors.newFixedThreadPool(3),
                Executors.newFixedThreadPool(2),
                new MainThreadExecutor()
        );
    }

    public AppExecutors(ExecutorService diskIo, ExecutorService computation, Executor mainThread) {
        this.diskIo = diskIo;
        this.computation = computation;
        this.mainThread = mainThread;
    }

    public ExecutorService io() {
        return diskIo;
    }

    public ExecutorService computation() {
        return computation;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static final class MainThreadExecutor implements Executor {
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainHandler.post(command);
        }
    }
}
