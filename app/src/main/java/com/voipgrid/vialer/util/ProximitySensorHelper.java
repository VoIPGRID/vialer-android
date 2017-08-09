package com.voipgrid.vialer.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.view.WindowManager;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.logging.RemoteLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Class to help with the disabling of the screen during a call.
 */
public class ProximitySensorHelper implements SensorEventListener, View.OnClickListener {
    private Context mContext;
    private PowerManager mPowerManager;
    private ProximitySensorInterface mProximityInterface;
    private Sensor mProximitySensor;
    private SensorManager mSensorManager;
    private View mLockView;
    private WakeLock mWakeLock;
    private RemoteLogger mRemoteLogger;


    public interface ProximitySensorInterface {
        boolean activateProximitySensor();
    }

    public ProximitySensorHelper(
            Context context, ProximitySensorInterface proximityInterface, View lockView
    ) {
        mContext = context;
        mProximityInterface = proximityInterface;
        mLockView = lockView;
        mRemoteLogger = new RemoteLogger(context, ProximitySensorHelper.class, 1);
        mRemoteLogger.e("ProximitySensorHelper");

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        setupWakeLock();

        if (mWakeLock == null) {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    public void startSensor() {
        mRemoteLogger.v("startSensor()");

        if (mProximitySensor != null) {
            mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
        updateWakeLock();
    }

    public void stopSensor() {
        mRemoteLogger.v("stopSensor()");

        if (mProximitySensor != null) {
            mSensorManager.unregisterListener(this);
        }

        // If the wakelock hasn't been used, it will still be null.
        if (mWakeLock != null) {
            // If a wakelock exists, make sure it is released before shutting down the helper.
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mRemoteLogger.v("onSensorChanged()");
        Float distance = event.values[0];
        // Leave the screen on if the measured distance is the max distance.
        if (mWakeLock == null) {
            if (distance >= event.sensor.getMaximumRange() || distance >= 10.0f) {
                toggleScreen(true);
            } else {
                toggleScreen(false);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onClick(View view) {
        Integer viewId = view.getId();
        switch (viewId) {
            case R.id.screen_off:
                break; // Screen is off so ignore click events
        }
    }

    /**
     * Function to set the screen to ON (visible) or OFF (dimmed and disabled).
     * @param on Whether or not the screen needs to be on or off.
     */
    private void toggleScreen(boolean on) {
        mRemoteLogger.v("toggleScreen(): " + on);

        Activity activity = (Activity) mContext;
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();

        if (on) {
            // Reset screen brightness.
            params.screenBrightness = -1;

            // Set the OFF version of the screen to gone.
            mLockView.setVisibility(View.GONE);

            // Remove the listener for the OFF screen state.
            mLockView.setOnClickListener(null);

            mLockView.setClickable(true);

            // Show status bar and navigation.
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            // Set screen brightness to 0.
            params.screenBrightness = 0;

            // Set the OFF version of the screen to visible.
            mLockView.setVisibility(View.VISIBLE);

            // Set the listener for the OFF screen stat.
            mLockView.setOnClickListener(this);

            mLockView.setClickable(false);

            mLockView.setEnabled(false);

            // Hide status bar and navigation.
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
        activity.getWindow().setAttributes(params);
    }

    /**
     * Try to setup the correct way for screen locking when the user is making a call.
     */
    private void setupWakeLock() {
        mRemoteLogger.v("setupWakeLock()");

        try {
            Boolean supportProximity;
            int proximityScreenOffWakeLock;
            Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
            proximityScreenOffWakeLock = (Integer) f.get(null);

            // After android Jelly Bean (API Level 17) there is support for the
            // isWakeLockLevelSupported function
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Method method = mPowerManager.getClass().getDeclaredMethod(
                        "isWakeLockLevelSupported",
                        int.class
                );
                supportProximity = (Boolean) method.invoke(
                        mPowerManager, proximityScreenOffWakeLock
                );
            } else {
                Method method = mPowerManager.getClass().getDeclaredMethod(
                        "getSupportedWakeLockFlags"
                );
                int supportedFlags = (Integer) method.invoke(mPowerManager);
                supportProximity = ((supportedFlags & proximityScreenOffWakeLock) != 0x0);
            }

            if (supportProximity) {
                mWakeLock = mPowerManager.newWakeLock(
                        proximityScreenOffWakeLock,
                        ProximitySensorHelper.class.getSimpleName()
                );
                mWakeLock.setReferenceCounted(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Function to manage the automatic wake lock of the phone.
     */
    public void updateWakeLock() {
        mRemoteLogger.v("updateWakeLock()");
        if (mWakeLock != null ) {
            if (mProximityInterface.activateProximitySensor()) {
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
            } else {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        }
    }
}
