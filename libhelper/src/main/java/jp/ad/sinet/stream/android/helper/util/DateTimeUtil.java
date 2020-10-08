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

package jp.ad.sinet.stream.android.helper.util;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtil {
    private final static String TAG = DateTimeUtil.class.getSimpleName();

    private final SimpleDateFormat mSimpleDateFormat;

    public DateTimeUtil() {
        /* We use standard ISO 8601 format. */
        String TIMESTAMP_FORMAT_ISO8601 = "yyyyMMdd'T'HHmmss.SSSZ";
        mSimpleDateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT_ISO8601, Locale.US);

        /* We need TimeZone to convert from UTC to local time. */
        TimeZone tz = TimeZone.getDefault();
        mSimpleDateFormat.setTimeZone(tz);
    }

    public long getUnixTime() {
        return System.currentTimeMillis();
    }

    public String toIso8601String(long unixTime) {
        String dstDateString;
        try {
            dstDateString = mSimpleDateFormat.format(new Date(unixTime));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid unixTime(" + unixTime + "): " + e.getMessage());
            dstDateString = "" + unixTime; /* Fallback to raw value */
        }
        return dstDateString;
    }
}
