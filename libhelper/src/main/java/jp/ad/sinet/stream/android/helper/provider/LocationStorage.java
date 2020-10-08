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

public class LocationStorage {
    private float mLongitude = Float.NaN;
    private float mLatitude = Float.NaN;

    public float getLongitude() {
        return mLongitude;
    }

    public void setLongitude(float longitude) {
        this.mLongitude = longitude;
    }

    public float getLatitude() {
        return mLatitude;
    }

    public void setLatitude(float latitude) {
        this.mLatitude = latitude;
    }

    public void setLocation(float longitude, float latitude) {
        setLongitude(longitude);
        setLatitude(latitude);
    }
}
