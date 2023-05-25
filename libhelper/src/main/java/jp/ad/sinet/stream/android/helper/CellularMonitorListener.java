/*
 * Copyright (c) 2022 National Institute of Informatics
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

import android.os.Bundle;

import androidx.annotation.NonNull;

public interface CellularMonitorListener {
    /**
     * Called when device check for cellular network availability has finished.
     * @param isReady true if we can call {@link CellularMonitor#bindCellularService}.
     */
    void onCellularSettingsChecked(boolean isReady);

    /**
     * Called when new cellular network data has received.
     *
     * @param bundle the telephony data received from {@link CellularService}
     */
    void onCellularDataReceived(@NonNull Bundle bundle);

    /**
     * Called when the {@link Bundle} data has parsed.
     *
     * @param networkType the currently connected network type, such as LTE.
     * @param data brief summary of radio signal values
     */
    void onCellularSummary(@NonNull String networkType, @NonNull String data);

    /**
     * Called on any error occasions.
     * @param description Error description message
     */
    void onError(@NonNull String description);
}
