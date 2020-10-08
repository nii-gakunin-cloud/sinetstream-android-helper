/*
 * Copyright (C) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.android.helper.constants;

import android.hardware.Sensor;
import android.os.Build;

import java.util.HashMap;
import java.util.Map;

public class SensorTypes {
    private final Map<Integer, String> mNameMap = new HashMap<>();

    /*
     * Sensor.getStringType() is supported from Build.VERSION_CODES.KITKAT_WATCH.
     * This is an effort to resolve sensor type name for old systems.
     *
     * [NB] Sensor.TYPE_XXX may be added/obsoleted as the progress of Android SDK.
     */
    public SensorTypes() {
        mNameMap.put(Sensor.TYPE_ACCELEROMETER, "accelerometer");
        mNameMap.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "ambient_temperature");
        mNameMap.put(Sensor.TYPE_GAME_ROTATION_VECTOR, "game_rotation_vector");
        mNameMap.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, "geomagnetic_rotation_vector");
        mNameMap.put(Sensor.TYPE_GRAVITY, "gravity");
        mNameMap.put(Sensor.TYPE_GYROSCOPE, "gyroscope");
        mNameMap.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, "gyroscope_uncalibrated");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_HEART_BEAT, "heart_beat");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            mNameMap.put(Sensor.TYPE_HEART_RATE, "heart_rate");
        }
        mNameMap.put(Sensor.TYPE_LIGHT, "light");
        mNameMap.put(Sensor.TYPE_LINEAR_ACCELERATION, "linear_acceleration");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNameMap.put(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, "low_latency_offbody_detect");
        }
        mNameMap.put(Sensor.TYPE_MAGNETIC_FIELD, "magnetic_field");
        mNameMap.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, "magnetic_field_uncalibrated");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_MOTION_DETECT, "motion_detect");
        }
        mNameMap.put(Sensor.TYPE_ORIENTATION, "orientation");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_POSE_6DOF, "pose_6dof");
        }
        mNameMap.put(Sensor.TYPE_PRESSURE, "pressure");
        mNameMap.put(Sensor.TYPE_PROXIMITY, "proximity");
        mNameMap.put(Sensor.TYPE_RELATIVE_HUMIDITY, "relative_humidity");
        mNameMap.put(Sensor.TYPE_ROTATION_VECTOR, "rotation_vector");
        mNameMap.put(Sensor.TYPE_SIGNIFICANT_MOTION, "significant_motion");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_STATIONARY_DETECT, "stationary_detect");
        }
        mNameMap.put(Sensor.TYPE_STEP_COUNTER, "step_counter");
        mNameMap.put(Sensor.TYPE_STEP_DETECTOR, "step_detector");
        mNameMap.put(Sensor.TYPE_TEMPERATURE, "temperature");
    }

    public String getName(int type) {
        String name = mNameMap.get(type);
        return ((name != null) ? name : "Unknown(" + type + ")");
    }
}
