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
        /* type=1 */
        mNameMap.put(Sensor.TYPE_ACCELEROMETER, "accelerometer");
        /* type=2 */
        mNameMap.put(Sensor.TYPE_MAGNETIC_FIELD, "magnetic_field");
        /* type=3 */
        mNameMap.put(Sensor.TYPE_ORIENTATION, "orientation");
        /* type=4 */
        mNameMap.put(Sensor.TYPE_GYROSCOPE, "gyroscope");
        /* type=5 */
        mNameMap.put(Sensor.TYPE_LIGHT, "light");
        /* type=6 */
        mNameMap.put(Sensor.TYPE_PRESSURE, "pressure");
        /* type=7 */
        mNameMap.put(Sensor.TYPE_TEMPERATURE, "temperature");
        /* type=8 */
        mNameMap.put(Sensor.TYPE_PROXIMITY, "proximity");
        /* type=9 */
        mNameMap.put(Sensor.TYPE_GRAVITY, "gravity");
        /* type=10 */
        mNameMap.put(Sensor.TYPE_LINEAR_ACCELERATION, "linear_acceleration");
        /* type=11 */
        mNameMap.put(Sensor.TYPE_ROTATION_VECTOR, "rotation_vector");
        /* type=12 */
        mNameMap.put(Sensor.TYPE_RELATIVE_HUMIDITY, "relative_humidity");
        /* type=13 */
        mNameMap.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "ambient_temperature");
        /* type=14 */
        mNameMap.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, "magnetic_field_uncalibrated");
        /* type=15 */
        mNameMap.put(Sensor.TYPE_GAME_ROTATION_VECTOR, "game_rotation_vector");
        /* type=16 */
        mNameMap.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, "gyroscope_uncalibrated");
        /* type=17 */
        mNameMap.put(Sensor.TYPE_SIGNIFICANT_MOTION, "significant_motion");
        /* type=18 */
        mNameMap.put(Sensor.TYPE_STEP_DETECTOR, "step_detector");
        /* type=19 */
        mNameMap.put(Sensor.TYPE_STEP_COUNTER, "step_counter");
        /* type=20 */
        mNameMap.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, "geomagnetic_rotation_vector");
        /* type=21 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            mNameMap.put(Sensor.TYPE_HEART_RATE, "heart_rate");
        }
        /* type=28 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_POSE_6DOF, "pose_6dof");
        }
        /* type=29 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_STATIONARY_DETECT, "stationary_detect");
        }
        /* type=30 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_MOTION_DETECT, "motion_detect");
        }
        /* type=31 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNameMap.put(Sensor.TYPE_HEART_BEAT, "heart_beat");
        }
        /* type=34 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNameMap.put(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, "low_latency_offbody_detect");
        }
        /* type=35 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNameMap.put(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED, "accelerometer_uncalibrated");
        }
        /* type=36 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mNameMap.put(Sensor.TYPE_HINGE_ANGLE, "hinge_angle");
        }
    }

    public String getName(int type) {
        String name = mNameMap.get(type);
        return ((name != null) ? name : "Unknown(" + type + ")");
    }
}
