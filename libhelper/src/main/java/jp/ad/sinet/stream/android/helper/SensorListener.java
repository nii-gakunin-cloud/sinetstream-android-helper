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

package jp.ad.sinet.stream.android.helper;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Public interface for the SensorController user.
 */
public interface SensorListener {
    /**
     * As the successful response of {@link SensorController#getAvailableSensorTypes},
     * {@link SensorService} returns the pair of ArrayLists, one for available
     * sensor types, and the other for those sensor type names.
     * <p>
     *     You can access each ArrayList elements as follows.
     *     <pre>{@code
     *         for (int i = 0; i < sensorTypes.size(i); i++) {
     *             int sensorType = sensorTypes.get(i);
     *             String sensorTypeName = sensorTypeNames.get(i);
     *             ...
     *         }
     *     }</pre>
     * </p>
     *
     * @param sensorTypes ArrayList of available sensor types
     * @param sensorTypeNames ArrayList of sensor type names, such as "accelerometer"
     *
     * @see <a href="https://developer.android.com/reference/android/hardware/Sensor">Sensor</a>
     * for details on possible sensor types (Sensor.TYPE_XXX).
     */
    void onSensorTypesReceived(
            @NonNull ArrayList<Integer> sensorTypes,
            @NonNull ArrayList<String> sensorTypeNames);

    /**
     * Called when SensorService has bound by {@link SensorController#bindSensorService}.
     * <p>
     *     From this point, client can operate sensors on the device.
     * </p>
     *
     * @param info A supplemental message from system, if any
     */
    void onSensorEngaged(@NonNull String info);

    /**
     * Called when SensorService has unbound by {@link SensorController#unbindSensorService}.
     * <p>
     *     Client should wait for this notification before exit.
     * </p>
     *
     * @param info A supplemental message from system, if any
     */
    void onSensorDisengaged(@NonNull String info);

    /**
     * Called when locally-stored sensor data has flushed.
     *
     * <p>
     *     Sample JSON data will look as follows.
     *     <pre>{@code
     *     {
     *         "device":{
     *             "sysinfo":{
     *                 "android":"8.0.0",
     *                 "manufacturer":"Google",
     *                 "model":"Android SDK built for x86"
     *             },
     *             "userinfo":{},
     *             "location":{}
     *         },
     *         "sensors":[
     *             {
     *                 "type":"light",
     *                 "name":"Goldfish Light sensor",
     *                 "timestamp":"20210224T184244.120+0900",
     *                 "value":9894.7001953125
     *             }
     *         ]
     *     }
     *     }</pre>
     * </p>
     *
     * @param jsonData JSON formatted data
     */
    void onSensorDataReceived(@NonNull String jsonData);

    /**
     * Called on any error occasions.
     * @param errmsg Error description message
     */
    void onError(@NonNull String errmsg);
}

