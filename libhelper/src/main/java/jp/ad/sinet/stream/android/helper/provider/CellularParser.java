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
import android.telephony.SignalStrength;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class CellularParser {
    private final String TAG = CellularParser.class.getSimpleName();

    private final CellularParserListener mListener;

    public CellularParser(@NonNull CellularParserListener listener) {
        this.mListener = listener;
    }

    public void parse(int networkType, @NonNull SignalStrength ss) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            parseCellSignalStrengths(networkType, ss);
        } else {
            parseSignalStrength(networkType, ss);
        }
    }

    private void parseSignalStrength(
            int networkType, @NonNull SignalStrength ss) {
        SignalStrengthParser parser = new SignalStrengthParser(
                new SignalStrengthParser.SignalStrengthParserListener() {
                    @Override
                    public void onGsmInfo(@Nullable Integer rssi,
                                          @Nullable Integer ber) {
                        mListener.onGsmInfo(rssi, ber);
                    }

                    @Override
                    public void onCdmaInfo(@Nullable Integer rssi,
                                           @Nullable Integer ecio) {
                        mListener.onCdmaInfo(rssi, ecio);
                    }

                    @Override
                    public void onEvdoInfo(@Nullable Integer rssi,
                                           @Nullable Integer ecio,
                                           @Nullable Integer snr) {
                        mListener.onEvdoInfo(rssi, ecio, snr);
                    }

                    @Override
                    public void onLteInfo(@Nullable Integer rssi,
                                          @Nullable Integer rsrp,
                                          @Nullable Integer rsrq,
                                          @Nullable Integer rssnr,
                                          @Nullable Integer cqi,
                                          @Nullable Integer cqiIndex,
                                          @Nullable Integer ta) {
                        mListener.onLteInfo(
                                rssi, rsrp, rsrq, rssnr, cqi, cqiIndex, ta);
                    }

                    @Override
                    public void onNrInfo(@Nullable Integer ssRsrp,
                                         @Nullable Integer ssRsrq,
                                         @Nullable Integer ssSinr) {
                        mListener.onNrInfo(ssRsrp, ssRsrq, ssSinr);
                    }

                    @Override
                    public void onOthersInfo(@Nullable String data) {
                        mListener.onOthersInfo(data);
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        mListener.onError(description);
                    }
                });

        parser.parse(networkType, ss);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void parseCellSignalStrengths(
            int networkType, @NonNull SignalStrength ss) {
        CellSignalStrengthsParser parser = new CellSignalStrengthsParser(
                new CellSignalStrengthsParser.CellSignalStrengthsParserListener() {
                    @Override
                    public void onGsmInfo(@Nullable Integer rssi,
                                          @Nullable Integer ber) {
                        mListener.onGsmInfo(rssi, ber);
                    }

                    @Override
                    public void onCdmaInfo(@Nullable Integer rssi,
                                           @Nullable Integer ecio) {
                        mListener.onCdmaInfo(rssi, ecio);
                    }

                    @Override
                    public void onEvdoInfo(@Nullable Integer rssi,
                                           @Nullable Integer ecio,
                                           @Nullable Integer snr) {
                        mListener.onEvdoInfo(rssi, ecio, snr);
                    }

                    @Override
                    public void onTdsCdmaInfo(@Nullable Integer rscp) {
                        mListener.onTdsCdmaInfo(rscp);
                    }

                    @Override
                    public void onWcdmaInfo(@Nullable Integer rscp,
                                            @Nullable Integer ecno) {
                        mListener.onWcdmaInfo(rscp, ecno);
                    }

                    @Override
                    public void onLteInfo(@Nullable Integer rssi,
                                          @Nullable Integer rsrp,
                                          @Nullable Integer rsrq,
                                          @Nullable Integer rssnr,
                                          @Nullable Integer cqi,
                                          @Nullable Integer cqiIndex,
                                          @Nullable Integer ta) {
                        mListener.onLteInfo(rssi, rsrp, rsrq, rssnr, cqi, cqiIndex, ta);
                    }

                    @Override
                    public void onNrInfo(@Nullable Integer ssRsrp,
                                         @Nullable Integer ssRsrq,
                                         @Nullable Integer ssSinr) {
                        mListener.onNrInfo(ssRsrp, ssRsrq, ssSinr);
                    }

                    @Override
                    public void onOthersInfo(@Nullable String data) {
                        mListener.onOthersInfo(data);
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        mListener.onError(description);
                    }
                }
        );

        parser.parse(networkType, ss);
    }

    public interface CellularParserListener {
        void onGsmInfo(@Nullable Integer rssi,
                       @Nullable Integer ber);

        void onCdmaInfo(@Nullable Integer rssi,
                        @Nullable Integer ecio);

        void onEvdoInfo(@Nullable Integer rssi,
                        @Nullable Integer ecio,
                        @Nullable Integer snr);

        void onTdsCdmaInfo(@Nullable Integer rscp);

        void onWcdmaInfo(@Nullable Integer rscp,
                         @Nullable Integer ecno);

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
