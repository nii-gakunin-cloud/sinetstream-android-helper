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
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;

public class GpsService extends Service {
    private static final String TAG = GpsService.class.getSimpleName();

    /** Keep track of all current registered clients */
    private final List<Messenger> mClients = new ArrayList<>();

    private LocationManager mLocationManager = null;
    private final GpsListener mGpsListener = new GpsListener();

    private static final String GPS_NOTIFICATION_CHANNEL_ID =
            TAG + ".notification_channel";
    private final int NOTIFICATION_ID = R.string.service_name_gps;

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
                String name = getString(R.string.service_name_gps);
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel notificationChannel =
                        new NotificationChannel(
                                GPS_NOTIFICATION_CHANNEL_ID, name, importance);

                notificationManager.createNotificationChannel(notificationChannel);
            }
        } else {
            Log.w(TAG, "getSystemService(NOTIFICATION_SERVICE) is null");
        }

        turnOnForegroundMode();
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
        cancelLocationUpdates();
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

    private void checkLocationSettings() {
        mLocationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager != null) {
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.d(TAG, "LocationManager: GPS provider is enabled");
                mIsLocationAvailable = true;
            } else {
                Log.d(TAG, "LocationManager: GPS provider is NOT enabled");
                mIsLocationAvailable = false;
            }
        } else {
            Log.w(TAG, "LocationManager is NOT available, give up");
            mIsLocationAvailable = false;
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        Log.d(TAG, "Going to request location updates");

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    50,
                    mGpsListener);
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "LocationManager.requestLocationUpdates: " + e);
            String errmsg = TAG + ": " + "requestLocationUpdates: " + e.getMessage();
            errorReply(null, errmsg);
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                ProviderProperties properties = mLocationManager.
                        getProviderProperties(LocationManager.GPS_PROVIDER);
                Log.d(TAG, "Provider(GPS): " + properties);
            } catch (IllegalArgumentException | SecurityException e) {
                Log.e(TAG, "LocationManager.getProviderProperties: " + e);
                String errmsg = TAG + ": " + "getProviderProperties: " + e.getMessage();
                errorReply(null, errmsg);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void cancelLocationUpdates() {
        Log.d(TAG, "Going to cancel location updates");

        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mGpsListener);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "LocationManager.removeUpdates: " + e);
                String errmsg = TAG + ": " + "removeUpdates: " + e.getMessage();
                errorReply(null, errmsg);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Going to call getCurrentLocation");
            try {
                mLocationManager.getCurrentLocation(
                        LocationManager.GPS_PROVIDER,
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
        } else {
            Log.d(TAG, "Going to call getLastKnownLocation");
            try {
                Location location = mLocationManager.
                        getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    if (mClients.size() > 0) {
                        reportNewLocation(location);
                    } else {
                        Log.d(TAG, "No clients for now");
                    }
                } else {
                    Log.d(TAG, "LastKnownLocation is NOT available");
                }
            } catch (IllegalArgumentException | SecurityException e) {
                Log.e(TAG, "LocationManager.getLastKnownLocation: " + e);
                String errmsg = TAG + ": " + "getLastKnownLocation: " + e.getMessage();
                errorReply(null, errmsg);
            }
        }
    }

    private class GpsListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d(TAG, "onLocationChanged: " + location);

            if (mClients.size() > 0) {
                reportNewLocation(location);
            } else {
                Log.d(TAG, "No clients for now");
            }
        }

        @Override
        public void onLocationChanged(@NonNull List<Location> locations) {
            for (int i = 0, n = locations.size(); i < n; i++) {
                Location location = locations.get(i);
                Log.d(TAG, "Location[" + (i+1) + "/" + n + "]: " + location.toString());
            }
        }

        @Override
        public void onFlushComplete(int requestCode) {
            Log.d(TAG, "onFlushComplete: requestCode=" + requestCode);
        }

        /**
         * @param provider the source of the location data
         * @param status internal status of the LocationProvider
         * @param extras any extra info
         * @deprecated
         */
        @Deprecated
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            /* Avoid runtime exception: java.lang.AbstractMethodError */
            /*
             * This method was deprecated in API level 29.
             * This callback will never be invoked on Android Q and above.
             */
            Log.d(TAG, "onStatusChanged: " +
                    "provider(" + provider + ")," +
                    "status(" + status + ")," +
                    "extras(" + extras + ")");

            /*
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    showMessage("Location: Out of Service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    showMessage("Location: Temporarily Unavailable");
                    break;
                case LocationProvider.AVAILABLE:
                    break;
            }
             */
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            /* Avoid runtime exception: java.lang.AbstractMethodError */
            Log.d(TAG, "onProviderEnabled: " + provider);

            mIsLocationAvailable = true;
            if (mClients.size() > 0) {
                reportLocationProviderStatus(null);
            } else {
                Log.d(TAG, "No clients for now");
            }
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            /* Avoid runtime exception: java.lang.AbstractMethodError */
            Log.d(TAG, "onProviderDisabled: " + provider);

            mIsLocationAvailable = false;
            if (mClients.size() > 0) {
                reportLocationProviderStatus(null);
            } else {
                Log.d(TAG, "No clients for now");
            }
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private Notification getNotification(@NonNull CharSequence message) {
        CharSequence text = getText(R.string.gps_service_running);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.service_name_gps))
                .setContentText(text)
                .setPriority(Notification.PRIORITY_DEFAULT);

        /*
         * Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel.
         * https://developer.android.com/training/notify-user/channels
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(GPS_NOTIFICATION_CHANNEL_ID);
        }

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }

    private void turnOnForegroundMode() {
        Notification notification =
                getNotification(getText(R.string.gps_service_running));

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
            new Messenger(new GpsService.IncomingHandler(
                    Looper.getMainLooper(),this));

    /**
     * Handler for incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
        private final String TAG_HANDLER = IncomingHandler.class.getSimpleName();
        final WeakReference<GpsService> weakReference;

        /*
         * Default constructor in android.os.Handler is deprecated.
         * We need to use an Executor or specify the Looper explicitly.
         */
        IncomingHandler(
                @NonNull Looper looper, @NonNull GpsService GpsService) {
            super(looper);

            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(GpsService);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg The {@link Message} object sent from a client.
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            GpsService GpsService = weakReference.get();
            if (GpsService != null) {
                GpsService.onClientMessageReceived(msg);
            } else {
                Log.w(TAG_HANDLER, "handleMessage: GpsService has gone");
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
                reportLocationProviderStatus(msg.replyTo);
                requestCurrentLocation();
                break;
            case IpcType.MSG_LOCATION_START_UPDATES:
                Log.d(TAG, "RX: LOCATION_START_UPDATES");
                requestLocationUpdates();
                break;
            case IpcType.MSG_LOCATION_STOP_UPDATES:
                Log.d(TAG, "RX: LOCATION_STOP_UPDATES");
                break;
            case IpcType.MSG_LOCATION_RESOLUTION_CORRECTED:
                Log.d(TAG, "RX: LOCATION_RESOLUTION_CORRECTED");
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
        bundle.putString(BundleKeys.BUNDLE_KEY_LOCATION_SOURCES,
                LocationManager.GPS_PROVIDER.toUpperCase(Locale.US));
        bundle.putBoolean(BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER_STATUS,
                mIsLocationAvailable);
        Log.d(TAG, "TX: LOCATION_PROVIDER_STATUS");
        sendToClient(client, IpcType.MSG_LOCATION_PROVIDER_STATUS, 0, bundle);
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
