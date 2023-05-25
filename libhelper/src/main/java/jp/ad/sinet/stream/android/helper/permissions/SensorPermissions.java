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

import static android.content.Context.SENSOR_SERVICE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import jp.ad.sinet.stream.android.helper.util.AppInfo;
import jp.ad.sinet.stream.android.helper.util.DialogUtil;

public class SensorPermissions {
    private final String TAG = SensorPermissions.class.getSimpleName();

    private final AppCompatActivity mActivity;
    private final AppInfo mAppInfo;
    private final SensorPermissionsListener mListener;

    public SensorPermissions(@NonNull AppCompatActivity activity,
                             @NonNull SensorPermissionsListener listener) {
        this.mActivity = activity;
        this.mAppInfo = new AppInfo(activity);
        this.mListener = listener;
    }

    public ArrayList<Integer> getActivityRecognitionSensors() {
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(Sensor.TYPE_STEP_DETECTOR);
        arrayList.add(Sensor.TYPE_STEP_COUNTER);
        return arrayList;
    }

    public void run() {
        if (hasPermissionProtectedSensors()) {
            checkRuntimePermissions();
        } else {
            Log.d(TAG, "No permission protected sensors");
            mListener.onSensorSettingsChecked(true);
        }
    }

    private boolean hasPermissionProtectedSensors() {
        boolean needToCheckRuntimePermissions = false;

        SensorManager sensorManager =
                (SensorManager) mActivity.getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (int i = 0, n = sensorList.size(); i < n; i++) {
                Sensor sensor = sensorList.get(i);

                if (sensor.getType() >= Sensor.TYPE_DEVICE_PRIVATE_BASE) {
                    /* We don't know how to handle this sensor. */
                    continue;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    switch (sensor.getType()) {
                        case Sensor.TYPE_STEP_DETECTOR:
                        case Sensor.TYPE_STEP_COUNTER:
                            needToCheckRuntimePermissions = true;
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            mListener.onError("SensorManager is NOT available on this system");
            return false;
        }
        return needToCheckRuntimePermissions;
    }

    private void checkRuntimePermissions() {
        /*
         * To access some of Sensor types that collect physical activities,
         * required permissions (ACTIVITY_RECOGNITION) must have granted.
         */
        if (ActivityCompat.checkSelfPermission(mActivity,
                Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "ACTIVITY_RECOGNITION(NG)");

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity, Manifest.permission.ACTIVITY_RECOGNITION)) {
                Log.d(TAG, "Should Show Request Permission Rationale");

                DialogUtil dialogUtil = new DialogUtil(mActivity);
                dialogUtil.showModalDialog(
                        mAppInfo.getApplicationName(),
                        "Please allow app-level permissions (Activity Recognition).",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mListener.onRuntimePermissionRequired();
                            }
                        });
            } else {
                /* Explanation to the user is NOT necessary */
                mListener.onRuntimePermissionRequired();
            }
        } else {
            Log.d(TAG, "ACTIVITY_RECOGNITION(OK)");
            mListener.onSensorSettingsChecked(true);
        }
    }

    public interface SensorPermissionsListener {
        /**
         * Called when user action is required to manually enable
         * application-level runtime permissions.
         */
        void onRuntimePermissionRequired();

        /**
         * Called when device check for sensor availability has finished.
         * @param isReady true if user can use all available sensor types on this device.
         */
        void onSensorSettingsChecked(boolean isReady);

        /**
         * Called on any error occasions.
         * @param description Error description message
         */
        void onError(@NonNull String description);
    }
}
