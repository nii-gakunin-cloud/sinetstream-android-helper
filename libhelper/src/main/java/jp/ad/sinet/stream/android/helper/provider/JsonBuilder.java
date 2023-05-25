/*
 * Copyright (C) 2020-2021 National Institute of Informatics
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

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import jp.ad.sinet.stream.android.helper.constants.JsonTags;
import jp.ad.sinet.stream.android.helper.constants.SensorTypes;
import jp.ad.sinet.stream.android.helper.models.SensorHolder;
import jp.ad.sinet.stream.android.helper.util.DateTimeUtil;

public class JsonBuilder {
    private final static String TAG = JsonBuilder.class.getSimpleName();

    private final String mPublisher;
    private final String mUserNote;
    private final double mLatitude;
    private final double mLongitude;
    private final long mUtcTime;

    private SensorTypes mSensorTypes = null;
    private final DateTimeUtil mDateTimeUtil = new DateTimeUtil();
    private JSONObject mExtraCellularData = null;
    private boolean mEnablePrettyPrint = false;

    public JsonBuilder(
            @Nullable String publisher, @Nullable String note,
            double latitude, double longitude, long utcTime) {
        this.mPublisher = publisher;
        this.mUserNote = note;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mUtcTime = utcTime;
    }

    public void addExtraCellularData(@NonNull JSONObject jsonObject) {
        mExtraCellularData = jsonObject;
    }

    public void switchPrettyPrint(boolean isEnabled) {
        mEnablePrettyPrint = isEnabled;
    }

    @Nullable
    public String buildJsonString(ArrayList<SensorHolder> sensorHolders) {
        String jsonString = null;
        JSONObject rootObject = new JSONObject();

        if (! setDevice(rootObject)
                || ! setSensorHolders(rootObject, sensorHolders)) {
            Log.w(TAG, "buildJsonString FAILED");
            rootObject = null;
        }
        if (rootObject != null) {
            if (mEnablePrettyPrint) {
                try {
                    jsonString = rootObject.toString(4);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONObject.toString: " + e);
                }
            } else {
                jsonString = rootObject.toString();
            }
        }
        /* DEBUG
        if (jsonString != null) {
            Log.d(TAG, "JSON=" + jsonString);
        }
         */
        return jsonString;
    }

    private boolean setDevice(JSONObject parentObject) {
        JSONObject jsonObject = new JSONObject();
        try {
            parentObject.put(JsonTags.JSON_TAGS_DEVICE.getName(), jsonObject);
            if (! setSysInfo(jsonObject)
                    || ! setUserInfo(jsonObject)
                    || ! setLocation(jsonObject)
                    || ! setCellularInfo(jsonObject)) {
                jsonObject = null;
            }
        } catch (JSONException e) {
            Log.e(TAG, "setDevice: JSONObject: " + e);
            jsonObject = null;
        }
        return (jsonObject != null);
    }

    private boolean setSysInfo(JSONObject parentObject) {
        JSONObject jsonObject = new JSONObject();
        try {
            parentObject.put(JsonTags.JSON_TAGS_SYSINFO.getName(), jsonObject);
            jsonObject.put("android", Build.VERSION.RELEASE);
            // jsonObject.put("board", Build.BOARD);
            // jsonObject.put("brand", Build.BRAND);
            // jsonObject.put("device", Build.DEVICE);
            // jsonObject.put("hardware", Build.HARDWARE);
            // jsonObject.put("host", Build.HOST);
            jsonObject.put("manufacturer", Build.MANUFACTURER);
            jsonObject.put("model", Build.MODEL);
            // jsonObject.put("product", Build.PRODUCT);
            // jsonObject.put("tags", Build.TAGS);
            // jsonObject.put("type", Build.TYPE);
        } catch (JSONException e) {
            Log.e(TAG, "setSysInfo: JSONObject.put: " + e);
            return false;
        }
        return true;
    }

    private boolean setUserInfo(JSONObject parentObject) {
        JSONObject jsonObject = new JSONObject();
        try {
            parentObject.put(JsonTags.JSON_TAGS_USERINFO.getName(), jsonObject);
            if (this.mPublisher != null) {
                jsonObject.put(JsonTags.JSON_TAGS_USERINFO_PUBLISHER.getName(), this.mPublisher);
            }
            if (this.mUserNote != null) {
                jsonObject.put(JsonTags.JSON_TAGS_USERINFO_NOTE.getName(), this.mUserNote);
            }
        } catch (JSONException e) {
            Log.e(TAG, "setUserInfo: JSONObject.put: " + e);
            return false;
        }
        return true;
    }

    private boolean setCellularInfo(JSONObject parentObject) {
        if (mExtraCellularData != null) {
            try {
                parentObject.put(JsonTags.JSON_TAGS_CELLULAR.getName(), mExtraCellularData);
            } catch (JSONException e) {
                Log.e(TAG, "setCellular: JSONObject.put: " + e);
                return false;
            }
        }
        return true;
    }

    private boolean setLocation(JSONObject parentObject) {
        JSONObject jsonObject = new JSONObject();
        try {
            parentObject.put(JsonTags.JSON_TAGS_LOCATION.getName(), jsonObject);
            if (! Double.isNaN(this.mLatitude) && ! Double.isNaN(this.mLongitude)) {
                jsonObject.put(JsonTags.JSON_TAGS_LOCATION_LATITUDE.getName(),
                        String.format(Locale.ENGLISH, "%.6f", this.mLatitude));
                jsonObject.put(JsonTags.JSON_TAGS_LOCATION_LONGITUDE.getName(),
                        String.format(Locale.ENGLISH, "%.6f", this.mLongitude));

                if (this.mUtcTime >= 0) {
                    String dateStr = mDateTimeUtil.toIso8601String(this.mUtcTime);
                    jsonObject.put(JsonTags.JSON_TAGS_SENSOR_TIMESTAMP.getName(), dateStr);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "setLocation: JSONObject.put: " + e);
            jsonObject = null;
        }
        return (jsonObject != null);
    }

    private boolean setSensorHolders(JSONObject parentObject, ArrayList<SensorHolder> sensorHolders) {
        JSONArray jsonArrayObject = new JSONArray();

        for (int i = 0, n = sensorHolders.size(); i < n; i++) {
            SensorHolder sensorHolder = sensorHolders.get(i);
            if (! setSensorHolder(jsonArrayObject, i, sensorHolder)) {
                jsonArrayObject = null;
                break;
            }
        }
        try {
            if (jsonArrayObject != null) {
                // Log.d(TAG, "SENSORS=" + jsonArrayObject.toString(4));
                parentObject.put(JsonTags.JSON_TAGS_SENSORS.getName(), jsonArrayObject);
            }
        } catch (JSONException e) {
            Log.e(TAG, "setSensorHolders: JSONObject.put: " + e);
            jsonArrayObject = null;
        }
        return (jsonArrayObject != null);
    }

    private boolean setSensorHolder(JSONArray parentArray, int idx, SensorHolder sensorHolder) {
        JSONObject jsonObject = new JSONObject();
        if (! setSensorType(jsonObject, sensorHolder)
                || ! setSensorName(jsonObject, sensorHolder)
                || ! setSensorId(jsonObject, sensorHolder)
                || ! setTimeStamp(jsonObject, sensorHolder)
                || ! setSensorValues(jsonObject, sensorHolder)) {
            jsonObject = null;
        }
        if (jsonObject != null) {
            try {
                parentArray.put(idx, jsonObject);
            } catch (JSONException e) {
                Log.e(TAG, "setSensorHolder: JSONArray.put: " + e);
                jsonObject = null;
            }
        }
        return (jsonObject != null);
    }

    private boolean setSensorType(JSONObject parentObject, SensorHolder sensorHolder) {
        SensorEvent sensorEvent = sensorHolder.getSensorEvent();
        Sensor sensor = sensorEvent.sensor;
        String typeName;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            String stringType = sensor.getStringType();

            /*
             * Sensor.getStringType() returns dot-separated symbol such like
             * "android.sensor.accelerometer". Extract the last element.
             */
            String[] wkArray = stringType.split("\\.");
            if (wkArray.length > 0) {
                typeName = wkArray[wkArray.length - 1];
            } else {
                typeName = stringType;
            }
        } else {
            if (mSensorTypes == null) {
                mSensorTypes = new SensorTypes();
            }
            typeName = mSensorTypes.getName(sensor.getType());
        }

        try {
            parentObject.put(JsonTags.JSON_TAGS_SENSOR_TYPE.getName(), typeName);
        } catch (JSONException e) {
            Log.e(TAG, "setSensorType: JSONObject.put: " + e);
            parentObject = null;
        }
        return (parentObject != null);
    }

    private boolean setSensorName(JSONObject parentObject, SensorHolder sensorHolder) {
        SensorEvent sensorEvent = sensorHolder.getSensorEvent();
        Sensor sensor = sensorEvent.sensor;
        try {
            parentObject.put(JsonTags.JSON_TAGS_SENSOR_NAME.getName(), sensor.getName());
        } catch (JSONException e) {
            Log.e(TAG, "setSensorName: JSONObject.put: " + e);
            return false;
        }
        return true;
    }

    private boolean setSensorId(JSONObject parentObject, SensorHolder sensorHolder) {
        SensorEvent sensorEvent = sensorHolder.getSensorEvent();
        Sensor sensor = sensorEvent.sensor;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            int sensorId = sensor.getId();
            if (sensorId > 0) {
                try {
                    parentObject.put(JsonTags.JSON_TAGS_SENSOR_ID.getName(), sensorId);
                } catch (JSONException e) {
                    Log.e(TAG, "setSensorId: JSONObject.put: " + e);
                    parentObject = null;
                }
            }
        } else {
            Log.e(TAG, "setSensorId: Sensor.getId() unsupported");
        }
        return (parentObject != null);
    }

    private boolean setTimeStamp(JSONObject parentObject, SensorHolder sensorHolder) {
        long unixTime = sensorHolder.getUnixTime();
        String dateStr = mDateTimeUtil.toIso8601String(unixTime);
        try {
            parentObject.put(JsonTags.JSON_TAGS_SENSOR_TIMESTAMP.getName(), dateStr);
        } catch (JSONException e) {
            Log.e(TAG, "setTimeStamp: JSONObject.put: " + e);
            return false;
        }
        return true;
    }

    private boolean setSensorValues(JSONObject parentObject, SensorHolder sensorHolder) {
        SensorEvent sensorEvent = sensorHolder.getSensorEvent();
        Sensor sensor = sensorEvent.sensor;
        int dimensions = 0;

        // Log.d(TAG, "XXX: SENSOR[id(" + sensor.getType() + "),name(" + sensor.getName() + ")]");
        switch (sensor.getType()) {
            case Sensor.TYPE_LIGHT:
            case Sensor.TYPE_PRESSURE:
            case Sensor.TYPE_PROXIMITY:
            case Sensor.TYPE_RELATIVE_HUMIDITY:
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
            case Sensor.TYPE_STATIONARY_DETECT:
            case Sensor.TYPE_MOTION_DETECT:
            case Sensor.TYPE_HEART_BEAT:
            case Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT:
            case Sensor.TYPE_HINGE_ANGLE:
                /*
                 * Following types are not mentioned in the developer document
                 * https://developer.android.com/reference/android/hardware/SensorEvent#values
                 * but we can safely say those outputs are scalar values.
                 */
            case Sensor.TYPE_STEP_COUNTER:
            case Sensor.TYPE_STEP_DETECTOR:
                dimensions = 1;
                break;

            case Sensor.TYPE_HEADING:
                dimensions = 2;
                break;

            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_GYROSCOPE:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ORIENTATION: /* Deprecated as of API 15 */
                dimensions = 3;
                break;

            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                dimensions = 4;
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                dimensions = 5;
                break;

            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
            case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
            case Sensor.TYPE_HEAD_TRACKER:
            case Sensor.TYPE_ACCELEROMETER_LIMITED_AXES:
            case Sensor.TYPE_GYROSCOPE_LIMITED_AXES:
                dimensions = 6;
                break;

            case Sensor.TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED:
            case Sensor.TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED:
                dimensions = 9;
                break;

            case Sensor.TYPE_POSE_6DOF:
                dimensions = 15;
                break;

            default:
                Log.w(TAG, "SENSOR[id(" + sensor.getType() +
                        "),name(" + sensor.getName() + ")]: Unknown type");
                /* Treat as Scalar value, as a conservative bet */
                break;
        }

        try {
            if (dimensions > 1) {
                /* Vector values */
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < dimensions; i++) {
                    jsonArray.put(sensorEvent.values[i]);
                }
                parentObject.put(
                        JsonTags.JSON_TAGS_SENSOR_VECTOR_VALUES.getName(),
                        jsonArray);
            } else {
                /* Scalar value */
                parentObject.put(
                        JsonTags.JSON_TAGS_SENSOR_SCALAR_VALUE.getName(),
                        sensorEvent.values[0]);
            }
        } catch (JSONException e) {
            Log.e(TAG, "setSensorValues: JSONObject.put: " + e);
            parentObject = null;
        }
        return (parentObject != null);
    }
}
