package com.example.app;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AppTaskRunner implements AutoCloseable {
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean closed;

    <T> void run(Callable<T> task, Callback<T> success, Callback<Exception> failure) {
        if (closed) return;
        executor.execute(() -> {
            try {
                T value = task.call();
                mainHandler.post(() -> {
                    if (!closed) success.accept(value);
                });
            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (!closed) failure.accept(exception);
                });
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }

    interface Callback<T> {
        void accept(T value);
    }
}
