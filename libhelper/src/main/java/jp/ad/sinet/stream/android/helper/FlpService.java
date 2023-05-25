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

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;

public class FlpService extends Service implements Executor {
    private static final String TAG = FlpService.class.getSimpleName();

    /** Keep track of all current registered clients */
    private final List<Messenger> mClients = new ArrayList<>();

    private static final String FLP_NOTIFICATION_CHANNEL_ID =
            TAG + ".notification_channel";
    private final int NOTIFICATION_ID = R.string.service_name_flp;

    private LocationSettingsStates mLocationSettingsStates = null;
    private Integer mLocationSettingsProblemCode = null;
    private boolean mHasPendingLocationProviderStatus = false;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    private boolean mRequestingLocationUpdates = false;
    private boolean mIsLocationAvailable = false;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        NotificationManager notificationManager=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String name = getString(R.string.service_name_flp);
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel notificationChannel =
                        new NotificationChannel(
                                FLP_NOTIFICATION_CHANNEL_ID, name, importance);

                notificationManager.createNotificationChannel(notificationChannel);
            }
        } else {
            Log.w(TAG, "getSystemService(NOTIFICATION_SERVICE) is null");
        }

        turnOnForegroundMode();

        mRequestingLocationUpdates = false;
        mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient =
                LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        checkLocationSettings();
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopLocationUpdates();
        turnOffForegroundMode();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // return super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand: Intent=" +
                (intent != null ? intent.toString() : "") +
                ", flags=" + flags +
                ", startId=" + startId);

        // We don't want this service being automatically restarted by
        // Android TaskManager.
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Intent="
                + (intent != null ? intent.toString() : ""));
        return mMessenger.getBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind: Intent="
                + (intent != null ? intent.toString() : ""));
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: Intent="
                + (intent != null ? intent.toString() : ""));
        return super.onUnbind(intent);
    }

    @Override
    public void execute(Runnable runnable) {
        /* Implementation of Executor */
        /*
         * https://developer.android.com/reference/java/util/concurrent/Executor
         */
        new Thread(runnable).start();
    }

    private void checkLocationSettings() {
        Log.d(TAG, "checkLocationSettings");

        // Begin by checking if the device has the necessary location settings.
        Task<LocationSettingsResponse> task1 =
                mSettingsClient.checkLocationSettings(mLocationSettingsRequest);

        task1.addOnSuccessListener(this, new OnSuccessListener<>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i(TAG, "All location settings are satisfied.");

                LocationSettingsStates states =
                        locationSettingsResponse.getLocationSettingsStates();
                if (states != null) {
                    mIsLocationAvailable = states.isLocationUsable();
                    dumpLocationSettings(states);
                }
                mLocationSettingsStates = states;

                if (mHasPendingLocationProviderStatus) {
                    mHasPendingLocationProviderStatus = false;
                    if (mClients.size() > 0) {
                        Log.d(TAG, "PENDING: Going to report Settings check results");
                        reportLocationProviderStatus(null);
                    } else {
                        Log.e(TAG, "PENDING: No clients?");
                    }
                }
            }
        });

        task1.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ApiException) {
                    int statusCode = ((ApiException) e).getStatusCode();
                    Log.w(TAG, "LocationSettingsResponse: FAILURE: " +
                            LocationSettingsStatusCodes.getStatusCodeString(statusCode) +
                            " (" + statusCode + ")");
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings ");
                            /*
                             * The standard way to handle this case should be to call
                             * "ResolvableApiException.startResolutionForResult()"
                             * and check its result via "Activity.onActivityResult()".
                             * as shown below.
                             *
                             * Our challenge is to keep complex work within this library
                             * without bothering user applications.
                             * See "reportLocationResolutionRequired()" as our solution.
                             *
                             * Google Play Service: SettingsClient
                             * https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
                             *
                            try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(
                                        mActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                             */
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String errorMessage = "Location settings are inadequate, and cannot be " +
                                    "fixed here. Fix in Settings.";
                            Log.e(TAG, errorMessage);
                            break;
                        default:
                            break;
                    }
                    mLocationSettingsProblemCode = statusCode;

                    if (mHasPendingLocationProviderStatus) {
                        mHasPendingLocationProviderStatus = false;
                        if (mClients.size() > 0) {
                            Log.d(TAG, "PENDING: Going to report Settings error");
                            reportLocationSettingsError(null);
                        } else {
                            Log.e(TAG, "PENDING: No clients?");
                        }
                    }
                } else {
                    Log.e(TAG, "LocationSettingsResponse: " + e.getMessage());
                }
            }
        });

        task1.addOnCanceledListener(this, new OnCanceledListener() {
            @Override
            public void onCanceled() {
                Log.w(TAG, "LocationSettingsResponse.onCanceled");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");

        if (mRequestingLocationUpdates) {
            Log.d(TAG, "LocationUpdates is already running.");
            return;
        }

        Log.d(TAG, "Going to call FusedLocationClient.requestLocationUpdates");
        try {
            /*
             * Calling Looper.prepare() here causes RuntimeException
             * java.lang.RuntimeException: Only one Looper may be created per thread
             */
            //Looper.prepare();
            mFusedLocationClient.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper());
            mRequestingLocationUpdates = true;
            Looper.loop();
        } catch (IllegalStateException e) {
            Log.e(TAG, "FusedLocationClient.requestLocationUpdates: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates");

        if (! mRequestingLocationUpdates) {
            Log.d(TAG, "LocationUpdates: No running requests");
            return;
        }

        Task<Void> task1 = mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        task1.addOnCompleteListener(this, new OnCompleteListener<>() {
            @Override
            public void onComplete(@NonNull Task<Void> task2) {
                Log.d(TAG, "removeLocationUpdates.onComplete: " +
                        (task2.isSuccessful() ? "SUCCESS" : "FAILURE"));
                mRequestingLocationUpdates = false;
            }
        });
    }

    private void createLocationRequest() {
        /*
         * From the release of play-services-location (v21.0.0),
         * LocationRequest.create() has deprecated.
         * We should use LocationRequest.Builder instead.
         *
         * https://developers.google.com/android/guides/releases#october_13_2022
         */
        LocationRequest.Builder builder =
                new LocationRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS);
        builder.setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        builder.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        builder.setWaitForAccurateLocation(true);
        mLocationRequest = builder.build();
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(TAG, "onLocationResult: " + locationResult);

                Location lastLocation = locationResult.getLastLocation();
                if (lastLocation != null) {
                    reportNewLocation(lastLocation);
                } else {
                    Log.d(TAG, "Last location unavailable...");
                }
            }

            @Override
            public void onLocationAvailability(
                    @NonNull LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                Log.d(TAG, "onLocationAvailability: " + locationAvailability);
                /*
                 * When LocationAvailability.isLocationAvailable() returns false
                 * you can assume that location will not be returned in
                 * onLocationResult(LocationResult) until something changes
                 * in the device's settings or environment.
                 */
                /* TODO NOTYET
                if (mIsLocationAvailable != locationAvailability.isLocationAvailable()) {
                    Log.d(TAG, "LocationAvailability CHANGED: " +
                            mIsLocationAvailable +
                            " -> " +
                            locationAvailability.isLocationAvailable());
                    mIsLocationAvailable = locationAvailability.isLocationAvailable();
                    if (mClients.size() > 0) {
                        reportLocationProviderStatus(null);
                    } else {
                        Log.d(TAG, "No clients for now");
                    }
                } else {
                    Log.d(TAG, "LocationAvailability(" + mIsLocationAvailable + ") UNCHANGED");
                }

                if (mIsLocationAvailable) {
                    requestCurrentLocation();
                }
                 */
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        //builder.setNeedBle(true);

        mLocationSettingsRequest = builder.build();
    }

    private void dumpLocationSettings(@NonNull LocationSettingsStates states) {
        String s = "LocationSettings: \n";

        s += "Location: present(" + states.isLocationPresent() + "), usable(" +
                states.isLocationUsable() + ")\n";
        s += "Gps: present(" + states.isGpsPresent() + "), usable(" +
                states.isGpsUsable() + ")\n";
        s += "Net: present(" + states.isNetworkLocationPresent() + "), usable(" +
                states.isNetworkLocationUsable() + ")\n";
        s += "Ble: present(" + states.isBlePresent() + "), usable(" +
                states.isBleUsable() + ")\n";

        Log.d(TAG, s);
    }

    @Nullable
    private String getLocationSources() {
        String s = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ArrayList<String> arrayList = new ArrayList<>();
            s = LocationManager.FUSED_PROVIDER.toUpperCase(Locale.US);
            LocationSettingsStates states = mLocationSettingsStates;
            if (states != null && states.isLocationUsable()) {
                if (states.isGpsUsable()) {
                    arrayList.add("gps");
                }
                if (states.isNetworkLocationUsable()) {
                    arrayList.add("net");
                }
                if (states.isBleUsable()) {
                    arrayList.add("ble");
                }
            }
            s += arrayList.toString();
        }
        return s;
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocation() {
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            errorReply(null, "LocationManager is NOT available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Going to call getCurrentLocation");
            try {
                locationManager.getCurrentLocation(
                        LocationManager.FUSED_PROVIDER,
                        null,
                        getMainExecutor(),
                        new Consumer<>() {
                            @Override
                            public void accept(Location location) {
                                if (mClients.size() > 0) {
                                    reportNewLocation(location);
                                } else {
                                    Log.d(TAG, "No clients for now");
                                }
                            }
                        });
            } catch (IllegalArgumentException | SecurityException e) {
                Log.e(TAG, "LocationManager.getCurrentLocation: " + e);
                String errmsg = TAG + ": " + "getCurrentLocation: " + e.getMessage();
                errorReply(null, errmsg);
            }
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private Notification getNotification(@NonNull CharSequence message) {
        CharSequence text = getText(R.string.flp_service_running);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.service_name_flp))
                .setContentText(text)
                .setPriority(Notification.PRIORITY_DEFAULT);

        /*
         * Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel.
         * https://developer.android.com/training/notify-user/channels
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(FLP_NOTIFICATION_CHANNEL_ID);
        }

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }

    private void turnOnForegroundMode() {
        Notification notification =
                getNotification(getText(R.string.flp_service_running));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void turnOffForegroundMode() {
        stopForeground(true);
    }

    private void updateNotification(@NonNull String contentMessage) {
        NotificationManager notificationManager=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Notification notification = getNotification(contentMessage);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger =
            new Messenger(new FlpService.IncomingHandler(
                    Looper.getMainLooper(),this));

    /**
     * Handler for incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
        private final String TAG_HANDLER = FlpService.IncomingHandler.class.getSimpleName();
        final WeakReference<FlpService> weakReference;

        /*
         * Default constructor in android.os.Handler is deprecated.
         * We need to use an Executor or specify the Looper explicitly.
         */
        IncomingHandler(
                @NonNull Looper looper, @NonNull FlpService FlpService) {
            super(looper);

            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(FlpService);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg The {@link Message} object sent from a client.
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            FlpService FlpService = weakReference.get();
            if (FlpService != null) {
                FlpService.onClientMessageReceived(msg);
            } else {
                Log.w(TAG_HANDLER, "handleMessage: FlpService has gone");
            }
            super.handleMessage(msg);
        }
    }

    private void onClientMessageReceived(@NonNull Message msg) {
        switch (msg.what) {
            case IpcType.MSG_REGISTER_CLIENT:
                Log.d(TAG, "RX: REGISTER_CLIENT");
                mClients.add(msg.replyTo);
                successReply(msg.replyTo, msg.what);
                break;
            case IpcType.MSG_UNREGISTER_CLIENT:
                Log.d(TAG, "RX: UNREGISTER_CLIENT");
                mClients.remove(msg.replyTo);
                successReply(msg.replyTo, msg.what);
                break;
            case IpcType.MSG_LOCATION_PROVIDER_STATUS:
                Log.d(TAG, "RX: LOCATION_PROVIDER_STATUS");
                if (mLocationSettingsStates != null) {
                    reportLocationProviderStatus(msg.replyTo);
                } else if (mLocationSettingsProblemCode != null) {
                    reportLocationSettingsError(msg.replyTo);
                } else {
                    Log.d(TAG, "Nothing to report?");
                    /* There may be timing slip. Report when time comes. */
                    mHasPendingLocationProviderStatus = true;
                }
                break;
            case IpcType.MSG_LOCATION_START_UPDATES:
                Log.d(TAG, "RX: LOCATION_START_UPDATES");
                startLocationUpdates();
                break;
            case IpcType.MSG_LOCATION_STOP_UPDATES:
                Log.d(TAG, "RX: LOCATION_STOP_UPDATES");
                stopLocationUpdates();
                break;
            case IpcType.MSG_LOCATION_RESOLUTION_CORRECTED:
                Log.d(TAG, "RX: LOCATION_RESOLUTION_CORRECTED");
                mLocationSettingsProblemCode = null;
                checkLocationSettings();
                break;
            default:
                Log.w(TAG, "Unknown IPC message: " + msg.what);
                break;
        }
    }

    private void sendToClient(
            @Nullable Messenger client, int what, int result_code, @Nullable Bundle bundle) {
        try {
            /*
             * Get a message containing a description and arbitrary data object
             * that can be sent to a Handler.
             */
            Message msg = Message.obtain(null, what, result_code, 0);
            if (bundle != null) {
                msg.setData(bundle);
            }
            if (client != null) {
                client.send(msg);
            } else {
                for (int i = mClients.size() - 1; i >= 0; i--) {
                    Messenger client2 = mClients.get(i);
                    client2.send(msg);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger.send: " + e.getMessage());
        }
    }

    private void reportLocationProviderStatus(@Nullable Messenger client) {
        Bundle bundle = new Bundle();

        String s = getLocationSources();
        if (s != null) {
            Log.d(TAG, "reportLocationProviderStatus: " + s);
            bundle.putString(BundleKeys.BUNDLE_KEY_LOCATION_SOURCES, s);

            bundle.putBoolean(BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER_STATUS,
                    mIsLocationAvailable);
            Log.d(TAG, "TX: LOCATION_PROVIDER_STATUS");
            sendToClient(client, IpcType.MSG_LOCATION_PROVIDER_STATUS, 0, bundle);
        } else {
            errorReply(client,
                    "Too old Android OS(" + Build.VERSION.SDK_INT + "): " +
                            "Cannot get location provider status");
        }
    }

    private void reportLocationSettingsError(@Nullable Messenger client) {
        if (mLocationSettingsProblemCode != null) {
            String errmsg = null;
            switch (mLocationSettingsProblemCode) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    reportLocationResolutionRequired(client);
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    errmsg = "Location settings are inadequate, and cannot be " +
                            "fixed here. Fix in Settings.";
                    break;
                default:
                    errmsg = "LocationSettingsResponse: FAILURE: " +
                            LocationSettingsStatusCodes.getStatusCodeString(
                                    mLocationSettingsProblemCode) +
                            " (" + mLocationSettingsProblemCode + ")";
                    break;
            }

            if (errmsg != null) {
                errorReply(client, errmsg);
            }
        }
    }

    private void reportLocationResolutionRequired(@Nullable Messenger client) {
        Log.d(TAG, "reportLocationResolutionRequired");
        /*
         * Unfortunately, there is no simple way to pass
         * the ResolvableApiException to client.
         *
         * java.lang.ClassCastException: com.google.android.gms.common.api.ResolvableApiException cannot be cast to android.os.Parcelable
         */
        Log.d(TAG, "TX: LOCATION_RESOLUTION_REQUIRED");
        sendToClient(client, IpcType.MSG_LOCATION_RESOLUTION_REQUIRED, 0, null);
    }

    private void reportNewLocation(@NonNull Location location) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BundleKeys.BUNDLE_KEY_LOCATION_PARCELABLE, location);
        sendToClient(null, IpcType.MSG_LOCATION_DATA, 0, bundle);
    }

    private void successReply(@Nullable Messenger client, int ipcType) {
        sendToClient(client, ipcType, 0, null);
    }

    private void errorReply(@Nullable Messenger client, @NonNull String errmsg) {
        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE, errmsg);
        sendToClient(client, IpcType.MSG_ERROR, -1, bundle);
    }
}
