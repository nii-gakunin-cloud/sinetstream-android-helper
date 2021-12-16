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

public class IpcType {
    /* Client -> Service */
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service. The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, or stop receiving callbacks
     * from the service. The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT = 2;

    public static final int MSG_LIST_SENSOR_TYPES = 3;
    public static final int MSG_ENABLE_SENSORS = 4;
    public static final int MSG_DISABLE_SENSORS = 5;
    public static final int MSG_SET_INTERVAL_TIMER = 6;
    public static final int MSG_SET_LOCATION = 7;
    public static final int MSG_RESET_LOCATION = 71;
    public static final int MSG_SET_USER_DATA = 8;
    /* Client -> Service: Location Specific */
    public static final int MSG_LOCATION_START_UPDATES = 9;
    public static final int MSG_LOCATION_STOP_UPDATES = 10;
    public static final int MSG_LOCATION_RESOLUTION_CORRECTED = 11;

    /* Service -> Client */
    public static final int MSG_SENSOR_DATA = 103;
    /* Service -> Client: Location Specific */
    public static final int MSG_LOCATION_DATA = 104;
    public static final int MSG_LOCATION_PROVIDER_STATUS = 105;
    public static final int MSG_LOCATION_RESOLUTION_REQUIRED = 106;

    /* Client <-> Service */
    public static final int MSG_ERROR = 999;
}
