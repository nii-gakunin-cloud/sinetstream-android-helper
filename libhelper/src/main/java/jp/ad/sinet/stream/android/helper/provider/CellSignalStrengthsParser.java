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
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

public class CellSignalStrengthsParser {
    private final String TAG = CellSignalStrengthsParser.class.getSimpleName();

    private final CellSignalStrengthsParserListener mListener;

    public CellSignalStrengthsParser(
            @NonNull CellSignalStrengthsParserListener listener) {
        this.mListener = listener;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void parse(int networkType, @NonNull SignalStrength ss) {
        List<CellSignalStrength> list = ss.getCellSignalStrengths();
        if (list.isEmpty()) {
            Log.w(TAG, "Empty CellSignalStrength list");
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            CellSignalStrength elem = list.get(i);
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GSM:
                    if (elem instanceof CellSignalStrengthGsm) {
                        CellSignalStrengthGsm cssGsm = (CellSignalStrengthGsm) elem;
                        parseCellSignalStrengthGsm(cssGsm);
                    }
                    break;
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    if (elem instanceof CellSignalStrengthTdscdma) {
                        CellSignalStrengthTdscdma cssTdsCdma = (CellSignalStrengthTdscdma) elem;
                        parseCellSignalStrengthTdsCdma(cssTdsCdma);
                        break;
                    }
                    if (elem instanceof CellSignalStrengthWcdma) {
                        CellSignalStrengthWcdma cssWcdma = (CellSignalStrengthWcdma) elem;
                        parseCellSignalStrengthWcdma(cssWcdma);
                        break;
                    }
                    if (elem instanceof CellSignalStrengthCdma) {
                        CellSignalStrengthCdma cssCdma = (CellSignalStrengthCdma) elem;
                        parseCellSignalStrengthCdma(cssCdma);
                        break;
                    }
                    parseCellSignalStrengthOthers("Unknown CDMA type", elem);
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE: /* 4G/LTE or 5G NSA */
                    if (elem instanceof CellSignalStrengthLte) {
                        CellSignalStrengthLte cssLte = (CellSignalStrengthLte) elem;
                        parseCellSignalStrengthLte(cssLte);
                    }
                    break;
                case TelephonyManager.NETWORK_TYPE_NR: /* 5G SA */
                    if (elem instanceof CellSignalStrengthLte) {
                        CellSignalStrengthLte cssLte = (CellSignalStrengthLte) elem;
                        parseCellSignalStrengthLte(cssLte);
                    } else
                    if (elem instanceof CellSignalStrengthNr) {
                        CellSignalStrengthNr cssNr = (CellSignalStrengthNr) elem;
                        parseCellSignalStrengthNr(cssNr);
                    }
                    break;
                default:
                    String label = "Unknown network type (" + networkType + ")";
                    parseCellSignalStrengthOthers(label, elem);
                    break;
            }

            /*
             * TODO check if this SignalStrength is for the serving cell
             * For now, take the 1st element only...
             */
            break;
        }
    }

    private void parseCellSignalStrengthGsm(
            @NonNull CellSignalStrengthGsm cssGsm) {
        Log.d(TAG, "GSM: " + cssGsm);
        int ival;
        Integer rssi = null;
        Integer ber = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ival = cssGsm.getRssi();
        } else {
            ival = cssGsm.getDbm();
        }
        if (ival != CellInfo.UNAVAILABLE) {
            rssi = ival;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ival = cssGsm.getBitErrorRate();
        }
        if (ival != CellInfo.UNAVAILABLE) {
            ber = ival;
        }

