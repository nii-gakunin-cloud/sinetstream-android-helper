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

package jp.ad.sinet.stream.android.helper.provider;

import android.os.Bundle;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;

public class CellularStorage {
    private final String TAG = CellularStorage.class.getSimpleName();
    private int mNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private SignalStrength mSignalStrength = null;
    private long mTimestamp = 0;

    public void setCellularStorage(@NonNull Bundle bundle) {
        int networkType = bundle.getInt(
                BundleKeys.BUNDLE_KEY_CELLULAR_NETWORK_TYPE, -1);
        SignalStrength ss = bundle.getParcelable(
                BundleKeys.BUNDLE_KEY_CELLULAR_PARCELABLE);

        mNetworkType = networkType;
        mSignalStrength = ss;
        mTimestamp = System.currentTimeMillis();
    }

    public void clearCellularStorage() {
        mNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        mSignalStrength = null;
        mTimestamp = System.currentTimeMillis();
    }

    public int getNetworkType() {
        return mNetworkType;
    }

    @Nullable
    public SignalStrength getSignalStrength() {
        return mSignalStrength;
    }

    public long getTimestamp() {
        return mTimestamp;
    }
}
