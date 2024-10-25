package com.enderthor.kremote;

import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.enderthor.kremote.services.IKRemoteService;
import com.enderthor.kremote.services.KRemoteService;

import timber.log.Timber;

public class KRemoteApplication extends Application {

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Timber.d("Service connected");
            IKRemoteService.Stub.asInterface(binder);
            handler.post(() -> {});
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.d("Service disconnected");
        }
    };


    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        checkAccessibilityPermission();
        if (BuildConfig.DEBUG) {
            // noinspection DataFlowIssue
            Timber.plant((Timber.Tree) (Object) new Timber.DebugTree() {

                @Override
                protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
                    Log.println(priority, tag, message + (t == null ? "" : "\n" + t.getMessage() + "\n" + Log.getStackTraceString(t)));
                }
            });
        } else {
            Timber.plant(new Timber.Tree() {
                @Override
                protected boolean isLoggable(@Nullable String tag, int priority) {
                    return priority > Log.DEBUG;
                }

                @Override
                protected void log(int priority, @Nullable String tag, @NonNull String message, @Nullable Throwable t) {
                    Log.println(priority, tag, message + (t == null ? "" : "\n" + t.getMessage() + "\n" + Log.getStackTraceString(t)));
                }
            });
        }

        Timber.d("KRemoteApplication started");

        bindService(KRemoteService.getIntent(), serviceConnection, BIND_AUTO_CREATE);
    }
    private void checkAccessibilityPermission() {
        Timber.d("Checking accessibility permission");
        int accessEnabled = 0;
        ContentResolver contentResolver = getContentResolver();
        try {
            accessEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Timber.e(e);
        }
        if (accessEnabled == 0) {

            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
    @Override
    public void onTerminate() {
        Timber.d("KRemoteApplication terminated");
        super.onTerminate();
    }
}