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

import android.telephony.SignalStrength;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import jp.ad.sinet.stream.android.helper.util.DateTimeUtil;

public class JsonBuilderForCellular {
    private final String TAG = JsonBuilderForCellular.class.getSimpleName();

    private final JsonBuilderForCellularListener mListener;
    private final DateTimeUtil mDateTimeUtil;
    private long mTimestamp;

    public JsonBuilderForCellular(
            @NonNull JsonBuilderForCellularListener listener) {
        mListener = listener;
        mDateTimeUtil = new DateTimeUtil();
    }

    public void build(int networkType, @NonNull SignalStrength ss, long timestamp) {
        mTimestamp = timestamp;
        CellularParser cellularParser = new CellularParser(mCellularParserListener);
        cellularParser.parse(networkType, ss);
    }

    private final CellularParser.CellularParserListener mCellularParserListener =
            new CellularParser.CellularParserListener() {
                @Override
                public void onGsmInfo(@Nullable Integer rssi,
                                      @Nullable Integer ber) {
                    buildGsmInfo(rssi, ber);
                }

                @Override
                public void onCdmaInfo(@Nullable Integer rssi,
                                       @Nullable Integer ecio) {
                    buildCdmaInfo(rssi, ecio);
                }

                @Override
                public void onEvdoInfo(@Nullable Integer rssi,
                                       @Nullable Integer ecio,
                                       @Nullable Integer snr) {
                    buildEvdoInfo(rssi, ecio, snr);
                }

                @Override
                public void onTdsCdmaInfo(@Nullable Integer rscp) {
                    buildTdsCdmaInfo(rscp);
                }

                @Override
                public void onWcdmaInfo(@Nullable Integer rscp,
                                        @Nullable Integer ecno) {
                    buildWcdmaInfo(rscp, ecno);
                }

                @Override
                public void onLteInfo(@Nullable Integer rssi,
                                      @Nullable Integer rsrp,
                                      @Nullable Integer rsrq,
                                      @Nullable Integer rssnr,
                                      @Nullable Integer cqi,
                                      @Nullable Integer cqiIndex,
                                      @Nullable Integer ta) {
                    buildLteInfo(rssi, rsrp, rsrq, rssnr, cqi, cqiIndex, ta);
                }

                @Override
                public void onNrInfo(@Nullable Integer ssRsrp,
                                     @Nullable Integer ssRsrq,
                                     @Nullable Integer ssSinr) {
                    buildNrInfo(ssRsrp, ssRsrq, ssSinr);
                }

                @Override
                public void onOthersInfo(@Nullable String data) {
                    buildOthersInfo(data);
                }

                @Override
                public void onError(@NonNull String description) {
                    mListener.onError(description);
                }
            };

    private void buildGsmInfo(@Nullable Integer rssi, @Nullable Integer ber) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("gsm", jsonObject);

            if (rssi != null) {
                jsonObject.put("rssi", rssi);
            }
            if (ber != null) {
                jsonObject.put("ber", ber);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildCdmaInfo(@Nullable Integer rssi, @Nullable Integer ecio) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("cdma", jsonObject);

            if (rssi != null) {
                jsonObject.put("rssi", rssi);
            }
            if (ecio != null) {
                jsonObject.put("ecio", ecio);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildEvdoInfo(
            @Nullable Integer rssi, @Nullable Integer ecio, @Nullable Integer snr) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("evdo", jsonObject);

            if (rssi != null) {
                jsonObject.put("rssi", rssi);
            }
            if (ecio != null) {
                jsonObject.put("ecio", ecio);
            }
            if (snr != null) {
                jsonObject.put("snr", snr);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildTdsCdmaInfo(@Nullable Integer rscp) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("td-scdma", jsonObject);

            if (rscp != null) {
                jsonObject.put("rscp", rscp);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildWcdmaInfo(@Nullable Integer rscp, @Nullable Integer ecno) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("w-cdma", jsonObject);

            if (rscp != null) {
                jsonObject.put("rscp", rscp);
            }
            if (ecno != null) {
                jsonObject.put("ecno", ecno);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildLteInfo(@Nullable Integer rssi,
                              @Nullable Integer rsrp,
                              @Nullable Integer rsrq,
                              @Nullable Integer rssnr,
                              @Nullable Integer cqi,
                              @Nullable Integer cqiIndex,
                              @Nullable Integer ta) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("lte", jsonObject);

            if (rssi != null) {
                jsonObject.put("rssi", rssi);
            }
            if (rsrp != null) {
                jsonObject.put("rsrp", rsrp);
            }
            if (rsrq != null) {
                jsonObject.put("rsrq", rsrq);
            }
            if (rssnr != null) {
                jsonObject.put("rssnr", rssnr);
            }
            if (cqi != null) {
                jsonObject.put("cqi", cqi);
            }
            if (cqiIndex != null) {
                jsonObject.put("cqiTableIndex", cqiIndex);
            }
            if (ta != null) {
                jsonObject.put("ta", ta);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildNrInfo(@Nullable Integer ssRsrp,
                             @Nullable Integer ssRsrq,
                             @Nullable Integer ssSinr) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("nr", jsonObject);

            if (ssRsrp != null) {
                jsonObject.put("ssRsrp", ssRsrp);
            }
            if (ssRsrq != null) {
                jsonObject.put("ssRsrq", ssRsrq);
            }
            if (ssSinr != null) {
                jsonObject.put("ssSinr", ssSinr);
            }
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    private void buildOthersInfo(@Nullable String data) {
        JSONObject rootObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            rootObject.put("others", jsonObject);
            jsonObject.put("timestamp", mDateTimeUtil.toIso8601String(mTimestamp));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put: " + e.getMessage());
            return;
        }
        mListener.onJsonObject(rootObject);
    }

    public interface JsonBuilderForCellularListener {
        void onJsonObject(@NonNull JSONObject jsonObject);
        void onError(@NonNull String description);
    }
}
