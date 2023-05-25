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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.SignalStrength;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;
import jp.ad.sinet.stream.android.helper.constants.NetworkTypes;
import jp.ad.sinet.stream.android.helper.provider.CellularParser;

public class CellularMonitor {
    private static final String TAG = CellularMonitor.class.getSimpleName();

    /**
     * Messenger for communicating with the service.
     */
    private Messenger mService = null;

    private boolean mIsBound = false;
    private final CellularMonitorListener mListener;
    private final Context mContext;
    private final int mClientId;

    public CellularMonitor(@NonNull AppCompatActivity activity, int clientId) {
        mClientId = clientId;
        if (activity instanceof CellularMonitorListener) {
            mContext = activity;
            mListener = (CellularMonitorListener) activity;
        } else {
            throw new RuntimeException(activity
                    + " must implement CellularMonitorListener");
        }
    }

    /**
     * Check system settings before starting the cellular service.
     * <p>
     *     This method triggers the series of checks to see if current settings
     *     are adequate for using the CellularService.
     *
     *     If everything goes fine, {@link CellularService}
     *     will be automatically started followed by a notification
     *     {@link CellularMonitorListener#onCellularSettingsChecked(boolean)}
     *     with {@code isReady} argument set to {@code true}.
     *     Otherwise, the same notification will be delivered with {@code isReady}
     *     argument set to {@code false}.
     * </p>
     */
    public void start() {
        startCellularService();
    }

    /**
     * Stops the cellular service if it's running.
     */
    public void stop() {
        stopCellularService();
    }

    private void startCellularService() {
        Intent intent = new Intent(mContext, CellularService.class);

        Log.d(TAG, "Going to start cellular service");
        try {
            ComponentName componentName = mContext.startService(intent);
            if (componentName == null) {
                mListener.onError(
                        TAG + ": Cellular service not found?");
            }
        } catch (SecurityException | IllegalStateException e) {
            mListener.onError(
                    "Cannot start cellular service: " + e.getMessage());
        }
    }

    private void stopCellularService() {
        Intent intent = new Intent(mContext, CellularService.class);

        Log.d(TAG, "Going to stop cellular service");
        try {
            mContext.stopService(intent);
        } catch (SecurityException | IllegalStateException e) {
            mListener.onError(
                    "Cannot stop cellular service: " + e.getMessage());
        }
    }

    /**
     * Binds the cellular service to open a communication line.
     * <p>
     *     Once the connection has established, we can send/receive IPC messages
     *     with the cellular service.
     * </p>
     */
    public void bindCellularService() {
        Log.d(TAG, "bindCellularService");

        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        if (mIsBound) {
            Log.w(TAG, "bindCellularService: Already bound?");
        } else {
            Intent intent = new Intent(mContext, CellularService.class);
            try {
                mIsBound = mContext.bindService(
                        intent,
                        mConnection,
                        Context.BIND_AUTO_CREATE);
                if (!mIsBound) {
                    Log.w(TAG, "bindCellularService: bindService failed?");
                }
            } catch (SecurityException e) {
                mListener.onError(TAG + ": bindCellularService: " + e.getMessage());
            }
        }
    }

