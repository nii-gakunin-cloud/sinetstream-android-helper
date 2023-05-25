/*
 * Copyright (c) 2022 National Institute of Informatics
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
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Map;

import jp.ad.sinet.stream.android.helper.constants.LocationProviderType;
import jp.ad.sinet.stream.android.helper.constants.PermissionTypes;
import jp.ad.sinet.stream.android.helper.permissions.CellularPermissions;
import jp.ad.sinet.stream.android.helper.permissions.LocationPermissions;
import jp.ad.sinet.stream.android.helper.permissions.SensorPermissions;

public class PermissionHandler {
    private final String TAG = PermissionHandler.class.getSimpleName();

    private final AppCompatActivity mActivity;
    private final PermissionHandlerListener mListener;

    private final ActivityResultLauncher<String[]> mRequestPermissionLauncher;

    private SensorPermissions mSensorPermissions = null;
    private CellularPermissions mCellularPermissions = null;
    private LocationPermissions mLocationPermissions = null;

    private final ArrayList<String> mRuntimePermissionTypes = new ArrayList<>();
    private int mTargetPermissionTypes = 0;
    private int mPermissionGrantedTypes = 0;
    private int mPermissionDeniedTypes = 0;

    public PermissionHandler(@NonNull AppCompatActivity activity,
                             @NonNull PermissionHandlerListener listener) {
        this.mActivity = activity;
        this.mListener = listener;

        /*
         * https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
         */
        mRequestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> map) {
                        Log.d(TAG, "RequestPermissionLauncher.onActivityResult: map=" + map);
                        processPermissionResults(map);
                    }
                }
        );
    }

    /**
     * Setup required permissions to read sensor data.
     * <p>
     *     Some sensor types are protected by runtime permissions.
     *     Call this method to register permission check handler for sensor types.
     * </p>
     * <p>
     *     [NB] This method should be called before run().
     * </p>
     */
    public void checkSensorPermissions() {
        mSensorPermissions =
                new SensorPermissions(mActivity,
                        new SensorPermissions.SensorPermissionsListener() {
                            @Override
                            public void onRuntimePermissionRequired() {
                                Log.d(TAG, "onRuntimePermissionRequired: Sensors");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    mRuntimePermissionTypes.add(
                                            Manifest.permission.ACTIVITY_RECOGNITION);
                                    deviceSettingsChecked(PermissionTypes.ACTIVITY_RECOGNITION);
                                } else {
                                    Log.d(TAG, "Android OS " +
                                            "(" + Build.VERSION.SDK_INT + "): " +
                                            "not applicable on this device");
                                }
                            }

                            @Override
                            public void onSensorSettingsChecked(boolean isReady) {
                                Log.d(TAG, "onSensorSettingsChecked: isReady=" + isReady);
                                if (isReady) {
                                    mPermissionGrantedTypes |= PermissionTypes.ACTIVITY_RECOGNITION;
                                } else {
                                    mPermissionDeniedTypes |= PermissionTypes.ACTIVITY_RECOGNITION;
                                }
                                deviceSettingsChecked(PermissionTypes.ACTIVITY_RECOGNITION);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                mListener.onError(description);
                            }
                        });

        mTargetPermissionTypes |= PermissionTypes.ACTIVITY_RECOGNITION;
    }

    /**
     * Setup required permissions to read cellular data.
     * <p>
     *     To read cellular data, adding to the network connectivity,
     *     application-level runtime permissions must be cleared.
     *     Call this method to register permission check handler for cellular data.
     * </p>
     * <p>
     *     [NB] This method should be called before run().
     * </p>
     */
    public void checkCellularPermissions() {
        mCellularPermissions =
                new CellularPermissions(mActivity,
                        new CellularPermissions.CellularPermissionsListener() {
                            @Override
                            public void onRuntimePermissionRequired() {
                                Log.d(TAG, "onRuntimePermissionRequired: Cellular");
                                mRuntimePermissionTypes.add(
                                        Manifest.permission.READ_PHONE_STATE);
                                deviceSettingsChecked(PermissionTypes.READ_PHONE_STATE);
                            }

                            @Override
                            public void onCellularSettingsChecked(boolean isReady) {
                                Log.d(TAG, "onCellularSettingsChecked: isReady=" + isReady);
                                if (isReady) {
                                    mPermissionGrantedTypes |= PermissionTypes.READ_PHONE_STATE;
                                } else {
                                    mPermissionDeniedTypes |= PermissionTypes.READ_PHONE_STATE;
                                }
                                deviceSettingsChecked(PermissionTypes.READ_PHONE_STATE);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                mListener.onError(description);
                            }
                        });

        mTargetPermissionTypes |= PermissionTypes.READ_PHONE_STATE;
    }

    /**
     * Setup required permissions to read device location data.
     * <p>
     *     To read device location data, complex combinations of system-level
     *     permissions, as well as application-level runtime permissions
     *     must be cleared.
     *     Call this method to register permission check handler for cellular data.
     * </p>
     * <p>
     *     [NB] This method should be called before run().
     * </p>
     */
    public void checkLocationPermissions(@NonNull String providerName) {
        LocationProviderType locationProviderType;

        if (providerName.equals(LocationManager.GPS_PROVIDER)) {
            locationProviderType = LocationProviderType.GPS;
        } else if (providerName.equals(LocationManager.FUSED_PROVIDER)) {
            locationProviderType = LocationProviderType.FUSED;
        } else {
            mListener.onError(
                    TAG + "Unknown LocationProvider(" + providerName + ")");
            return;
        }

        mLocationPermissions =
                new LocationPermissions(mActivity,
                        locationProviderType,
                        new LocationPermissions.LocationPermissionsListener() {
                            @Override
                            public void onRuntimePermissionRequired() {
                                Log.d(TAG, "onRuntimePermissionRequired: Location");
                                mRuntimePermissionTypes.add(
                                        Manifest.permission.ACCESS_FINE_LOCATION);
                                deviceSettingsChecked(PermissionTypes.LOCATION);
                            }

                            @Override
                            public void onLocationSettingsChecked(boolean isReady) {
                                Log.d(TAG, "onLocationSettingsChecked: isReady=" + isReady);
                                if (isReady) {
                                    mPermissionGrantedTypes |= PermissionTypes.LOCATION;
                                } else {
                                    mPermissionDeniedTypes |= PermissionTypes.LOCATION;
                                }
                                deviceSettingsChecked(PermissionTypes.LOCATION);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                mListener.onError(description);
                            }
                        });

        mTargetPermissionTypes |= PermissionTypes.LOCATION;
    }

    /**
     * Run sequence of permission checks.
     */
    public void run() {
        if (mTargetPermissionTypes != 0) {
            Log.d(TAG, "Going to check device permissions");
            if (mSensorPermissions != null) {
                mSensorPermissions.run();
            }
            if (mCellularPermissions != null) {
                mCellularPermissions.run();
            }
            if (mLocationPermissions != null) {
                mLocationPermissions.run();
            }
        } else {
            Log.d(TAG, "No permission check required");
            mListener.onPermissionChecked(0, 0);
        }
    }

    private void deviceSettingsChecked(int type) {
        mTargetPermissionTypes &= ~type;
        if (mTargetPermissionTypes != 0) {
            Log.d(TAG, "Wait remaining device permissions checks");
        } else {
            Log.d(TAG, "Device permissions OK. Check app-level permissions next");
            requestRuntimePermissions();
        }
    }

    private void requestRuntimePermissions() {
        if (mRuntimePermissionTypes.size() > 0) {
            Log.d(TAG, "Going to request runtime permissions");
            try {
                /* We can request multiple permissions all at once */
                String[] input = mRuntimePermissionTypes.toArray(new String[0]);
                mRequestPermissionLauncher.launch(input);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "ActivityResultLauncher: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "No runtime permissions required");
            mListener.onPermissionChecked(mPermissionGrantedTypes, mPermissionDeniedTypes);
        }
    }

    private void processPermissionResults(@NonNull Map<String, Boolean> map) {
        for (int i = 0, n = mRuntimePermissionTypes.size(); i < n; i++) {
            String type = mRuntimePermissionTypes.get(i);
            Boolean isGranted = map.get(type);
            if (isGranted == null) {
                Log.w(TAG, "onRequestPermissionResult(" + type + "): Not found?");
                continue;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (type.equals(Manifest.permission.ACTIVITY_RECOGNITION)) {
                    if (isGranted) {
                        Log.d(TAG, type + ": GRANTED");
                        mPermissionGrantedTypes |= PermissionTypes.ACTIVITY_RECOGNITION;
                    } else {
                        Log.d(TAG, type + ": DENIED");
                        mPermissionDeniedTypes |= PermissionTypes.ACTIVITY_RECOGNITION;
                    }
                    continue;
                }
            }

            if (type.equals(Manifest.permission.READ_PHONE_STATE)) {
                if (isGranted) {
                    Log.d(TAG, type + ": GRANTED");
                    mPermissionGrantedTypes |= PermissionTypes.READ_PHONE_STATE;
                } else {
                    Log.d(TAG, type + ": DENIED");
                    mPermissionDeniedTypes |= PermissionTypes.READ_PHONE_STATE;
                }
                continue;
            }

            if (type.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (isGranted) {
                    Log.d(TAG, type + ": GRANTED");
                    mPermissionGrantedTypes |= PermissionTypes.LOCATION;
                } else {
                    Log.d(TAG, type + ": DENIED");
                    mPermissionDeniedTypes |= PermissionTypes.LOCATION;
                }
            }
        }
        mListener.onPermissionChecked(mPermissionGrantedTypes, mPermissionDeniedTypes);
    }

    /**
     * Return sensor types those which runtime permission have denied.
     *
     * @return the array of 'Sensor.TYPE_XXX' values, which can be empty.
     */
    @NonNull
    public ArrayList<Integer> getDeniedSensorTypes() {
        return mSensorPermissions.getActivityRecognitionSensors();
    }

    public interface PermissionHandlerListener {
        /**
         * Called on all permission check has finished.
         *
         * @param grantedTypes the bitmap of granted {@link PermissionTypes}.
         * @param deniedTypes the bitmap of denied {@link PermissionTypes}.
         */
        void onPermissionChecked(int grantedTypes, int deniedTypes);

        /**
         * Called on any error occasions.
         * @param description Error description message
         */
        void onError(@NonNull String description);
    }
}
