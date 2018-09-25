package com.voipgrid.vialer.util;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.logging.Logger;

/**
 * Class to help with the disabling of the screen during a call.
 */
public class ProximitySensorHelper implements SensorEventListener, View.OnClickListener {
    private Context mContext;
    private Sensor mProximitySensor;
    private SensorManager mSensorManager;
    private Logger mLogger;
    private View mLockView;

    public ProximitySensorHelper(Context context) {
        mContext = context;
        mLogger = new Logger(ProximitySensorHelper.class);
        mLogger.v("ProximitySensorHelper");

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    public void startSensor(View lockView) {
        mLockView = lockView;
        mLogger.v("startSensor()");

        if (mProximitySensor != null) {
            mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
        toggleScreen(true);
    }

    public void stopSensor() {
        mLogger.v("stopSensor()");

        if (mProximitySensor != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mLogger.v("onSensorChanged()");
        Float distance = event.values[0];
        // Leave the screen on if the measured distance is the max distance.
        if (distance >= event.sensor.getMaximumRange() || distance >= 10.0f) {
            toggleScreen(true);
        } else {
            toggleScreen(false);
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
        mLogger.v("toggleScreen(): " + on);

        Activity activity = (Activity) mContext;
        Window window = activity.getWindow();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();

        if (on) {
            if (mLockView != null) {
                mLockView.setVisibility(View.GONE);
            }

            // Reset screen brightness.
            params.screenBrightness = -1;

            params.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

            // Remove the disable touch flag
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            // Set screen brightness to 0.
            params.screenBrightness = 0;

            mLockView.setVisibility(View.VISIBLE);

            // Disable the touch
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        window.setAttributes(params);
    }
}