    /**
     * Unbinds the cellular service.
     */
    public void unbindCellularService() {
        Log.d(TAG, "unbindCellularService");

        // If we have received the service, and hence registered with
        // it, then now is the time to unregister.
        if (mService != null) {
            try {
                Message msg = Message.obtain(null,
                        IpcType.MSG_UNREGISTER_CLIENT,
                        0, mClientId);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                mListener.onError(TAG + ": unbindCellularService: " + e.getMessage());
            }

            // Detach out existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void getNetworkSummary(@NonNull Bundle bundle) {
        int networkType = bundle.getInt(
                BundleKeys.BUNDLE_KEY_CELLULAR_NETWORK_TYPE, -1);
        String networkOperator = bundle.getString(
                BundleKeys.BUNDLE_KEY_CELLULAR_NETWORK_OPERATOR, null);
        SignalStrength ss = bundle.getParcelable(
                BundleKeys.BUNDLE_KEY_CELLULAR_PARCELABLE);

        String networkName;
        if (networkOperator != null && !networkOperator.isEmpty()) {
            networkName = NetworkTypes.toName(networkType) + ", " + networkOperator;
        } else {
            networkName = NetworkTypes.toName(networkType);
        }

        CellularParser parser = new CellularParser(
                new CellularParser.CellularParserListener() {
                    @Override
                    public void onGsmInfo(@Nullable Integer rssi,
                                          @Nullable Integer ber) {
                        int nelem = 0;
                        String data = "";
                        if (rssi != null) {
                            data += "rssi(" + rssi + ")";
                            nelem++;
                        }
                        if (ber != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ber(" + ber + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onCdmaInfo(@Nullable Integer rssi,
                                           @Nullable Integer ecio) {
                        int nelem = 0;
                        String data = "";
                        if (rssi != null) {
                            data += "rssi(" + rssi + ")";
                            nelem++;
                        }
                        if (ecio != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ecio(" + ecio + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onEvdoInfo(@Nullable Integer rssi,
                                           @Nullable Integer ecio,
                                           @Nullable Integer snr) {
                        int nelem = 0;
                        String data = "";
                        if (rssi != null) {
                            data += "rssi(" + rssi + ")";
                            nelem++;
                        }
                        if (ecio != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ecio(" + ecio + ")";
                            nelem++;
                        }
                        if (snr != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "snr(" + snr + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onTdsCdmaInfo(@Nullable Integer rscp) {
                        String data = "";
                        if (rscp != null) {
                            data += "rscp(" + rscp + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onWcdmaInfo(@Nullable Integer rscp,
                                            @Nullable Integer ecno) {
                        int nelem = 0;
                        String data = "";
                        if (rscp != null) {
                            data += "rscp(" + rscp + ")";
                            nelem++;
                        }
                        if (ecno != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ecno(" + ecno + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onLteInfo(@Nullable Integer rssi,
                                          @Nullable Integer rsrp,
                                          @Nullable Integer rsrq,
                                          @Nullable Integer rssnr,
                                          @Nullable Integer cqi,
                                          @Nullable Integer cqiIndex,
                                          @Nullable Integer ta) {
                        int nelem = 0;
                        String data = "";
                        if (rssi != null) {
                            data += "rssi(" + rssi + ")";
                            nelem++;
                        }
                        if (rsrp != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "rsrp(" + rsrp + ")";
                            nelem++;
                        }
                        if (rsrq != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "rsrq(" + rsrq + ")";
                            nelem++;
                        }
                        if (rssnr != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "rssnr(" + rssnr + ")";
                            nelem++;
                        }
                        if (cqi != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "cqi(" + cqi + ")";
                            nelem++;
                        }
                        if (cqiIndex != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "cqiIndex(" + cqiIndex + ")";
                            nelem++;
                        }
                        if (ta != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ta(" + ta + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onNrInfo(@Nullable Integer ssRsrp,
                                         @Nullable Integer ssRsrq,
                                         @Nullable Integer ssSinr) {
                        int nelem = 0;
                        String data = "";
                        if (ssRsrp != null) {
                            data += "ssRsrp(" + ssRsrp + ")";
                            nelem++;
                        }
                        if (ssRsrq != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ssRsrq(" + ssRsrq + ")";
                            nelem++;
                        }
                        if (ssSinr != null) {
                            if (nelem > 0) {
                                data += ",";
                            }
                            data += "ssSinr(" + ssSinr + ")";
                        }
                        mListener.onCellularSummary(networkName, data);
                    }

                    @Override
                    public void onOthersInfo(@Nullable String data) {
                        mListener.onCellularSummary(networkName, (data != null) ? data : "N/A");
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        mListener.onError(description);
                    }
                });

        parser.parse(networkType, ss);
    }

    /**
     * IPC endpoint to send messages to Service.
     */
    private final Messenger mMessenger =
            new Messenger(new CellularMonitor.IncomingHandler(
                    Looper.getMainLooper(),this));

    /**
     * Handler for incoming messages from Service.
     */
    private static class IncomingHandler extends Handler {
        final WeakReference<CellularMonitor> weakReference;

        /*
         * Default constructor in android.os.Handler is deprecated.
         * We need to use an Executor or specify the Looper explicitly.
         */
        IncomingHandler(
                @NonNull Looper looper, @NonNull CellularMonitor cellularMonitor) {
            super(looper);

            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(cellularMonitor);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg A Message object containing a bundle of data
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            CellularMonitor cellularMonitor = weakReference.get();
            if (cellularMonitor != null) {
                cellularMonitor.onServerMessageReceived(msg);
            } else {
                Log.w(TAG, "handleMessage: CellularMonitor has gone");
            }
            super.handleMessage(msg);
        }
    }

    private void onServerMessageReceived(@NonNull Message msg) {
        int result_code = msg.arg1;
        Bundle bundle = msg.getData();

        switch (msg.what) {
            case IpcType.MSG_REGISTER_CLIENT:
                Log.d(TAG, "RX: REGISTER_CLIENT: result=" + result_code);
                requestCellularStartUpdates();
                break;
            case IpcType.MSG_UNREGISTER_CLIENT:
                Log.d(TAG, "RX: UNREGISTER_CLIENT: result=" + result_code);
                break;
            case IpcType.MSG_CELLULAR_DATA:
                if (bundle != null) {
                    mListener.onCellularDataReceived(bundle);
                } else {
                    Log.w(TAG, "MSG_CELLULAR_DATA: No bundle?");
                }
                break;
            case IpcType.MSG_ERROR:
                if (bundle != null) {
                    String errmsg = bundle.getString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE);
                    if (errmsg != null) {
                        mListener.onError(errmsg);
                    } else {
                        Log.w(TAG, "MSG_ERROR: Invalid bundle: " + bundle);
                    }
                } else {
                    Log.w(TAG, "MSG_ERROR: No bundle?");
                }
                break;
            default:
                Log.e(TAG, "Unknown IpcType: " + msg.what);
                break;
        }
    }

    /**
     * Callback interfaces for Service connection management.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            Log.d(TAG, "ServiceConnection.onServiceConnected: " + name.toString());
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            Message msg = Message.obtain(
                    null, IpcType.MSG_REGISTER_CLIENT, 0, mClientId);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (an then reconnected if it can be restarted)
                // so there is no need to do anything here.
                Log.e(TAG, "Messenger.send: " + e.getMessage());
                return;
            }

            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d(TAG, "ServiceConnection.onServiceDisconnected: " + name.toString());
            mService = null;

            mIsBound = false;
        }
    };

    private void requestCellularStartUpdates() {
        Log.d(TAG, "requestCellularStartUpdate");
        sendMessage(IpcType.MSG_CELLULAR_START_UPDATES);
    }

    private void requestCellularStopUpdates() {
        Log.d(TAG, "requestCellularStopUpdate");
        sendMessage(IpcType.MSG_CELLULAR_STOP_UPDATES);
    }

    private void sendMessage(int ipcType) {
        Message msg = Message.obtain(null, ipcType, 0, mClientId);
        msg.replyTo = mMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger.send: " + e.getMessage());
            mListener.onError("Cannot send message: " + e.getMessage());
        }
    }
}
