package com.enderthor.kremote.services;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class ServiceHandler {

    private Looper looper;
    private Handler handler;

    public ServiceHandler() {
        Thread thread = new Thread(() -> {
            Looper.prepare();
            this.looper = Looper.myLooper();
            assert this.looper != null;
            this.handler = new RunnableHandler(this.looper);
            Looper.loop();
        }, "ServiceHandler");
        thread.start();

        while (this.handler == null) {
            Thread.yield();
        }
    }

    public void dispose() {
        this.looper.quit();
    }

    private static class RunnableHandler extends Handler {

        public RunnableHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message message) {
            try {
                super.handleMessage(message);
            } catch (Exception e) {
                Timber.e(e, "Error handling message");
            }
        }
    }
}
