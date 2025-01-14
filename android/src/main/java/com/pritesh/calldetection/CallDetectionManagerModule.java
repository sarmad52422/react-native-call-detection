package com.pritesh.calldetection;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

public class CallDetectionManagerModule
        extends ReactContextBaseJavaModule
        implements Application.ActivityLifecycleCallbacks,
        CallDetectionPhoneStateListener.PhoneCallStateUpdate {

    private boolean wasAppInOffHook = false;
    private boolean wasAppInRinging = false;
    private ReactApplicationContext reactContext;
    private TelephonyManager telephonyManager;
    String eventType = null;

    private CallStateUpdateActionModule jsModule = null;
    private CallDetectionPhoneStateListener callDetectionPhoneStateListener;
    private Activity activity = null;

    public CallDetectionManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CallDetectionManagerAndroid";
    }

    @ReactMethod
    public void startListener() {
        if (activity == null) {
            activity = getCurrentActivity();
            activity.getApplication().registerActivityLifecycleCallbacks(this);
        }

        telephonyManager = (TelephonyManager) this.reactContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        callDetectionPhoneStateListener = new CallDetectionPhoneStateListener(this);
        telephonyManager.listen(callDetectionPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

    }

    @ReactMethod
    public void stopListener() {
        telephonyManager.listen(callDetectionPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
        telephonyManager = null;
        callDetectionPhoneStateListener = null;
    }

    /**
     * @return a map of constants this module exports to JS. Supports JSON types.
     */
    public Map<String, Object> getConstants() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Incoming", "Incoming");
        map.put("Offhook", "Offhook");
        map.put("Disconnected", "Disconnected");
        map.put("Missed", "Missed");
        return map;
    }

    // Activity Lifecycle Methods
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceType) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private void sendEvent(String eventName, String phoneNumber) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("PhoneCallStateUpdate", phoneNumber + "|" + eventName);
    }

    @Override
    public void phoneCallStateUpdated(int state, String phoneNumber) {
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                if (wasAppInOffHook) {
                    eventType = "Disconnected";
                } else if (wasAppInRinging) {
                    eventType = "Missed";
                }
                wasAppInRinging = false;
                wasAppInOffHook = false;
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                wasAppInOffHook = true;
                eventType = "Offhook";
                break;

            case TelephonyManager.CALL_STATE_RINGING:
                wasAppInRinging = true;
                eventType = "Incoming";
                break;
        }

        if (eventType != null) {
            sendEvent(eventType, phoneNumber);
        }
    }

}
