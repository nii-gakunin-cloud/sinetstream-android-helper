/*
 * Copyright (C) 2020-2021 National Institute of Informatics
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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.AsyncTask;
import android.os.Build;
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

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import jp.ad.sinet.stream.android.helper.constants.BundleKeys;
import jp.ad.sinet.stream.android.helper.constants.IpcType;
import jp.ad.sinet.stream.android.helper.models.SensorHolder;
import jp.ad.sinet.stream.android.helper.provider.CellularStorage;
import jp.ad.sinet.stream.android.helper.provider.JsonBuilder;
import jp.ad.sinet.stream.android.helper.provider.JsonBuilderForCellular;
import jp.ad.sinet.stream.android.helper.provider.LocationStorage;
import jp.ad.sinet.stream.android.helper.provider.SensorStorage;
import jp.ad.sinet.stream.android.helper.provider.UserDataStorage;
import jp.ad.sinet.stream.android.helper.util.DateTimeUtil;

/**
 * As the back-end element of the SINETStreamHelper library, this class
 * takes care of {@link Sensor} management via {@link SensorManager}.
 */
public class SensorService extends Service
        implements SensorEventListener {
    private final static String TAG = SensorService.class.getSimpleName();

    /** Keep track of all current registered clients */
    private final List<Messenger> mClients = new ArrayList<>();

    private SensorManager mSensorManager = null;
    private final SensorStorage mSensorStorage = new SensorStorage();
    private final CellularStorage mCellularStorage = new CellularStorage();
    private final LocationStorage mLocationStorage = new LocationStorage();
    private final UserDataStorage mUserDataStorage = new UserDataStorage();

    private final DateTimeUtil mDateTimeUtil = new DateTimeUtil();

    /* Rate control parameters */
    private long mTimeStamp = 0;
    private long mInterval = ms2ns(1000L);

    /* Make sure ALL sensor listener gets unregistered on unbind */
    private boolean mSensorListenerActive = false;

    private final static String NOTIFICATION_CHANNEL_ID =
            TAG + ".notification_channel";

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        showNotification();
        onServiceStarted();
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        onServiceStopped();
        removeNotification();
        super.onDestroy();
    }

    /**
     * When binding to the service, we return an interface to out messenger
     * for sending messages to the service.
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return {@link IBinder} object used by {@link Messenger}.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Intent="
                + (intent != null ? intent.toString() : ""));
        return mMessenger.getBinder();
    }

    /**
     * Called when all clients have disconnected from a particular interface
     * published by the service.  The default implementation does nothing and
     * returns false.
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return true if you would like to have the service's
     * {@link #onRebind} method later called when new clients bind to it.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: Intent="
                + (intent != null ? intent.toString() : ""));

        if (mSensorListenerActive) {
            Log.w(TAG, "Forcibly disable ALL sensors");
            ArrayList<Integer> sensorTypes = mSensorStorage.getSensorTypes();
            disableSensors(sensorTypes);
        }
        return super.onUnbind(intent);
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
     * This is called if the service is currently running and the user has
     * removed a task that comes from the service's application.  If you have
     * set {@link ServiceInfo#FLAG_STOP_WITH_TASK ServiceInfo.FLAG_STOP_WITH_TASK}
     * then you will not receive this callback; instead, the service will simply
     * be stopped.
     *
     * @param rootIntent The original root Intent that was used to launch
     *                   the task that is being removed.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: Intent=" +
                (rootIntent != null ? rootIntent.toString() : ""));

        // User has swiped out the application from the recent apps list.
        // It's time to stop myself along with companion Activities.
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    private void onServiceStarted() {
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        if (mSensorManager != null) {
            List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (int i = 0, n = sensorList.size(); i < n; i++) {
                Sensor sensor = sensorList.get(i);

                String attr = "";
                attr += "[";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    attr += "id(" + sensor.getId() + "),";
                }
                attr += "type(" + sensor.getType() + "),";
                attr += "name(" + sensor.getName() + ")";
                attr += "]";
                Log.d(TAG, "Register SENSOR" + attr);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int reportingMode = sensor.getReportingMode();
                    String modeName = "Unknown";
                    switch (reportingMode) {
                        case Sensor.REPORTING_MODE_CONTINUOUS:
                            modeName = "CONTINUOUS";
                            break;
                        case Sensor.REPORTING_MODE_ON_CHANGE:
                            modeName = "ON_CHANGE";
                            break;
                        case Sensor.REPORTING_MODE_ONE_SHOT:
                            modeName = "ONE_SHOT";
                            break;
                        case Sensor.REPORTING_MODE_SPECIAL_TRIGGER:
                            /* Step detectors, etc. */
                            modeName = "SPECIAL_TRIGGER";
                            break;
                        default:
                            break;
                    }
                    Log.d(TAG, "ReportingMode(" + reportingMode + "): " + modeName);
                }

                if (sensor.getType() >= Sensor.TYPE_DEVICE_PRIVATE_BASE) {
                    /* We don't know how to handle this sensor. */
                    Log.d(TAG, "Skip device private sensor" +
                            "[type(" + sensor.getType() +
                            "),name(" + sensor.getName() + ")]");
                    continue;
                }

                mSensorStorage.registerSensor(sensor);
            }
        } else {
            Log.w(TAG, "SENSOR_SERVICE unavailable?");
        }
    }

    private void onServiceStopped() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        NotificationManager nm =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence text = getText(R.string.sensor_service_running);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.btn_star)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.service_name_sensor))
                .setContentText(text)
                .setPriority(Notification.PRIORITY_DEFAULT);

        /*
         * Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel.
         * https://developer.android.com/training/notify-user/channels
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;

        int NOTIFICATION_ID = R.string.service_name_sensor;
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String name = getString(R.string.service_name_sensor);
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel notificationChannel =
                        new NotificationChannel(
                                NOTIFICATION_CHANNEL_ID, name, importance);

                nm.createNotificationChannel(notificationChannel);
            }
            nm.notify(NOTIFICATION_ID, notification);
        }
    }

    private void removeNotification() {
        NotificationManager nm =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        int NOTIFICATION_ID = R.string.service_name_sensor;
        if (nm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
            }
            nm.cancel(NOTIFICATION_ID);
        }
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
        final WeakReference<SensorService> weakReference;

        /*
         * Default constructor in android.os.Handler is deprecated.
         * We need to use an Executor or specify the Looper explicitly.
         */
        IncomingHandler(
                @NonNull Looper looper, @NonNull SensorService sensorService) {
            super(looper);

            /* Keep the enclosing class object as weak-reference to prevent leaks */
            weakReference = new WeakReference<>(sensorService);
        }

        /**
         * Subclasses must implement this to receive messages.
         *
         * @param msg The {@link Message} object sent from a client.
         */
        @Override
        public void handleMessage(@NonNull Message msg) {
            SensorService sensorService = weakReference.get();
            if (sensorService != null) {
                sensorService.onClientMessageReceived(msg);
            } else {
                Log.w(TAG, "handleMessage: SensorService has gone");
            }
            super.handleMessage(msg);
        }
    }

    private void onClientMessageReceived(Message msg) {
        Bundle bundle_req = msg.getData();
        Bundle bundle_rsp;
        int result_code = -1; /* Error if non-zero */

        switch (msg.what) {
            case IpcType.MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case IpcType.MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case IpcType.MSG_SET_INTERVAL_TIMER:
                if (bundle_req != null) {
                    long milliseconds = bundle_req.getLong(
                            BundleKeys.BUNDLE_KEY_INTERVAL_TIMER, -1L);
                    if (milliseconds > 0L) {
                        Log.d(TAG, "Set interval timer to " + milliseconds + " (milliseconds)");
                        mInterval = ms2ns(milliseconds);
                        result_code = 0;
                    } else {
                        errorReply(msg.replyTo, "Invalid interval timer: " + milliseconds);
                    }
                } else {
                    errorReply(msg.replyTo, "INTERVAL_TIMER: Bundle data is missing?");
                }
                if (result_code != 0) {
                    /* ErrorReply has sent; avoid calling sendToClient() again */
                    break;
                }

                /* Send back process result */
                sendToClient(msg.replyTo, msg.what, result_code, null);
                break;
            case IpcType.MSG_CELLULAR_DATA:
                if (bundle_req != null) {
                    mCellularStorage.setCellularStorage(bundle_req);
                    result_code = 0;
                } else {
                    errorReply(msg.replyTo, "CELLULAR: Bundle data is missing?");
                    break;
                }

                /* Send back process result */
                sendToClient(msg.replyTo, msg.what, result_code, null);
                break;
            case IpcType.MSG_SET_LOCATION:
                if (bundle_req != null) {
                    double latitude = bundle_req.getDouble(
                            BundleKeys.BUNDLE_KEY_LOCATION_LATITUDE, Double.NaN);
                    double longitude = bundle_req.getDouble(
                            BundleKeys.BUNDLE_KEY_LOCATION_LONGITUDE, Double.NaN);
                    long utcTime = bundle_req.getLong(
                            BundleKeys.BUNDLE_KEY_LOCATION_TIMESTAMP, -1);
                    if (! Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                        Log.d(TAG, "Set location {" + latitude + ", " + longitude + "}");
                        mLocationStorage.setLocation(latitude, longitude, utcTime);
                        result_code = 0;
                    }
                } else {
                    errorReply(msg.replyTo, "LOCATION: Bundle data is missing?");
                    break;
                }

                /* Send back process result */
                sendToClient(msg.replyTo, msg.what, result_code, null);
                break;
            case IpcType.MSG_RESET_LOCATION:
                mLocationStorage.resetLocation();
                result_code = 0;

                /* Send back process result */
                sendToClient(msg.replyTo, msg.what, result_code, null);
                break;
            case IpcType.MSG_SET_USER_DATA:
                if (bundle_req != null) {
                    String publisher = bundle_req.getString(
                            BundleKeys.BUNDLE_KEY_USERINFO_PUBLISHER, null);
                    String note = bundle_req.getString(
                            BundleKeys.BUNDLE_KEY_USERINFO_NOTE, null);
                    if (publisher != null) {
                        Log.d(TAG, "Set publisher(" + publisher + ")");
                        mUserDataStorage.setPublisher(publisher);
                    }
                    if (note != null) {
                        Log.d(TAG, "Set note(" + note + ")");
                        mUserDataStorage.setNote(note);
                    }
                    /* Allow even if both publisher and note are omitted */
                    result_code = 0;
                } else {
                    errorReply(msg.replyTo, "USER_DATA: Bundle data is missing?");
                }
                if (result_code != 0) {
                    /* ErrorReply has sent; avoid calling sendToClient() again */
                    break;
                }

                /* Send back process result */
                sendToClient(msg.replyTo, msg.what, result_code, null);
                break;
            case IpcType.MSG_LIST_SENSOR_TYPES:
                /* User may have denied runtime permissions for some sensor types */
                if (bundle_req != null) {
                    ArrayList<Integer> sensorTypes =
                            bundle_req.getIntegerArrayList(BundleKeys.BUNDLE_KEY_SENSOR_TYPES);
                    if (sensorTypes != null) {
                        Log.d(TAG, "Going to EXCLUDE some sensor types");
                        excludeSensors(msg, sensorTypes);
                    }
                }
                /* Send back available sensor types */
                ArrayList<Integer> availableSensorTypes =
                        mSensorStorage.getSensorTypes();
                ArrayList<String> sensorTypeNames =
                        mSensorStorage.getSensorTypeNames(availableSensorTypes);

                bundle_rsp = new Bundle();
                bundle_rsp.putIntegerArrayList(
                        BundleKeys.BUNDLE_KEY_SENSOR_TYPES, availableSensorTypes);
                bundle_rsp.putStringArrayList(
                        BundleKeys.BUNDLE_KEY_SENSOR_TYPE_NAMES, sensorTypeNames);
                result_code = 0;

                sendToClient(msg.replyTo, msg.what, result_code, bundle_rsp);
                break;
            case IpcType.MSG_ENABLE_SENSORS:
                if (bundle_req != null) {
                    ArrayList<Integer> sensorTypes =
                            bundle_req.getIntegerArrayList(BundleKeys.BUNDLE_KEY_SENSOR_TYPES);
                    if (sensorTypes == null) {
                        Log.d(TAG, "Going to enable ALL sensor types");
                        sensorTypes = mSensorStorage.getSensorTypes();
                    }
                    enableSensors(msg, sensorTypes);
                } else {
                    errorReply(msg.replyTo, "SENSOR_TYPES: Bundle data is missing?");
                }
                break;
            case IpcType.MSG_DISABLE_SENSORS:
                if (bundle_req != null) {
                    ArrayList<Integer> sensorTypes =
                            bundle_req.getIntegerArrayList(BundleKeys.BUNDLE_KEY_SENSOR_TYPES);
                    if (sensorTypes == null) {
                        Log.d(TAG, "Going to disable ALL sensor types");
                        sensorTypes = mSensorStorage.getSensorTypes();
                    }
                    disableSensors(sensorTypes);
                } else {
                    errorReply(msg.replyTo, "SENSOR_TYPES: Bundle data is missing?");
                }
            default:
                break;
        }
    }

    private void excludeSensors(Message msg, ArrayList<Integer> sensorTypes) {
        if (mSensorListenerActive) {
            Log.w(TAG, "ExcludeSensors: Invalid calling sequence");
            return;
        }
        for (int i = 0, n = sensorTypes.size(); i < n; i++) {
            int sensorType = sensorTypes.get(i);

            Sensor sensor = mSensorStorage.lookupSensor(sensorType);
            if (sensor != null) {
                Log.d(TAG, "XXX: " + "[" + (i+1) + "/" + n + "]" +
                        "Going to EXCLUDE: " + sensor.getName());

                mSensorStorage.unregisterSensor(sensor);
            } else {
                Log.w(TAG, "Unsupported sensor type: " + sensorType);
            }
        }
    }

    private void enableSensors(Message msg, ArrayList<Integer> sensorTypes) {
        for (int i = 0, n = sensorTypes.size(); i < n; i++) {
            int sensorType = sensorTypes.get(i);
            String typeName = mSensorStorage.getSensorTypeName(sensorType);

            Sensor sensor = mSensorStorage.lookupSensor(sensorType);
            if (sensor != null) {
                Log.d(TAG, "XXX: " + "[" + (i+1) + "/" + n + "]" +
                        "Going to enable: " + sensor.getName());

                if (isOneshot(sensor)) {
                    try {
                        if (! mSensorManager.requestTriggerSensor(
                                mTriggerEventListener, sensor)) {
                            errorReply(msg.replyTo, TAG +
                                    ": requestTriggerSensor(" + typeName + "): FAILED?");
                            break;
                        }
                    } catch (IllegalArgumentException e) {
                        errorReply(msg.replyTo, TAG +
                                ": requestTriggerSensor(" + typeName + "): " +
                                e);
                        break;
                    }
                } else {
                    if (! mSensorManager.registerListener(
                            this, sensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                        errorReply(msg.replyTo, TAG +
                                ": registerListener(" + typeName + "): FAILED?");
                        break;
                    }
                }
                mSensorListenerActive = true;
            } else {
                Log.w(TAG, "Unsupported sensor type: " + sensorType);
            }
        }
    }

    private void disableSensors(ArrayList<Integer> sensorTypes) {
        for (int i = 0, n = sensorTypes.size(); i < n; i++) {
            int sensorType = sensorTypes.get(i);
            String typeName = mSensorStorage.getSensorTypeName(sensorType);

            Sensor sensor = mSensorStorage.lookupSensor(sensorType);
            if (sensor != null) {
                Log.d(TAG, "XXX: " + "[" + (i+1) + "/" + n + "]" +
                        "Going to disable: " + sensor.getName());

                if (isOneshot(sensor)) {
                    try {
                        if (! mSensorManager.cancelTriggerSensor(
                                mTriggerEventListener, sensor)) {
                            Log.w(TAG, "cancelTriggerSensor(" + typeName + "): FAILED?");
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "cancelTriggerSensor(" + typeName + "): " +
                                e);
                    }
                } else {
                    mSensorManager.unregisterListener(this, sensor);
                }
                mSensorListenerActive = false;
            } else {
                Log.w(TAG, "Unsupported sensor type: " + sensorType);
            }
        }
    }

    private boolean isOneshot(Sensor sensor) {
        boolean result = false;
        if (android.os.Build.VERSION.SDK_INT
                >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int reportingMode = sensor.getReportingMode();
            switch (reportingMode) {
                case Sensor.REPORTING_MODE_ONE_SHOT:
                    result = true;
                    break;
                case Sensor.REPORTING_MODE_CONTINUOUS:
                case Sensor.REPORTING_MODE_ON_CHANGE:
                case Sensor.REPORTING_MODE_SPECIAL_TRIGGER:
                default:
                    break;
            }
        } else {
            /*
             * Maybe the best bet without Sensor.getReportingMode().
             */
            result = (sensor.getType() == Sensor.TYPE_SIGNIFICANT_MOTION);
        }
        return result;
    }

    /**
     * Called when there is a new sensor event.  Note that "on changed"
     * is somewhat of a misnomer, as this will also be called if we have a
     * new reading from a sensor with the exact same sensor values (but a
     * newer timestamp).
     *
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     *
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Log.d(TAG, "onSensorChanged: " + event.toString());

        /*
         * Though SensorEvent.timestamp has the same time base
         * as SystemClock.elapsedRealTimeNanos(), it's not suited
         * to convert to the wall time being used in Unix system.
         */
        long unixTime = mDateTimeUtil.getUnixTime();

        /* Keep new value */
        mSensorStorage.setSensorEvent(event, unixTime);

        /* Rate control */
        if (event.timestamp - mTimeStamp >= mInterval) {
            exportSensorValues();
            mTimeStamp = event.timestamp;
        }
    }

    /**
     * Called when the accuracy of the registered sensor has changed.  Unlike
     * onSensorChanged(), this is only called when this accuracy value changes.
     *
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor The target {@link Sensor} object of interest.
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged: sensor(" + sensor.getName() +
                "),accuracy(" + accuracy + ")");
    }

    private final TriggerEventListener mTriggerEventListener =
            new TriggerEventListener() {
        /**
         * The method that will be called when the sensor
         * is triggered. Override this method in your implementation
         * of this class.
         *
         * @param event The details of the event.
         */
        @Override
        public void onTrigger(TriggerEvent event) {
            Sensor sensor = event.sensor;
            long timestamp = event.timestamp;
            float[] values = event.values;

            Log.d(TAG, "onTrigger: SENSOR[" +
                    "name(" + sensor.getName() + ")" +
                    ",timestamp(" + timestamp + ")" +
                    "]");

            for (int i = 0, n = values.length; i < n; i++) {
                float value = values[i];
                Log.d(TAG, "VALUE[" + (i+1) + '/' + n + "]: " + value);
            }
        }
    };

    private void exportSensorValues() {
        String publisher = mUserDataStorage.getPublisher(); // "user1@example.com";
        String note = mUserDataStorage.getNote();
        double latitude = mLocationStorage.getLatitude(); // (double) 139.767125;
        double longitude = mLocationStorage.getLongitude(); // (double) 35.681236;
        long utcTime = mLocationStorage.getUtcTime();
        JsonBuilder jsonBuilder =
                new JsonBuilder(publisher, note, latitude, longitude, utcTime);

        SignalStrength ss = mCellularStorage.getSignalStrength();
        if (ss != null) {
            int networkType = mCellularStorage.getNetworkType();
            long timestamp = mCellularStorage.getTimestamp();
            JsonBuilderForCellular jsonBuilder2 = new JsonBuilderForCellular(
                    new JsonBuilderForCellular.JsonBuilderForCellularListener() {
                        @Override
                        public void onJsonObject(@NonNull JSONObject jsonObject) {
                            jsonBuilder.addExtraCellularData(jsonObject);
                            exportJsonString(jsonBuilder);
                        }

                        @Override
                        public void onError(@NonNull String description) {
                            Log.e(TAG, description);
                            exportJsonString(jsonBuilder);
                        }
                    });

            jsonBuilder2.build(networkType, ss, timestamp);
            return;
        }

        exportJsonString(jsonBuilder);
    }

    private void exportJsonString(@NonNull JsonBuilder jsonBuilder) {
        ArrayList<SensorHolder> sensorHolders =
                mSensorStorage.getSensorHolders();

        String jsonString = jsonBuilder.buildJsonString(sensorHolders);
        if (jsonString != null) {
            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.BUNDLE_KEY_SENSOR_VALUES, jsonString);
            sendToClients(IpcType.MSG_SENSOR_DATA, 0, bundle);
        } else {
            Log.w(TAG, "CANNOT BUILD JSON...");
        }
        mSensorStorage.clearSensorEvent();
    }

    private void sendToClients(int what, int result_code, Bundle bundle) {
        if (mClients.size() > 0) {
            for (int i = mClients.size() - 1; i >= 0; i--) {
                Messenger client = mClients.get(i);
                sendToClient(client, what, result_code, bundle);
            }
        }
    }

    private void sendToClient(Messenger client, int what, int result_code, Bundle bundle) {
        try {
            /*
             * Get a message containing a description and arbitrary data object
             * that can be sent to a Handler.
             */
            Message msg = Message.obtain(null, what, result_code, 0);
            if (bundle != null) {
                msg.setData(bundle);
            }
            client.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Messenger.send: " + e);
        }
    }

    private void errorReply(Messenger replyTo, String errmsg) {
        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE, errmsg);
        sendToClient(replyTo, IpcType.MSG_ERROR, -1, bundle);
    }

    private long ms2ns(long milliseconds) {
        /* Milliseconds -> Nanoseconds */
        return milliseconds * 1000 * 1000;
    }
}
