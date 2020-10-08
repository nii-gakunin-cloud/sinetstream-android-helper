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

package jp.ad.sinet.stream.android.helper;

import java.util.ArrayList;

/**
 * Public interface for the SensorController user.
 */
public interface SensorListener {
    /**
     * Called as the successful response of SensorController#getAvailableSensorTypes.
     * @param sensorTypes List of Sensor.TYPE_XXX
     */
    void onSensorTypesReceived(ArrayList<Integer> sensorTypes);

    /**
     * Called when SensorService has bound by SensorController#bindSensorService.
     * From this point, client can operate sensors on the device.
     * @param info A supplemental message from system, if any
     */
    void onSensorEngaged(String info);

    /**
     * Called when SensorService has unbound by SensorController#unbindSensorService.
     * @param info A supplemental message from system, if any
     */
    void onSensorDisengaged(String info);

    /**
     * Called when locally-stored sensor data has flushed.
     * @param data Formatted sensor data for display.
     */
    void onSensorDataReceived(String data);

    /**
     * Called on any error occasions.
     * @param errmsg Error description message
     */
    void onError(String errmsg);
}

