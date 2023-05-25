/*
 * Copyright (c) 2021 National Institute of Informatics
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

import android.location.Location;

import androidx.annotation.NonNull;

public interface LocationTrackerListener {
    /**
     * Called when device check for location availability has finished.
     * @param isReady true if we can call {@link LocationTracker#bindLocationService}.
     */
    void onLocationSettingsChecked(boolean isReady);

    /**
     * Called when LocationService (either GPS or FLP) has bound by
     * {@link LocationTracker#bindLocationService}.
     * <p>
     *     From this point, client can receive Location notifications.
     * </p>
     *
     * @param info A supplemental message from system, if any
     */
    void onLocationEngaged(@NonNull String info);

    /**
     * Called when LocationService (either GPS or FLP) has unbound by
     * {@link LocationTracker#unbindLocationService}.
     * <p>
     *     Client should wait for this notification before exit.
     * </p>
     *
     * @param info A supplemental message from system, if any
     */
    void onLocationDisengaged(@NonNull String info);

    /**
     * Called when new Location data has received.
     *
     * @param location the {@link Location} object notified from system
     */
    void onLocationDataReceived(@NonNull Location location);

    /**
     * Called on any error occasions.
     * @param description Error description message
     */
    void onError(@NonNull String description);
}