        mListener.onGsmInfo(rssi, ber);
    }

    private void parseCellSignalStrengthCdma(
            @NonNull CellSignalStrengthCdma cssCdma) {
        Log.d(TAG, "CDMA: " + cssCdma);
        int ival;
        Integer rssi = null;
        Integer ecio = null;
        Integer snr = null;

        int cdmaLevel = cssCdma.getCdmaLevel();
        int evdoLevel = cssCdma.getEvdoLevel();
        boolean isEvdo;

        if (evdoLevel == CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            /* We don't know evdo, use cdma */
            isEvdo = false;
        } else if (cdmaLevel == CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN) {
            /* We don't know cdma, use evdo */
            isEvdo = true;
        } else {
            /* We know both, use the lowest level */
            if (cdmaLevel < evdoLevel) {
                /* CDMA */
                isEvdo = false;
            } else {
                /* EVDO */
                isEvdo = true;
            }
        }

        if (isEvdo) {
            ival = cssCdma.getEvdoDbm();
            if (ival != CellInfo.UNAVAILABLE) {
                rssi = ival;
            }
            ival = cssCdma.getEvdoEcio();
            if (ival != CellInfo.UNAVAILABLE) {
                ecio = ival;
            }
            ival = cssCdma.getEvdoSnr();
            if (ival != CellInfo.UNAVAILABLE) {
                snr = ival;
            }
            mListener.onEvdoInfo(rssi, ecio, snr);
        } else {
            ival = cssCdma.getCdmaDbm();
            if (ival != CellInfo.UNAVAILABLE) {
                rssi = ival;
            }

            ival = cssCdma.getCdmaEcio();
            if (ival != CellInfo.UNAVAILABLE) {
                ecio = ival;
            }
            mListener.onCdmaInfo(rssi, ecio);
        }
    }

    private void parseCellSignalStrengthTdsCdma(
            @NonNull CellSignalStrengthTdscdma cssTdsCdma) {
        Log.d(TAG, "TD-SCDMA: " + cssTdsCdma);
        int ival;
        Integer rscp = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ival = cssTdsCdma.getRscp();
        } else {
            ival = cssTdsCdma.getDbm();
        }
        if (ival != CellInfo.UNAVAILABLE) {
            rscp = ival;
        }

        mListener.onTdsCdmaInfo(rscp);
    }

    private void parseCellSignalStrengthWcdma(
            @NonNull CellSignalStrengthWcdma cssWcdma) {
        Log.d(TAG, "W-CDMA: " + cssWcdma);
        int ival;
        Integer rscp = null;
        Integer ecno = null;

        ival = cssWcdma.getDbm();
        if (ival != CellInfo.UNAVAILABLE) {
            rscp = ival;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ival = cssWcdma.getEcNo();
            if (ival != CellInfo.UNAVAILABLE) {
                ecno = ival;
            }
        }

        mListener.onWcdmaInfo(rscp, ecno);
    }

    private void parseCellSignalStrengthLte(
            @NonNull CellSignalStrengthLte cssLte) {
        Log.d(TAG, "LTE: " + cssLte);
        int ival;

        Integer rssi = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ival = cssLte.getRssi();
            if (ival != CellInfo.UNAVAILABLE) {
                rssi = ival;
            }
        }

        Integer rsrp = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ival = cssLte.getRsrp();
        } else {
            ival = cssLte.getDbm();
        }
        if (ival != CellInfo.UNAVAILABLE) {
            rsrp = ival;
        }

        Integer rsrq = null;
        Integer rssnr = null;
        Integer cqi = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ival = cssLte.getRsrq();
            if (ival != CellInfo.UNAVAILABLE) {
                rsrq = ival;
            }

            ival = cssLte.getRssnr();
            if (ival != CellInfo.UNAVAILABLE) {
                rssnr = ival;
            }

            ival = cssLte.getCqi();
            if (ival != CellInfo.UNAVAILABLE) {
                cqi = ival;
            }
        }

        Integer cqiIndex = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ival = cssLte.getCqiTableIndex();
            if (ival != CellInfo.UNAVAILABLE) {
                cqiIndex = ival;
            }
        }

        Integer ta = null;
        ival = cssLte.getTimingAdvance();
        if (ival != CellInfo.UNAVAILABLE) {
            ta = ival;
        }

        mListener.onLteInfo(rssi, rsrp, rsrq, rssnr, cqi, cqiIndex, ta);
    }

    private void parseCellSignalStrengthNr(
            @NonNull CellSignalStrengthNr cssNr) {
        Log.d(TAG, "NR: " + cssNr);
        int ival;

        Integer ssRsrp = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ival = cssNr.getSsRsrp();
        } else {
            ival = cssNr.getDbm();
        }
        if (ival != CellInfo.UNAVAILABLE) {
            ssRsrp = ival;
        }

        Integer ssRsrq = null;
        Integer ssSinr = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ival = cssNr.getSsRsrq();
            if (ival != CellInfo.UNAVAILABLE) {
                ssRsrq = ival;
            }
            ival = cssNr.getSsSinr();
            if (ival != CellInfo.UNAVAILABLE) {
                ssSinr = ival;
            }
        }

        mListener.onNrInfo(ssRsrp, ssRsrq, ssSinr);
    }

    private void parseCellSignalStrengthOthers(
            @NonNull String label,
            @NonNull CellSignalStrength css) {
        Log.d(TAG, label + ": " + css);
        mListener.onOthersInfo(null);
    }

    public interface CellSignalStrengthsParserListener {
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
