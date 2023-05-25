/*
 * Copyright (c) 2021 National Institute of Informatics
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package jp.ad.sinet.stream.android.helper;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;
import jp.ad.sinet.stream.android.helper.constants.LocationProviderType;
import jp.ad.sinet.stream.android.helper.util.AppInfo;
import jp.ad.sinet.stream.android.helper.util.DialogUtil;

public class LocationTracker {
    private static final String TAG = LocationTracker.class.getSimpleName();

    /**
     * Messenger for communicating with the service.
     */
    private Messenger mService = null;

    private boolean mIsBound = false;
    private final AppCompatActivity mActivity;
    private final LocationTrackerListener mListener;
    private final Context mContext;
    private final AppInfo mAppInfo;
    private final int mClientId;

    private final ActivityResultLauncher<Intent> mActivityResultLauncher;

    private final LocationProviderType mLocationProviderType;

    public LocationTracker(
            @NonNull AppCompatActivity activity,
            @NonNull LocationProviderType locationProviderType,
            int clientId) {
        if (activity instanceof LocationTrackerListener) {
            mContext = activity;
            mListener = (LocationTrackerListener) activity;
        } else {
            throw new RuntimeException(activity
                    + " must implement LocationTrackerListener");
        }

        mActivity = activity;
        mAppInfo = new AppInfo(activity);
        mLocationProviderType = locationProviderType;
        mClientId = clientId;

        /*
         * https://developer.android.com/training/basics/intents/result#register
         */
        mActivityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Log.d(TAG, "ActivityResultLauncher.onActivityResult: result=" + result);
                        processActivityResult(result);
                    }
                }
        );
    }

    /**
     * Starts the location service.
     * <p>
     *     Here assumes that all required permission checks (both for system
     *     level and per-application level settings) have passed via
     *     {@link PermissionHandler}.
     * </p>
     */
    public void start() {
        startLocationService();
    }

    /**
     * Stops the location service if it's running.
     */
    public void stop() {
        stopLocationService();
    }

    private void launchSystemSettings() {
        if (mActivity.isFinishing() || mActivity.isDestroyed()) {
            Log.d(TAG, "Calling Activity is now finishing. Do nothing here");
            return;
        }
        Log.d(TAG, "Going to launch System Settings");

        /*
         * https://developer.android.com/training/basics/intents/result#launch
         */
        Intent settingsIntent =
                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        try {
            Log.d(TAG, "Going to launch ActivityResultLauncher");
            mActivityResultLauncher.launch(settingsIntent);
        } catch (ActivityNotFoundException e) {
            mListener.onError(TAG + ": ActivityResultLauncher.launch: " + e.getMessage());
        }
    }

    private void processActivityResult(@NonNull ActivityResult result) {
        Log.d(TAG, "processActivityResult");
        /*
         * It seems the invoked sub-Activity (System Settings)
         * does not call Activity.setResult() before finish.
         * That is, resultCode is always RESULT_CANCELED.
         *
        if (result.getResultCode() == RESULT_OK) {
            locationStart();
        }
         */
        if (verifyDeviceLocationSettings()) {
            checkRuntimePermissions();
        }
    }

    private boolean verifyDeviceLocationSettings() {
        LocationManager locationManager =
                (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            mListener.onError("LocationManager is NOT available on this system");
            return false;
        }

        boolean isLocationEnabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLocationEnabled = locationManager.isLocationEnabled();
            Log.d(TAG, "LocationManager.isLocationEnabled: " + isLocationEnabled);
        } else {
            int mode = Settings.Secure.getInt(
                    mContext.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            Log.d(TAG, "Settings: Location: mode(" + mode + ")");
            isLocationEnabled = (mode != Settings.Secure.LOCATION_MODE_OFF);
        }

        if (isLocationEnabled) {
            Log.d(TAG, "Settings: Device location is ENABLED");

            /* Global location settings seems OK; how about location provider? */
            switch (mLocationProviderType) {
                case GPS:
                    if (locationManager.
                            isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Log.d(TAG, "LocationManager(gps): enabled");
                    } else {
                        /*
                         * If location resolution is not high enough, GPS is not available.
                         * Prompt user to check system Settings.
                         */
                        DialogUtil dialogUtil = new DialogUtil(mContext);
                        dialogUtil.showModalDialog(
                                mAppInfo.getApplicationName(),
                                "To enable GPS, set location mode either \"High accuracy\" or \"Device only\".",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        launchSystemSettings();
                                    }
                                });
                        return false;
                    }
                    break;
                case FUSED:
                    if (locationManager.
                            isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                        Log.d(TAG, "LocationManager(fused): enabled");
                    } else {
                        mListener.onError("LocationManager: \n" +
                                "Sorry, FLP is not available on this device. " +
                                "Please use GPS instead");
                        return false;
                    }
                    break;
                default:
                    /* No such case */
                    break;
            }
        } else {
            Log.w(TAG, "Settings: Device location is DISABLED");
            //mListener.onError("Device location is NOT set");
            return false;
        }

        /* It seems all OK, so far. */
        return true;
    }

    private void checkRuntimePermissions() {
        /*
         * To access the GPS data via LocationManager.requestLocationUpdates(),
         * required permissions (ACCESS_FINE_LOCATION) must have granted.
         * Note that there are COARSE and FINE location permissions, but both
         * are in the same permission group and FINE is stricter. So we ask
         * permission just for the FINE location.
         */
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mListener.onError(TAG + ": App-level permission " +
                    "\"ACCESS_FINE_LOCATION\" has lost?");
        } else {
            Log.d(TAG, "OK, location resolution has upgraded");
            reportLocationResolutionCorrected();
        }
    }

    @Nullable
    private Intent getLocationServiceIntent() {
        Intent intent = null;
        switch (mLocationProviderType) {
            case GPS:
                intent = new Intent(mContext, GpsService.class);
                break;
            case FUSED:
                intent = new Intent(mContext, FlpService.class);
                break;
            default:
                Log.e(TAG, "getLocationServiceIntent: Calling sequence failure");
                break;
        }
        return intent;
    }

    private void startLocationService() {
        Intent intent = getLocationServiceIntent();
        if (intent == null) {
            mListener.onError("Cannot determine location service type");
            return;
        }

        Log.d(TAG, "Going to start location service");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(intent);
            } else {
                mContext.startService(intent);
            }
        } catch (SecurityException | IllegalStateException e) {
            mListener.onError(
                    "Cannot start location service: " + e.getMessage());
        }
    }

    private void stopLocationService() {
        Intent intent = getLocationServiceIntent();
        if (intent == null) {
            mListener.onError("Cannot determine location service type");
            return;
        }

        Log.d(TAG, "Going to stop location service");
        try {
            mContext.stopService(intent);
        } catch (SecurityException | IllegalStateException e) {
            mListener.onError(
                    "Cannot stop location service: " + e.getMessage());
        }
    }

    /**
     * Binds the location service to open a communication line.
     * <p>
     *     Once the connection has established, we can send/receive IPC messages
     *     with the location service.
     * </p>
     */
    public void bindLocationService() {
        Log.d(TAG, "bindLocationService");

        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        if (mIsBound) {
            Log.w(TAG, "bindLocationService: Already bound?");
        } else {
            Intent intent = getLocationServiceIntent();
            if (intent == null) {
                mListener.onError("Cannot determine location service type");
                return;
            }
            try {
                mIsBound = mContext.bindService(
                        intent,
                        mConnection,
                        Context.BIND_AUTO_CREATE);
                if (!mIsBound) {
                    Log.w(TAG, "bindLocationService: bindService failed?");
                }
            } catch (SecurityException e) {
                mListener.onError(TAG + ": bindLocationService: " + e.getMessage());
            }
        }
    }

    /**
     * Unbinds the location service.
     */
    public void unbindLocationService() {
        Log.d(TAG, "unbindLocationService");

        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (mService != null) {
            try {
                Message msg = Message.obtain(null,
                        IpcType.MSG_UNREGISTER_CLIENT,
                        0, mClientId);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                mListener.onError(TAG + ": unbindLocationService: " + e.getMessage());
            }

            // Detach out existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    /**
     * IPC endpoint to send messages to Service.
     */
    private final Messenger mMessenger =
            new Messenger(new LocationTracker.IncomingHandler(
                    Looper.getMainLooper(),this));

    /**
     * Handler for incoming messages from Service.
     */
    private static class IncomingHandler extends Handler {
        final WeakReference<LocationTracker> weakReference;

        /*
         * Default constructor in android.os.Handler is deprecated.
         * We need to use an Executor or specify the Looper explicitly.
         */
        IncomingHandler(
                @NonNull Looper looper, @NonNull LocationTracker locationTracker) {
            super(looper);

            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(locationTracker);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg A Message object containing a bundle of data
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            LocationTracker LocationTracker = weakReference.get();
            if (LocationTracker != null) {
                LocationTracker.onServerMessageReceived(msg);
            } else {
                Log.w(TAG, "handleMessage: LocationTracker has gone");
            }
            super.handleMessage(msg);
        }
    }

    private void onServerMessageReceived(@NonNull Message msg) {
        int result_code = msg.arg1;
        Bundle bundle = msg.getData();

        switch (msg.what) {
            case IpcType.MSG_REGISTER_CLIENT:
                Log.d(TAG, "RX: REGISTER_CLIENT: result=" + result_code);
                requestLocationProviderStatus();
                break;
            case IpcType.MSG_UNREGISTER_CLIENT:
                Log.d(TAG, "RX: UNREGISTER_CLIENT: result=" + result_code);
                break;
            case IpcType.MSG_LOCATION_PROVIDER_STATUS:
                Log.d(TAG, "RX: LOCATION_PROVIDER_STATUS: result=" + result_code);
                if (bundle != null) {
                    String sources = bundle.getString(
                            BundleKeys.BUNDLE_KEY_LOCATION_SOURCES, "UNKNOWN");
                    boolean enabled = bundle.getBoolean(
                            BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER_STATUS, false);
                    Log.d(TAG, "LocationProviderStatus: " +
                            "sources(" + sources + "),enabled(" + enabled + ")");
                    if (enabled) {
                        mListener.onLocationEngaged(sources);
                        requestLocationStartUpdate();
                    } else {
                        mListener.onLocationDisengaged(sources);
                    }
                } else {
                    Log.w(TAG, "MSG_LOCATION_PROVIDER_STATUS: No bundle?");
                }
                break;
            case IpcType.MSG_LOCATION_DATA:
                if (bundle != null) {
                    Location location = bundle.getParcelable(
                            BundleKeys.BUNDLE_KEY_LOCATION_PARCELABLE);
                    if (location != null) {
                        mListener.onLocationDataReceived(location);
                    } else {
                        Log.w(TAG, "MSG_LOCATION_DATA: Invalid bundle: " + bundle);
                    }
                } else {
                    Log.w(TAG, "MSG_LOCATION_DATA: No bundle?");
                }
                break;
            case IpcType.MSG_LOCATION_RESOLUTION_REQUIRED:
                Log.d(TAG, "RX: LOCATION_RESOLUTION_REQUIRED: result=" + result_code);
                String dialogMessage = "Insufficient location resolution:\n";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dialogMessage += "Please turn on \"Google Location Accuracy\" in \"Location services\".";
                } else {
                    dialogMessage += "Please set location mode either \"High accuracy\" or \"Device only\".";
                }
                DialogUtil dialogUtil = new DialogUtil(mContext);
                dialogUtil.showModalDialog(
                        mAppInfo.getApplicationName(),
                        dialogMessage,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                launchSystemSettings();
                            }
                        });
                break;
            case IpcType.MSG_ERROR:
                if (bundle != null) {
                    String errmsg = bundle.getString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE);
                    if (errmsg != null) {
                        mListener.onError(errmsg);
                    } else {
                        Log.w(TAG, "MSG_ERROR: Invalid bundle: " + bundle);
                    }
                } else {
                    Log.w(TAG, "MSG_ERROR: No bundle?");
                }
                break;
            default:
                Log.e(TAG, "Unknown IpcType: " + msg.what);
                break;
        }
    }

    /**
     * Callback interfaces for Service connection management.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            Log.d(TAG, "ServiceConnection.onServiceConnected: " + name.toString());
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            Message msg = Message.obtain(
                    null, IpcType.MSG_REGISTER_CLIENT, 0, mClientId);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (an then reconnected if it can be restarted)
                // so there is no need to do anything here.
                Log.e(TAG, "Messenger.send: " + e.getMessage());
                return;
            }

            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d(TAG, "ServiceConnection.onServiceDisconnected: " + name.toString());
            mService = null;

            mIsBound = false;
        }
    };

    private void requestLocationProviderStatus() {
        Log.d(TAG, "requestLocationProviderStatus");
        sendMessage(IpcType.MSG_LOCATION_PROVIDER_STATUS);
    }

    private void requestLocationStartUpdate() {
        Log.d(TAG, "requestLocationStartUpdate");
        sendMessage(IpcType.MSG_LOCATION_START_UPDATES);
    }

    private void requestLocationStopUpdate() {
        Log.d(TAG, "requestLocationStopUpdate");
        sendMessage(IpcType.MSG_LOCATION_STOP_UPDATES);
    }

    private void reportLocationResolutionCorrected() {
        Log.d(TAG, "reportLocationResolutionCorrected");
        sendMessage(IpcType.MSG_LOCATION_RESOLUTION_CORRECTED);
    }

    private void sendMessage(int ipcType) {
        Message msg = Message.obtain(null, ipcType, 0, mClientId);
        msg.replyTo = mMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger.send: " + e.getMessage());
            mListener.onError("Cannot send message: " + e.getMessage());
        }
    }
}
