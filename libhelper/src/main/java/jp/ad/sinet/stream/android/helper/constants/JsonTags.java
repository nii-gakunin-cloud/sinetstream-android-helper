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

public enum JsonTags {
    JSON_TAGS_DEVICE("device"),
    JSON_TAGS_SYSINFO("sysinfo"),
    JSON_TAGS_USERINFO("userinfo"),
    JSON_TAGS_USERINFO_PUBLISHER("publisher"),
    JSON_TAGS_USERINFO_NOTE("note"),
    JSON_TAGS_LOCATION("location"),
    JSON_TAGS_LOCATION_LONGITUDE("longitude"),
    JSON_TAGS_LOCATION_LATITUDE("latitude"),
    JSON_TAGS_SENSORS("sensors"),
    JSON_TAGS_SENSOR_TYPE("type"),
    JSON_TAGS_SENSOR_NAME("name"),
    JSON_TAGS_SENSOR_ID("id"),
    JSON_TAGS_SENSOR_SCALAR_VALUE("value"),
    JSON_TAGS_SENSOR_VECTOR_VALUES("values"),
    JSON_TAGS_SENSOR_TIMESTAMP("timestamp"),;

    private final String mName;

    JsonTags(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }
}
