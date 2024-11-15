package com.enderthor.kremote.services;

import android.app.Service;
import android.content.ComponentName;

import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.content.ContentResolver;

import java.util.EnumSet;
import java.util.Objects;

import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.enderthor.kremote.KRemoteKeys;

import com.dsi.ant.plugins.antplus.pcc.controls.AntPlusGenericControllableDevicePcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pcc.controls.defines.CommandStatus;
import com.dsi.ant.plugins.antplus.pcc.controls.defines.GenericCommandNumber;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import timber.log.Timber;

public class KRemoteService extends Service {

    /**
     * Get intent to bind to this service.
     */
    public static Intent getIntent() {
        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName("com.enderthor.kremote", "com.enderthor.kremote.services.KRemoteService"));
        return serviceIntent;
    }
    private boolean isAccessServiceEnabled=false;
    private boolean checkAccessServiceEnabled()
    {
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
        return accessEnabled != 0;
    }
    private final IKRemoteService.Stub binder = new IKRemoteService.Stub() {
    };

    //ANT Remote Code
    private AntPlusGenericControllableDevicePcc remotePcc = null;
    private PccReleaseHandle<AntPlusGenericControllableDevicePcc> remoteReleaseHandle = null;


    private final AntPluginPcc.IPluginAccessResultReceiver<AntPlusGenericControllableDevicePcc> mRemoteResultReceiver = (result, resultCode, initialDeviceState) -> {
        if (resultCode == RequestAccessResult.SUCCESS) {
            remotePcc = result;
            Timber.d("[Remote]" + result.getDeviceName() + ": " + initialDeviceState + "antnumber : " + result.getAntDeviceNumber());
        }
    };

    private final AntPluginPcc.IDeviceStateChangeReceiver mRemoteDeviceStateChangeReceiver = newDeviceState -> {
        Timber.d( remotePcc.getDeviceName() + " onDeviceStateChange:" + newDeviceState);
        if(newDeviceState == DeviceState.DEAD) {
            Timber.d("ANT DEAD");
            remote_key();
        }
    };

    private final AntPlusGenericControllableDevicePcc.IGenericCommandReceiver mRemoteCommand = this::onNewGenericCommand;

    // ANT end

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onCreate() {

        Timber.d( "Init kservices oncreate");
        remote_key();
    }

    private void remote_key()
    {
        Timber.d("Remote Key STARTED");
        close_ant_handler(remoteReleaseHandle);
        remoteReleaseHandle = AntPlusGenericControllableDevicePcc.requestAccess(this, mRemoteResultReceiver, mRemoteDeviceStateChangeReceiver, mRemoteCommand, 0);
    }


    private void close_ant_handler(PccReleaseHandle<AntPlusGenericControllableDevicePcc> remHandle)
    {
        Timber.d("Close ant handler");
        if (remHandle!=null) remHandle.close();
    }

    @Override
    public void onDestroy() {
        Timber.d( "kservices onDestroy");
        close_ant_handler(remoteReleaseHandle);
        super.onDestroy();
    }

    private CommandStatus onNewGenericCommand(long estTimestamp, EnumSet<EventFlag> eventFlags, int serialNumber, int manufacturerID, int sequenceNumber, GenericCommandNumber commandNumber) {
        Timber.d("Button pressed : %s", commandNumber);
        if (!isAccessServiceEnabled) isAccessServiceEnabled = checkAccessServiceEnabled();
        Timber.d("isAccessServiceEnabled : %s", isAccessServiceEnabled);
        Timber.d("ThreadCount :  %s ", Thread.activeCount());
        Thread actionthread = new Thread(() -> {
            Timber.d("Thread Started N:%s", Thread.currentThread().getName());
            if (isAccessServiceEnabled) {
                if (commandNumber == GenericCommandNumber.MENU_DOWN) {
                    Timber.d("IN ANTPLUS RIGHT");
                    Objects.requireNonNull(KRemoteListen.getInstance()).doActionKarooScreen(KRemoteKeys.RIGHT);
                }
                if (commandNumber == GenericCommandNumber.LAP) {
                    Timber.d("IN ANTPLUS BACK");
                    Objects.requireNonNull(KRemoteListen.getInstance()).doActionKarooScreen(KRemoteKeys.BACK);
                }
                if (commandNumber == GenericCommandNumber.UNRECOGNIZED) {
                    Timber.d("IN ANTPLUS  MAP");
                    Objects.requireNonNull(KRemoteListen.getInstance()).doActionKarooScreen(KRemoteKeys.MIDDLE);
                }
            }
        });
        actionthread.start();
        return CommandStatus.PASS;
    }
}