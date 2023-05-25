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

import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jp.ad.sinet.stream.android.helper.constants.NetworkTypes;

public class SignalStrengthParser {
    private final String TAG = SignalStrengthParser.class.getSimpleName();

    private final SignalStrengthParserListener mListener;

    public SignalStrengthParser(@NonNull SignalStrengthParserListener listener) {
        this.mListener = listener;
    }

    public void parse(int networkType, @NonNull SignalStrength ss) {
        String s = "";

        s += "NetworkType: " + networkType +
                "(" + NetworkTypes.toName(networkType) + ")";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            s += ",";
            s += "Antenna Level(" + ss.getLevel() + ")";
        }
        Log.d(TAG, s);

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GSM:
                getGsmInfo(ss);
                break;
            case TelephonyManager.NETWORK_TYPE_CDMA:
                getCdmaInfo(ss);
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                getEvdoInfo(ss);
                break;
            case TelephonyManager.NETWORK_TYPE_LTE: /* 4G/LTA or 5G NSA */
                getLteInfoByReflection(ss);
                break;
            case TelephonyManager.NETWORK_TYPE_NR: /* 5G SA */
            default:
                Log.d(TAG, "Current network type: " + networkType);
                getOthersInfo(ss);
                break;
        }
    }

    private void getGsmInfo(@NonNull SignalStrength ss) {
        int ival;
        Integer rssi = null;
        Integer ber = null;

        ival = ss.getGsmSignalStrength();
        if (ival != CellInfo.UNAVAILABLE) {
            rssi = ival;
        }
        ival = ss.getGsmBitErrorRate();
        if (ival != CellInfo.UNAVAILABLE) {
            ber = ival;
        }
        mListener.onGsmInfo(rssi, ber);
    }

    private void getCdmaInfo(@NonNull SignalStrength ss) {
        int ival;
        Integer rssi = null;
        Integer ecio = null;

        ival = ss.getCdmaDbm();
        if (ival != SignalStrength.INVALID) {
            rssi = ival;
        }
        ival = ss.getCdmaEcio();
        if (ival != SignalStrength.INVALID) {
            ecio = ival;
        }

        mListener.onCdmaInfo(rssi, ecio);
    }

    private void getEvdoInfo(@NonNull SignalStrength ss) {
        int ival;
        Integer rssi = null;
        Integer ecio = null;
        Integer snr = null;

        ival = ss.getEvdoDbm();
        if (ival != SignalStrength.INVALID) {
            rssi = ival;
        }

        ival = ss.getEvdoEcio();
        if (ival != SignalStrength.INVALID) {
            ecio = ival;
        }

        ival = ss.getEvdoSnr();
        if (ival != SignalStrength.INVALID) {
            snr = ival;
        }

        mListener.onEvdoInfo(rssi, ecio, snr);
    }

    private void getLteInfoByReflection(@NonNull SignalStrength ss) {
        Method[] methods = SignalStrength.class.getMethods();
        Integer rssi = null;
        Integer rsrp = null;
        Integer rsrq = null;
        Integer rssnr = null;
        Integer cqi = null;
        Integer cqiIndex = null;
        Integer ta = null;

        try {
            for (Method method : methods) {
                /*
                if (method.getName().equals("getLteAsuLevel")) {
                    asu = (Integer) method.invoke(ss);
                }
                 */
                if (method.getName().equals("getLteCqi")) {
                    cqi = (Integer) method.invoke(ss);
                }
                /*
                if (method.getName().equals("getLteDbm")) {
                    label = "LTE dbm: ";
                }
                if (method.getName().equals("getLteLevel")) {
                    label = "LTE Level: ";
                }
                 */
                if (method.getName().equals("getLteRsrp")) {
                    rsrp = (Integer) method.invoke(ss);
                }
                if (method.getName().equals("getLteRsrq")) {
                    rsrq = (Integer) method.invoke(ss);
                }
                if (method.getName().equals("getLteRssnr")) {
                    rssnr = (Integer) method.invoke(ss);
                }
                if (method.getName().equals("getLteSignalStrength")) {
                    rssi = (Integer) method.invoke(ss);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            mListener.onError(TAG + ": Method.invoke: " + e.getMessage());
            return;
        }

        mListener.onLteInfo(rssi, rsrp, rsrq, rssnr, cqi, cqiIndex, ta);
    }

    private void getOthersInfo(@NonNull SignalStrength ss) {
        mListener.onOthersInfo(null);
    }

    public interface SignalStrengthParserListener {
        void onGsmInfo(@Nullable Integer rssi,
                       @Nullable Integer ber);

        void onCdmaInfo(@Nullable Integer rssi,
                        @Nullable Integer ecio);

        void onEvdoInfo(@Nullable Integer rssi,
                        @Nullable Integer ecio,
                        @Nullable Integer snr);

        void onLteInfo(@Nullable Integer rssi,
                       @Nullable Integer rsrp,
                       @Nullable Integer rsrq,
                       @Nullable Integer rssnr,
                       @Nullable Integer cqi,
                       @Nullable Integer cqiIndex,
                       @Nullable Integer ta);

        void onNrInfo(@Nullable Integer ssRsrp,
                      @Nullable Integer ssRsrq,
                      @Nullable Integer ssSinr);

        void onOthersInfo(@Nullable String data);

        void onError(@NonNull String description);
    }
}
