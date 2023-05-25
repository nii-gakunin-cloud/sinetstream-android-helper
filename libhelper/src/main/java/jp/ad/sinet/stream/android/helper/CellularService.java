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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;
import jp.ad.sinet.stream.android.helper.constants.NetworkTypes;

public class CellularService extends Service {
    private final String TAG = CellularService.class.getSimpleName();

    /* Keep track of all current registered clients */
    private final List<Messenger> mClients = new ArrayList<>();

    private TelephonyManager mTelephonyManager = null;
    private CustomTelephonyCallback mCustomTelephonyCallback = null;
    private PhoneStateListener mPhoneStateListener = null;
    private boolean mIsTelephonyAvailable = false;

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        checkTelephonySettings();
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        cleanupListener();
        super.onDestroy();
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * {@link Context#startService}, providing the arguments it supplied and a
     * unique integer token representing the start request.  Do not call this method directly.
     *
     * <p>For backwards compatibility, the default implementation calls
     * {@link #onStart} and returns either {@link #START_STICKY}
     * or {@link #START_STICKY_COMPATIBILITY}.
     *
     * <p class="caution">Note that the system calls this on your
     * service's main thread.  A service's main thread is the same
     * thread where UI operations take place for Activities running in the
     * same process.  You should always avoid stalling the main
     * thread's event loop.  When doing long-running operations,
     * network calls, or heavy disk I/O, you should kick off a new
     * thread, or use {@link AsyncTask}.</p>
     *
     * @param intent  The Intent supplied to {@link Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state.  It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     * @see #stopSelfResult(int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // return super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand: Intent=" +
                (intent != null ? intent.toString() : "") +
                ", flags=" + flags +
                ", startId=" + startId);

        // We don't want this service being automatically restarted by
        // Android TaskManager.
        return START_NOT_STICKY;
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     *
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Intent="
                + (intent != null ? intent.toString() : ""));
        return mMessenger.getBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind: Intent="
                + (intent != null ? intent.toString() : ""));
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: Intent="
                + (intent != null ? intent.toString() : ""));
        return super.onUnbind(intent);
    }

    @SuppressLint("MissingPermission")
    private void checkTelephonySettings() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mTelephonyManager.isDataEnabled()) {
                    mIsTelephonyAvailable = true;
                } else {
                    Log.w(TAG, "Mobile data is DISABLED, give up");
                }
            } else {
                mIsTelephonyAvailable = true;
            }
        } else {
            Log.w(TAG, "TelephonyManager is NOT available, give up");
        }

        if (mIsTelephonyAvailable) {
            setupListener();
        }
    }

    private void setupListener() {
        /* TODO Fill in TelephonyCallback cases
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTelephonyCallback();
        } else {
            setPhoneStateListener();
        }
         */
        setPhoneStateListener();
    }

    private void cleanupListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            clearTelephonyCallback();
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private void setTelephonyCallback() {
        mCustomTelephonyCallback = new CustomTelephonyCallback(
                new CustomTelephonyCallback.CustomTelephonyCallbackListener() {
                    @Override
                    public void onSignalStrength(@NonNull SignalStrength signalStrength) {
                        handleSignalStrength(signalStrength);
                    }
                });

        try {
            mTelephonyManager.registerTelephonyCallback(
                    getMainExecutor(), mCustomTelephonyCallback);
        } catch (SecurityException | IllegalStateException e) {
            String description = TAG +
                    ": TelephonyManager.registerTelephonyCallback: " + e.getMessage();
            errorReply(null, description);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void clearTelephonyCallback() {
        if (mCustomTelephonyCallback != null) {
            mTelephonyManager.unregisterTelephonyCallback(mCustomTelephonyCallback);
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static class CustomTelephonyCallback extends TelephonyCallback
            implements TelephonyCallback.SignalStrengthsListener {

        private final CustomTelephonyCallbackListener mListener;

        private CustomTelephonyCallback(CustomTelephonyCallbackListener listener) {
            this.mListener = listener;
        }

        /**
         * Callback invoked when network signal strengths changes on the registered subscription.
         * Note, the registration subscription ID comes from {@link TelephonyManager} object
         * which registers TelephonyCallback by
         * {@link TelephonyManager#registerTelephonyCallback(Executor, TelephonyCallback)}.
         * If this TelephonyManager object was created with
         * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
         * subscription ID. Otherwise, this callback applies to
         * {@link SubscriptionManager#getDefaultSubscriptionId()}.
         *
         * @param signalStrength the set of phone signal strength information
         */
        @Override
        public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {
            mListener.onSignalStrength(signalStrength);
        }

        interface CustomTelephonyCallbackListener {
            void onSignalStrength(@NonNull SignalStrength signalStrength);
        }
    }

    private void setPhoneStateListener() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            mPhoneStateListener = new PhoneStateListener(getMainExecutor()) {
                /**
                 * Callback invoked when network signal strengths changes on the registered subscription.
                 * Note, the registration subId comes from {@link TelephonyManager} object which registers
                 * PhoneStateListener by {@link TelephonyManager#listen(PhoneStateListener, int)}.
                 * If this TelephonyManager object was created with
                 * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
                 * subId. Otherwise, this callback applies to
                 * {@link SubscriptionManager#getDefaultSubscriptionId()}.
                 *
                 * @param signalStrength the set of phone signal strength information
                 * @deprecated Use {@link TelephonyCallback.SignalStrengthsListener} instead.
                 */
                @Deprecated
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    handleSignalStrength(signalStrength);
                }
            };
        } else {
            mPhoneStateListener = new PhoneStateListener() {
                /**
                 * Callback invoked when network signal strengths changes on the registered subscription.
                 * Note, the registration subId comes from {@link TelephonyManager} object which registers
                 * PhoneStateListener by {@link TelephonyManager#listen(PhoneStateListener, int)}.
                 * If this TelephonyManager object was created with
                 * {@link TelephonyManager#createForSubscriptionId(int)}, then the callback applies to the
                 * subId. Otherwise, this callback applies to
                 * {@link SubscriptionManager#getDefaultSubscriptionId()}.
                 *
                 * @param signalStrength the set of phone signal strength information
                 * @deprecated Use {@link TelephonyCallback.SignalStrengthsListener} instead.
                 */
                @Deprecated
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    handleSignalStrength(signalStrength);
                }
            };
        }
    }

    private void handleSignalStrength(@NonNull SignalStrength signalStrength) {
        Log.d(TAG, signalStrength.toString());
        int networkType = getNetworkType();
        String networkOperator = mTelephonyManager.getNetworkOperatorName();

        reportSignalStrength(networkType, networkOperator, signalStrength);
    }

    private void startMonitoringPhoneState() {
        if (mTelephonyManager != null) {
            if (mIsTelephonyAvailable) {
                try {
                    Log.d(TAG, "Going to START monitoring the phone state");
                    mTelephonyManager.listen(
                            mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                } catch (SecurityException | IllegalStateException e) {
                    String description = TAG + ": TelephonyManager.listen: " + e.getMessage();
                    errorReply(null, description);
                }
            } else {
                String description = TAG + "Mobile data is DISABLED on this device";
                errorReply(null, description);
            }
        } else {
            String description = TAG + ": TelephonyManager is NOT available on this device";
            errorReply(null, description);
        }
    }

    private void stopMonitoringPhoneState() {
        if (mPhoneStateListener != null) {
            try {
                Log.d(TAG, "Going to STOP monitoring the phone state");
                mTelephonyManager.listen(
                        mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            } catch (SecurityException | IllegalStateException e) {
                String description = TAG + ": TelephonyManager.listen: " + e.getMessage();
                errorReply(null, description);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private int getNetworkType() {
        /*
         * Here we assume the required permission READ_PHONE_STATE
         * has already granted at this point
         */
        int networkType;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            networkType = mTelephonyManager.getDataNetworkType();
        } else {
            networkType = mTelephonyManager.getNetworkType();
        }
        Log.d(TAG, "NetworkType: " + networkType +
                "(" + NetworkTypes.toName(networkType) + ")");
        Log.d(TAG, "NetworkOperator: " + mTelephonyManager.getNetworkOperatorName());
        Log.d(TAG, "SIM Operator: " + mTelephonyManager.getSimOperatorName());
        Log.d(TAG, "PhoneType: " + mTelephonyManager.getPhoneType());
        return networkType;
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger =
            new Messenger(new IncomingHandler(
                    Looper.getMainLooper(),this));

    /**
     * Handler for incoming messages from clients.
     */
    private static class IncomingHandler extends Handler {
        private final String TAG_HANDLER = CellularService.IncomingHandler.class.getSimpleName();
        final WeakReference<CellularService> weakReference;

        /*
         * Default constructor in android.os.Handler is deprecated.
         * We need to use an Executor or specify the Looper explicitly.
         */
        IncomingHandler(
                @NonNull Looper looper, @NonNull CellularService cellularService) {
            super(looper);

            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(cellularService);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg The {@link Message} object sent from a client.
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            CellularService cellularService = weakReference.get();
            if (cellularService != null) {
                cellularService.onClientMessageReceived(msg);
            } else {
                Log.w(TAG_HANDLER, "handleMessage: CellularService has gone");
            }
            super.handleMessage(msg);
        }
    }

    private void onClientMessageReceived(@NonNull Message msg) {
        switch (msg.what) {
            case IpcType.MSG_REGISTER_CLIENT:
                Log.d(TAG, "RX: REGISTER_CLIENT");
                mClients.add(msg.replyTo);
                successReply(msg.replyTo, msg.what);
                break;
            case IpcType.MSG_UNREGISTER_CLIENT:
                Log.d(TAG, "RX: UNREGISTER_CLIENT");
                mClients.remove(msg.replyTo);
                successReply(msg.replyTo, msg.what);
                break;
            case IpcType.MSG_CELLULAR_START_UPDATES:
                Log.d(TAG, "RX: CELLULAR_START_UPDATES");
                startMonitoringPhoneState();
                break;
            case IpcType.MSG_CELLULAR_STOP_UPDATES:
                Log.d(TAG, "RX: CELLULAR_STOP_UPDATES");
                stopMonitoringPhoneState();
                break;
            default:
                Log.w(TAG, "Unknown IPC message: " + msg.what);
                errorReply(msg.replyTo, TAG + ": Unknown IPC message: " + msg.what);
                break;
        }
    }

    private void sendToClient(
            @Nullable Messenger client, int what, int result_code, @Nullable Bundle bundle) {
        try {
            /*
             * Get a message containing a description and arbitrary data object
             * that can be sent to a Handler.
             */
            Message msg = Message.obtain(null, what, result_code, 0);
            if (bundle != null) {
                msg.setData(bundle);
            }
            if (client != null) {
                client.send(msg);
            } else {
                for (int i = mClients.size() - 1; i >= 0; i--) {
                    Messenger client2 = mClients.get(i);
                    client2.send(msg);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger.send: " + e.getMessage());
        }
    }

    private void reportSignalStrength(int networkType,
                                      @Nullable String networkOperator,
                                      @NonNull SignalStrength ss) {
        Bundle bundle = new Bundle();
        bundle.putInt(BundleKeys.BUNDLE_KEY_CELLULAR_NETWORK_TYPE, networkType);
        if (networkOperator != null) {
            bundle.putString(BundleKeys.BUNDLE_KEY_CELLULAR_NETWORK_OPERATOR, networkOperator);
        }
        bundle.putParcelable(BundleKeys.BUNDLE_KEY_CELLULAR_PARCELABLE, ss);
        sendToClient(null, IpcType.MSG_CELLULAR_DATA, 0, bundle);
    }

    private void successReply(@Nullable Messenger client, int ipcType) {
        sendToClient(client, ipcType, 0, null);
    }

    private void errorReply(@Nullable Messenger client, @NonNull String errmsg) {
        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE, errmsg);
        sendToClient(client, IpcType.MSG_ERROR, -1, bundle);
    }
}
