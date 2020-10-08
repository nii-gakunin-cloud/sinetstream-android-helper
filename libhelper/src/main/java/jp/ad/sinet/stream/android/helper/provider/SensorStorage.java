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

package jp.ad.sinet.stream.android.helper.provider;

import android.hardware.SensorEvent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jp.ad.sinet.stream.android.helper.models.SensorHolder;

public class SensorStorage {
    private final static String TAG = SensorStorage.class.getSimpleName();

    /**
     * Keep SensorInfo object per sensor type
     */
    private static class SensorInfo {
        private final String sensorName;
        // Other attributes may follow

        private SensorInfo(@NonNull String sensorName) {
            this.sensorName = sensorName;
        }

        public String getSensorName() {
            return sensorName;
        }
    }
    private final Map<Integer, SensorInfo> mSensorMap = new HashMap<>();

    public void registerSensorInfo(int sensorType, @NonNull String sensorName) {
        SensorInfo newValue = new SensorInfo(sensorName);
        mSensorMap.put(sensorType, newValue);
    }

    @Nullable
    public String getSensorName(int sensorType) {
        SensorInfo sensorInfo = mSensorMap.get(sensorType);
        return (sensorInfo != null) ? sensorInfo.getSensorName() : null;
    }

    public ArrayList<Integer> getSensorTypes() {
        Set<Integer> mapSet = mSensorMap.keySet();
        ArrayList<Integer> objArray = new ArrayList<>(Arrays.asList(
                mapSet.toArray(new Integer[0])).subList(0, mapSet.size()));
        Collections.sort(objArray);
        return objArray;
    }

    /**
     * Keep SensorEvent object per sensor type
     */
    private final Map<Integer, SensorHolder> mSensorEventMap = new HashMap<>();

    public void setSensorEvent(SensorEvent sensorEvent, long unixTime) {
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

    public ArrayList<SensorHolder> getSensorHolders() {
        ArrayList<SensorHolder> objArray = new ArrayList<>();
        Set<Integer> mapSet = mSensorEventMap.keySet();
        for (int i = 0; i < mapSet.size(); i++) {
            Integer sensorType = mapSet.toArray(new Integer[0])[i];
            SensorHolder sensorHolder = mSensorEventMap.get(sensorType);
            objArray.add(sensorHolder);
        }
        return objArray;
    }
}
