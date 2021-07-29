package com.zinspector.foregroundservice;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.os.Build;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.ReactApplication;

public class SocketReconnectReceiver extends BroadcastReceiver {
    private static final String TAG = "SocketReconnectReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Log.d(TAG, "Socket broadcast receiver on receive");
        ReactApplication rnApp = (ReactApplication) context.getApplicationContext();
        ReactContext reactContext = rnApp.getReactNativeHost().getReactInstanceManager().getCurrentReactContext();
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("RestartPTTAction", null);
        int connectionType = getConnectionType(context);
        if (connectionType > 0) {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("RestartSocketAction", null);
        }
        schedule(context);
    }

    private int getConnectionType(Context context) {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cm != null) {
                @SuppressLint("MissingPermission") NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = 2;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        result = 3;
                    }
                }
            }
        } else {
            if (cm != null) {
                @SuppressLint("MissingPermission") NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    // connected to the internet
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        result = 2;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        result = 1;
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_VPN) {
                        result = 3;
                    }
                }
            }
        }
        return result;
    }

    public static void schedule(Context context) {
        Intent intent = new Intent(context, SocketReconnectReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 11111112, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10000, pendingIntent);
    }
}