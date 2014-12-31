package com.soomla;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.soomla.events.AppToBackgroundEvent;
import com.soomla.events.AppToForegroundEvent;


/**
 * This implementation is based on the great article by Steve Liles:
 * http://steveliles.github.io/is_my_android_app_currently_foreground_or_background.html
 *
 * Thanks Steve!
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class Foreground implements Application.ActivityLifecycleCallbacks {

    public static final long CHECK_DELAY = 500;
    public static final String TAG = "SOOMLA " + Foreground.class.getName();

    private static Foreground instance;

    private boolean foreground = false, paused = true;
    private Handler handler = new Handler();
    private Runnable check;

    public boolean OutsideOperation = false;

    /**
     * Initializes Foreground
     *
     * @return an initialised Foreground instance
     */
    public static Foreground init(){
        if (instance == null) {
            instance = new Foreground();
            SoomlaApp.instance().registerActivityLifecycleCallbacks(instance);
        }
        return instance;
    }

    public boolean isForeground(){
        return foreground;
    }

    public boolean isBackground(){
        return !foreground;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);

        if (wasBackground){
            SoomlaUtils.LogDebug(TAG, "went foreground");
            BusProvider.getInstance().post(new AppToForegroundEvent());
        } else {
            SoomlaUtils.LogDebug(TAG, "still foreground");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);

        handler.postDelayed(check = new Runnable(){
            @Override
            public void run() {
                if (foreground && paused && !OutsideOperation) {
                    foreground = false;
                    SoomlaUtils.LogDebug(TAG, "went background");
                    BusProvider.getInstance().post(new AppToBackgroundEvent());
                } else {
                    SoomlaUtils.LogDebug(TAG, "still foreground");
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}