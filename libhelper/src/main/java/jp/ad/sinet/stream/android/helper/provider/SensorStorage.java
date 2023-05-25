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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jp.ad.sinet.stream.android.helper.constants.SensorTypes;
import jp.ad.sinet.stream.android.helper.models.SensorHolder;

public class SensorStorage {
    private final String TAG = SensorStorage.class.getSimpleName();

    final SensorTypes mSensorTypes = new SensorTypes();

    /**
     * A HashMap to keep {@link Sensor} object per sensor type.
     */
    private final Map<Integer, Sensor> mSensorMap = new HashMap<>();

    public void registerSensor(@NonNull Sensor sensor) {
        mSensorMap.put(sensor.getType(), sensor);
    }

    public void unregisterSensor(@NonNull Sensor sensor) {
        mSensorMap.remove(sensor.getType());
    }

    @Nullable
    public Sensor lookupSensor(int sensorType) {
        return mSensorMap.get(sensorType);
    }

    /**
     * Available {@link Sensor} objects on this device are kept in the
     * internal HashMap, which uses the sensor type as the key.
     * This method returns the sorted list of hash keys (= sensor types).
     *
     * @return Sorted list of available sensor types
     */
    @NonNull
    public ArrayList<Integer> getSensorTypes() {
        Set<Integer> mapSet = mSensorMap.keySet();
        ArrayList<Integer> objArray = new ArrayList<>(Arrays.asList(
                mapSet.toArray(new Integer[0])).subList(0, mapSet.size()));
        Collections.sort(objArray);
        return objArray;
    }

    /**
     * Each {@link Sensor} object has its own name such like
     * "Goldfish 3-axis Accelerometer", but it's arbitrarily named by
     * the device vendor and thus difficult to handle programmatically.
     * Instead, we use symbolic name derived from sensor type definitions.
     * For example, we take "accelerometer" from the constant value
     * "android.sensor.accelerometer", defined as
     * {@link Sensor#STRING_TYPE_ACCELEROMETER}.
     *
     * @param sensorType Target sensor type, which is a hash key
     * @return Symbolic sensor type name such like "accelerometer"
     */
    @NonNull
    public String getSensorTypeName(int sensorType) {
        Sensor sensor = mSensorMap.get(sensorType);
        String typeName;

        if (sensor != null) {
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
                /*
                 * Fallback method for old systems which does not support
                 * Sensor.getStringType().
                 */
                typeName = mSensorTypes.getName(sensor.getType());
            }
        } else {
            typeName = "Unknown (type=" + sensorType + ")";
        }
        return typeName;
    }

    /**
     * Generate an ArrayList of SensorTypeName which corresponds to
     * the given ArrayList of SensorType.
     *
     * @param sensorTypes ArrayList of target sensor types
     * @return ArrayList of sensor type names
     */
    @NonNull
    public ArrayList<String> getSensorTypeNames(
            @NonNull ArrayList<Integer> sensorTypes) {
        ArrayList<String> objArray = new ArrayList<>();
        for (int i = 0; i < sensorTypes.size(); i++) {
            int sensorType = sensorTypes.get(i); /* sensorType may not be contiguous */
            String typeName = getSensorTypeName(sensorType);
            objArray.add(typeName);
        }
        return objArray;
    }

    /**
     * A HashMap to keep {@link SensorEvent} object per sensor type.
     */
    private final Map<Integer, SensorHolder> mSensorEventMap = new HashMap<>();

    /**
     * Keep the given {@link SensorEvent} object along with timestamp
     * in the internal HashMap.
     *
     * @param sensorEvent the {@link SensorEvent} object notified from system
     * @param unixTime timestamp of the notification
     */
    public void setSensorEvent(@NonNull SensorEvent sensorEvent, long unixTime) {
        // Log.d(TAG, "setSensorEvent(" + unixTime + "): " + sensorEvent.sensor.toString());
        int sensorType = sensorEvent.sensor.getType();

        SensorHolder sensorHolder = mSensorEventMap.get(sensorType);
        if (sensorHolder != null) {
            /* update current entry */
            sensorHolder.setSensorEvent(sensorEvent);
            sensorHolder.setUnixTime(unixTime);
        } else {
            /* allocate new entry */
            sensorHolder = new SensorHolder(sensorEvent, unixTime);
        }
        mSensorEventMap.put(sensorType, sensorHolder);
    }

    /**
     * Generate an ArrayList of {@link SensorHolder} objects as the
     * latest collection of {@link SensorEvent} object and timestamp.
     *
     * @return ArrayList of {@link SensorHolder} objects
     */
    public ArrayList<SensorHolder> getSensorHolders() {
        ArrayList<SensorHolder> objArray = new ArrayList<>();
        Set<Integer> mapSet = mSensorEventMap.keySet();
        for (int i = 0; i < mapSet.size(); i++) {
            Integer sensorType = mapSet.toArray(new Integer[0])[i];
            SensorHolder sensorHolder = mSensorEventMap.get(sensorType);
            if (sensorHolder != null) {
                objArray.add(sensorHolder);
            } else {
                Log.e(TAG, "SensorHolder(sensorType=" + sensorType + ") not found?");
            }
        }
        return objArray;
    }

    /**
     * Clear the HashMap to keep {@link SensorEvent} objects.
     */
    public void clearSensorEvent() {
        mSensorEventMap.clear();
    }
}
