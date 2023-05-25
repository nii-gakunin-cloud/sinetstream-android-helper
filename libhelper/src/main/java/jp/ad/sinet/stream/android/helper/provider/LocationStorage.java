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
    private double mLatitude = Double.NaN;
    private double mLongitude = Double.NaN;
    private long mUtcTime = -1;

    public double getLongitude() {
        return mLongitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public long getUtcTime() {
        return mUtcTime;
    }

    public void setLocation(double latitude, double longitude, long utcTime) {
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mUtcTime = utcTime;
    }

    public void resetLocation() {
        setLocation(Double.NaN, Double.NaN, -1);
    }
}
