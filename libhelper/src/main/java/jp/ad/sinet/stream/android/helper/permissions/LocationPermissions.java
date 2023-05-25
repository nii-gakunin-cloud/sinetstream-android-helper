/*
 * Copyright (c) 2023 National Institute of Informatics
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

package jp.ad.sinet.stream.android.helper.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import jp.ad.sinet.stream.android.helper.FlpService;
import jp.ad.sinet.stream.android.helper.GpsService;
import jp.ad.sinet.stream.android.helper.LocationTracker;
import jp.ad.sinet.stream.android.helper.LocationTrackerListener;
import jp.ad.sinet.stream.android.helper.constants.LocationProviderType;
import jp.ad.sinet.stream.android.helper.util.AppInfo;
import jp.ad.sinet.stream.android.helper.util.DialogUtil;

public class LocationPermissions {
    private final String TAG = LocationPermissions.class.getSimpleName();

    private final Context mContext;
    private final Activity mActivity;
    private final AppInfo mAppInfo;
    private final LocationPermissionsListener mListener;

    private final ActivityResultLauncher<Intent> mActivityResultLauncher;

    private final LocationProviderType mLocationProviderType;

    public LocationPermissions(@NonNull AppCompatActivity activity,
                               @NonNull LocationProviderType locationProviderType,
                               @NonNull LocationPermissionsListener listener) {
        this.mContext = activity;
        this.mActivity = activity;
        this.mAppInfo = new AppInfo(activity);
        this.mLocationProviderType = locationProviderType;
        this.mListener = listener;

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
     * Check system settings before starting the location service.
     * <p>
     *     This method triggers the series of checks to see if current settings
     *     are adequate for using the LocationService.
     *
     *     If everything goes fine, {@link GpsService} or {@link FlpService}
     *     will be automatically started followed by a notification
     *     {@link LocationTrackerListener#onLocationSettingsChecked(boolean)}
     *     with {@code isReady} argument set to {@code true}.
     *     Otherwise, the same notification will be delivered with {@code isReady}
     *     argument set to {@code false}.
     * </p>
     */
    public void run() {
        if (checkDeviceLocationSettings(true)) {
            checkAppRuntimePermissions();
        }
    }

    private boolean checkDeviceLocationSettings(boolean promptIfDisabled) {
        Log.d(TAG, "checkDeviceLocationSettings: promptIfDisabled=" + promptIfDisabled);

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

            if (! promptIfDisabled) {
                /* We once reached here. Nothing to do anymore */
                mListener.onLocationSettingsChecked(false);
                return false;
            }

            DialogUtil dialogUtil = new DialogUtil(mContext);
            dialogUtil.showModalDialog(
                    mAppInfo.getApplicationName(),
                    "Please turn on device location.",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            launchSystemSettings();
                        }
                    });
            return false;
        }

        /* It seems all OK, so far. */
        return true;
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
            mActivityResultLauncher.launch(settingsIntent);
        } catch (ActivityNotFoundException e) {
            mListener.onError(TAG + ": ActivityResultLauncher.launch: " + e.getMessage());
        }
    }

    private void processActivityResult(@NonNull ActivityResult result) {
        /*
         * It seems the invoked sub-Activity (System Settings)
         * does not call Activity.setResult() before finish.
         * That is, resultCode is always RESULT_CANCELED.
         *
        if (result.getResultCode() == RESULT_OK) {
            locationStart();
        }
         */
        if (checkDeviceLocationSettings(false)) {
            checkAppRuntimePermissions();
        } else {
            Log.d(TAG, "Going to start dialog session");
        }
    }

    public void checkAppRuntimePermissions() {
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
            Log.d(TAG, "ACCESS_FINE_LOCATION(NG)");

            int mode = Settings.Secure.getInt(
                    mContext.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            Log.d(TAG, "Settings: Location: mode(" + mode + ")");
            if (mode != Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                Log.w(TAG, "Location mode requires higher precision");
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d(TAG, "Should Show Request Permission Rationale");

                DialogUtil dialogUtil = new DialogUtil(mContext);
                dialogUtil.showModalDialog(
                        mAppInfo.getApplicationName(),
                        "Please allow app-level permissions (Location).",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mListener.onRuntimePermissionRequired();
                            }
                        });
            } else {
                /* Explanation to the user is NOT necessary */
                Log.d(TAG, "Explanation to the user is NOT necessary");
                mListener.onRuntimePermissionRequired();
            }
        } else {
            Log.d(TAG, "ACCESS_FINE_LOCATION(OK)");
            mListener.onLocationSettingsChecked(true);
        }
    }

    public interface LocationPermissionsListener {
        /**
         * Called when user action is required to manually enable
         * application-level runtime permissions.
         */
        void onRuntimePermissionRequired();

        /**
         * Called when device check for location availability has finished.
         * @param isReady true if we can call {@link LocationTracker#bindLocationService}.
         */
        void onLocationSettingsChecked(boolean isReady);

        /**
         * Called on any error occasions.
         * @param description Error description message
         */
        void onError(@NonNull String description);
    }
}
