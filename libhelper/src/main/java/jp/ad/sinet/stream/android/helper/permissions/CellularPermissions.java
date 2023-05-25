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
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import jp.ad.sinet.stream.android.helper.CellularMonitor;
import jp.ad.sinet.stream.android.helper.CellularMonitorListener;
import jp.ad.sinet.stream.android.helper.CellularService;
import jp.ad.sinet.stream.android.helper.util.AppInfo;
import jp.ad.sinet.stream.android.helper.util.DialogUtil;

public class CellularPermissions {
    private final String TAG = CellularPermissions.class.getSimpleName();

    private final Context mContext;
    private final Activity mActivity;
    private final AppInfo mAppInfo;
    private final CellularPermissionsListener mListener;

    private final ActivityResultLauncher<Intent> mActivityResultLauncher;

    public CellularPermissions(@NonNull AppCompatActivity activity,
                               @NonNull CellularPermissionsListener listener) {
        mContext = activity;
        mActivity = activity;
        mAppInfo = new AppInfo(mContext);
        mListener = listener;

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
     * Check system settings before starting the cellular service.
     * <p>
     *     This method triggers the series of checks to see if current settings
     *     are adequate for using the CellularService.
     *
     *     If everything goes fine, {@link CellularService}
     *     will be automatically started followed by a notification
     *     {@link CellularMonitorListener#onCellularSettingsChecked(boolean)}
     *     with {@code isReady} argument set to {@code true}.
     *     Otherwise, the same notification will be delivered with {@code isReady}
     *     argument set to {@code false}.
     * </p>
     */
    public void run() {
        if (checkDeviceCellularSettings(true)) {
            checkRuntimePermissions();
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
        if (checkDeviceCellularSettings(false)) {
            checkRuntimePermissions();
        } else {
            Log.d(TAG, "Going to start dialog session");
        }
    }

    private boolean checkDeviceCellularSettings(boolean promptIfDisabled) {
        Log.d(TAG, "checkDeviceCellularSettings: promptIfDisabled=" + promptIfDisabled);

        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            mListener.onError("TelephonyManager is NOT available on this system");
            return false;
        }

        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0) {
            Log.d(TAG, "Airplane mode is ON");

            if (! promptIfDisabled) {
                /* We once reached here. Nothing to do anymore */
                mListener.onCellularSettingsChecked(false);
                return false;
            }

            DialogUtil dialogUtil = new DialogUtil(mContext);
            dialogUtil.showModalDialog(
                    mAppInfo.getApplicationName(),
                    "Please turn off airplane mode.",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            launchSystemSettings();
                        }
                    });
            return false;
        } else {
            Log.d(TAG, "Airplane mode is OFF");
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
                new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        try {
            mActivityResultLauncher.launch(settingsIntent);
        } catch (ActivityNotFoundException e) {
            mListener.onError(TAG + "ActivityResultLauncher.launch: " + e.getMessage());
        }
    }

    private void checkRuntimePermissions() {
        /*
         * To access some of TelephonyManager information,
         * required permissions (READ_PHONE_STATE) must have granted.
         */
        if (ActivityCompat.checkSelfPermission(
                mContext, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_PHONE_STATE(NG)");

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity, Manifest.permission.READ_PHONE_STATE)) {
                Log.d(TAG, "Should Show Request Permission Rationale");

                DialogUtil dialogUtil = new DialogUtil(mContext);
                dialogUtil.showModalDialog(
                        mAppInfo.getApplicationName(),
                        "Please allow app-level permissions (READ_PHONE_STATE).",
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
            Log.d(TAG, "READ_PHONE_STATE(OK)");
            mListener.onCellularSettingsChecked(true);
        }
    }

    public interface CellularPermissionsListener {
        /**
         * Called when user action is required to manually enable
         * application-level runtime permissions.
         */
        void onRuntimePermissionRequired();

        /**
         * Called when device check for cellular network availability has finished.
         * @param isReady true if we can call {@link CellularMonitor#bindCellularService}.
         */
        void onCellularSettingsChecked(boolean isReady);

        /**
         * Called on any error occasions.
         * @param description Error description message
         */
        void onError(@NonNull String description);
    }
}
