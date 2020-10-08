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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;


public class SensorController {
    private final static String TAG = SensorController.class.getSimpleName();

    /**
     * Messenger for communicating with the service.
     */
    private Messenger mService = null;

    private boolean mIsBound = false;
    private final SensorListener mListener;
    private final Context mContext;
    private final int mClientId;

    public SensorController(Context context, int clientId) {
        if (context instanceof SensorListener) {
            this.mContext = context;
            this.mListener = (SensorListener)context;
            this.mClientId = clientId;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SensorListener");
        }
    }

    /**
     * API functions to control SensorService.
     */
    /*
     * XXX: NOTYET: Do we need start/stop Service functions?
     *
    public void startSensorService() {
        mContext.startService(new Intent(mContext, SensorService.class));
    }

    public void stopSensorService() {
        mContext.stopService(new Intent(mContext, SensorService.class));
    }
    */

    public void bindSensorService() {
        // Establish a connection with the service. We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        if (mIsBound) {
            Log.w(TAG, "bindSensorService: Already bound to Service");
        } else {
            try {
                mIsBound = mContext.bindService(
                        new Intent(mContext, SensorService.class),
                        mConnection,
                        Context.BIND_AUTO_CREATE);
                if (!mIsBound) {
                    Log.w(TAG, "bindSensorService: bindService failed?");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "bindSensorService: " + e.toString());
            }
        }
    }

    public void unbindSensorService() {
        Log.d(TAG, "unbindSensorService");

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
                Log.w(TAG, "unbindSensorService: " + e.toString());
            }

            // Detach out existing connection.
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }

    public void getAvailableSensorTypes() {
        if (mIsBound) {
            Message msg = Message.obtain(
                    null, IpcType.MSG_LIST_SENSOR_TYPES, 0, mClientId);
            /* No bundled data for this message */
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Messenger.send: " + e.toString());
            }
        } else {
            Log.w(TAG, "Service not yet bound");
        }
    }

    public void enableSensors(@NonNull ArrayList<Integer> sensorTypes) {
        if (mIsBound) {
            Message msg = Message.obtain(
                    null, IpcType.MSG_ENABLE_SENSORS, 0, mClientId);
            Bundle bundle = new Bundle();
            bundle.putIntegerArrayList(BundleKeys.BUNDLE_KEY_SENSOR_TYPES, sensorTypes);
            msg.setData(bundle);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Messenger.send: " + e.toString());
            }
        } else {
            Log.w(TAG, "Service not yet bound");
        }
    }

    public void disableSensors(@NonNull ArrayList<Integer> sensorTypes) {
        if (mIsBound) {
            Message msg = Message.obtain(
                    null, IpcType.MSG_DISABLE_SENSORS, 0, mClientId);
            Bundle bundle = new Bundle();
            bundle.putIntegerArrayList(BundleKeys.BUNDLE_KEY_SENSOR_TYPES, sensorTypes);
            msg.setData(bundle);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Messenger.send: " + e.toString());
            }
        } else {
            Log.w(TAG, "Service not yet bound");
        }
    }

    public void setIntervalTimer(long seconds) {
        /*
         * SensorEvent.timestamp is set in nanoseconds.
         * To prevent overload, we handle interval timer in seconds.
         */
        if (seconds <= 0L || (Long.MAX_VALUE / 1000 * 1000) < seconds) {
            mListener.onError("IntervalTimer(" + seconds + ") out of range");
            return;
        }
        if (mIsBound) {
            Message msg = Message.obtain(
                    null, IpcType.MSG_SET_INTERVAL_TIMER, 0, mClientId);
            Bundle bundle = new Bundle();
            bundle.putLong(BundleKeys.BUNDLE_KEY_INTERVAL_TIMER, seconds);
            msg.setData(bundle);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Messenger.send: " + e.toString());
            }
        } else {
            Log.w(TAG, "Service not yet bound");
        }
    }

    public void setLocation(float longitude, float latitude) {
        if ((longitude < -180.0 || 180.0 < longitude)
                || (latitude < -90 || 90.0 < latitude)) {
            mListener.onError("Location{" + longitude +
                    ", " + latitude + "} out of range");
            return;
        }
        if (mIsBound) {
            Message msg = Message.obtain(
                    null, IpcType.MSG_SET_LOCATION, 0, mClientId);
            Bundle bundle = new Bundle();
            bundle.putFloat(BundleKeys.BUNDLE_KEY_LOCATION_LONGITUDE, longitude);
            bundle.putFloat(BundleKeys.BUNDLE_KEY_LOCATION_LATITUDE, latitude);
            msg.setData(bundle);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Messenger.send: " + e.toString());
            }
        } else {
            Log.w(TAG, "Service not yet bound");
        }
    }

    public void setUserData(@Nullable String publisher, @Nullable String note) {
        if (mIsBound) {
            Message msg = Message.obtain(
                    null, IpcType.MSG_SET_USER_DATA, 0, mClientId);
            Bundle bundle = new Bundle();
            if (publisher != null) {
                bundle.putString(BundleKeys.BUNDLE_KEY_USERINFO_PUBLISHER, publisher);
            }
            if (note != null) {
                bundle.putString(BundleKeys.BUNDLE_KEY_USERINFO_NOTE, note);
            }
            msg.setData(bundle);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.w(TAG, "Messenger.send: " + e.toString());
            }
        } else {
            Log.w(TAG, "Service not yet bound");
        }
    }

    /**
     * IPC endpoint to send messages to Service.
     */
    private final Messenger mMessenger =
            new Messenger(new IncomingHandler(this));

    /**
     * Handler for incoming messages from Service.
     */
    private static class IncomingHandler extends Handler {
        final WeakReference<SensorController> weakReference;

        IncomingHandler(SensorController sensorController) {
            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(sensorController);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg A Message object containing a bundle of data
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            SensorController sensorController = weakReference.get();
            if (sensorController != null) {
                sensorController.onServerMessageReceived(msg);
            } else {
                Log.w(TAG, "handleMessage: SensorController has gone");
            }
            super.handleMessage(msg);
        }
    }

    private void onServerMessageReceived(@NonNull Message msg) {
        int result_code = msg.arg1;
        Bundle bundle = msg.getData();

        switch (msg.what) {
            case IpcType.MSG_LIST_SENSOR_TYPES:
                if (bundle != null) {
                    ArrayList<Integer> sensorTypes =
                            bundle.getIntegerArrayList(BundleKeys.BUNDLE_KEY_SENSOR_TYPES);
                    if (sensorTypes != null) {
                        mListener.onSensorTypesReceived(sensorTypes);
                    } else {
                        Log.w(TAG, "Null sensorTypes?");
                    }
                } else {
                    Log.w(TAG, "MSG_LIST_SENSOR_TYPES: No bundle?");
                }
                break;
            case IpcType.MSG_SENSOR_DATA:
                if (bundle != null) {
                    String sensorData = bundle.getString(BundleKeys.BUNDLE_KEY_SENSOR_VALUES);
                    if (sensorData != null) {
                        mListener.onSensorDataReceived(sensorData);
                    } else {
                        Log.w(TAG, "MSG_SENSOR_DATA: Invalid bundle: " + bundle.toString());
                    }
                } else {
                    Log.w(TAG, "MSG_SENSOR_DATA: No bundle?");
                }
                break;
            case IpcType.MSG_SET_INTERVAL_TIMER:
            case IpcType.MSG_SET_LOCATION:
            case IpcType.MSG_SET_USER_DATA:
                /* These cases are meant to be an ACK */
                if (result_code != 0) {
                    /* Any error will be notified by IpcType.MSG_ERROR, actually */
                    Log.w(TAG, "IpcType(" + msg.what + "): Failed to set value");
                }
                break;
            case IpcType.MSG_ERROR:
                if (bundle != null) {
                    String errmsg = bundle.getString(BundleKeys.BUNDLE_KEY_PARCELABLE);
                    if (errmsg != null) {
                        mListener.onError(errmsg);
                    } else {
                        Log.w(TAG, "MSG_ERROR: Invalid bundle: " + bundle.toString());
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
                Log.e(TAG, "ServiceConnection.onServiceConnected: " + e.toString());
                return;
            }

            mIsBound = true;
            mListener.onSensorEngaged(
                    mContext.getString(R.string.remote_service_connected));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d(TAG, "ServiceConnection.onServiceDisconnected: " + name.toString());
            mService = null;

            mIsBound = false;
            mListener.onSensorDisengaged(
                    mContext.getString(R.string.remote_service_disconnected));
        }
    };
}
