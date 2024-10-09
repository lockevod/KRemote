package com.enderthor.kremote.services;

import android.app.Service;
import android.content.ComponentName;

import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.content.ContentResolver;

import java.util.EnumSet;
import java.util.Objects;

import com.enderthor.kremote.utils.KarooKey;

import com.dsi.ant.plugins.antplus.pcc.controls.AntPlusGenericControllableDevicePcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;
import com.dsi.ant.plugins.antplus.pcc.controls.defines.CommandStatus;
import com.dsi.ant.plugins.antplus.pcc.controls.defines.GenericCommandNumber;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
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
    private boolean isAccessServiceEnabled()
    {
        int accessEnabled = 0;
        ContentResolver contentResolver = getContentResolver();
        try {
            accessEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Timber.e(e);
        }
        return accessEnabled != 0;
    }
    private final IKRemoteService.Stub binder = new IKRemoteService.Stub() {
    };
    private ServiceHandler serviceHandler;


    //ANT REmote start
    private AntPlusGenericControllableDevicePcc remotePcc = null;
    private PccReleaseHandle<AntPlusGenericControllableDevicePcc> remoteReleaseHandle = null;
    private PccReleaseHandle<AntPlusGenericControllableDevicePcc> remoteReleaseHandle2 = null;


    private final AntPluginPcc.IPluginAccessResultReceiver<AntPlusGenericControllableDevicePcc> mRemoteResultReceiver = new AntPluginPcc.IPluginAccessResultReceiver<AntPlusGenericControllableDevicePcc>()
    {
        @Override
        public void onResultReceived(AntPlusGenericControllableDevicePcc result,
                                     RequestAccessResult resultCode, DeviceState initialDeviceState)
        {
            if (resultCode == RequestAccessResult.SUCCESS) {
                remotePcc = result;
                Timber.d( "[Remote]" + result.getDeviceName() + ": " + initialDeviceState + "antnumber : " + result.getAntDeviceNumber());
            }
        }
    };

    private final AntPluginPcc.IDeviceStateChangeReceiver mRemoteDeviceStateChangeReceiver = new AntPluginPcc.IDeviceStateChangeReceiver() {
        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState)
        {
            Timber.d( remotePcc.getDeviceName() + " onDeviceStateChange:" + newDeviceState);
            if(newDeviceState == DeviceState.DEAD) remote_key2();
        }
    };

    private final AntPlusGenericControllableDevicePcc.IGenericCommandReceiver mRemoteCommand = new AntPlusGenericControllableDevicePcc.IGenericCommandReceiver () {
        @Override
        public CommandStatus onNewGenericCommand(final long estTimestamp, final EnumSet<EventFlag> eventFlags, final int serialNumber,
                                                 final int manufacturerID, final int sequenceNumber, final GenericCommandNumber commandNumber) {
            Thread threadCommand=  new Thread(() -> {

                if (commandNumber == GenericCommandNumber.MENU_DOWN) {
                    Timber.d("RIGHT");
                    if (KRemoteListen.bServiceRunning || isAccessServiceEnabled()) {
                        Objects.requireNonNull(KRemoteListen.getInstance()).doActionKarooScreen(KarooKey.RIGHT);
                    }
                }
                if (commandNumber == GenericCommandNumber.LAP) {
                    Timber.d("LAP");
                    if (KRemoteListen.bServiceRunning || isAccessServiceEnabled()) {
                        Objects.requireNonNull(KRemoteListen.getInstance()).doActionKarooScreen(KarooKey.BACK);
                    }
                }

                if (commandNumber == GenericCommandNumber.UNRECOGNIZED) {
                    Timber.d("MAP");
                    if (KRemoteListen.bServiceRunning || isAccessServiceEnabled()) {
                        Objects.requireNonNull(KRemoteListen.getInstance()).doActionKarooScreen(KarooKey.VIRTUAL_SWITCH_TO_MAP_PAGE);
                    }
                }

            });
            threadCommand.start();
            return CommandStatus.PASS;
        }
    };

    // ANT end

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    @Override
    public void onCreate() {

        Timber.d( "Init kservices oncreate");
        serviceHandler = new ServiceHandler();
        remote_key();
    }

    private void remote_key()
    {
        close_ant_handler(remoteReleaseHandle);
        remoteReleaseHandle = AntPlusGenericControllableDevicePcc.requestAccess(this, mRemoteResultReceiver, mRemoteDeviceStateChangeReceiver, mRemoteCommand, 0);
    }
    private void remote_key2()
    {
        close_ant_handler(remoteReleaseHandle2);
        remoteReleaseHandle2 = AntPlusGenericControllableDevicePcc.requestAccess(this, mRemoteResultReceiver, mRemoteDeviceStateChangeReceiver, mRemoteCommand, 0);
    }
    private void close_ant_handler(PccReleaseHandle<AntPlusGenericControllableDevicePcc> remHandle)
    {
        if (remHandle!=null)
        {
            remHandle.close();
            remHandle=null;
        }
    }

    @Override
    public void onDestroy() {
        close_ant_handler(remoteReleaseHandle);
        close_ant_handler(remoteReleaseHandle2);
        serviceHandler.dispose();
        super.onDestroy();
    }
}