package com.soomla;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import com.soomla.events.AppToBackgroundEvent;
import com.soomla.events.AppToForegroundEvent;

import java.util.List;


/**
 * This implementation is based on the great article by Steve Liles:
 * http://steveliles.github.io/is_my_android_app_currently_foreground_or_background.html
 *
 * Thanks Steve!
 */

public abstract class Foreground {

    private static Foreground instance;

    public static final String TAG = "SOOMLA " + Foreground.class.getName();

    // used to know if there's an outside operation running and the app will return to foreground soon.
    public boolean OutsideOperation = false;

    /**
     * Initializes Foreground
     *
     * @return an initialised Foreground instance
     */
    public static Foreground init() {
        if (instance == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                instance = new ForegroundNew();
            } else {
                instance = new ForegroundOld();
            }
        }
        return instance;
    }

    public abstract boolean isForeground();
    public abstract boolean isBackground();

    private static class ForegroundOld extends Foreground {

        @Override
        public boolean isForeground() {
            ActivityManager activityManager = (ActivityManager) SoomlaApp.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = SoomlaApp.getAppContext().getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isBackground() {
            return !isForeground();
        }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static class ForegroundNew extends Foreground implements Application.ActivityLifecycleCallbacks {
        private static final long CHECK_DELAY = 500;

        private boolean foreground = false, paused = true;
        private Handler handler = new Handler();
        private Runnable check;
        private int count = 0;

        public ForegroundNew() {
            SoomlaApp.instance().registerActivityLifecycleCallbacks(this);
        }

        @Override
        public boolean isForeground() {
            return foreground;
        }

        @Override
        public boolean isBackground() {
            return !foreground;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            paused = false;
            boolean wasBackground = !foreground;
            foreground = true;

            if (check != null)
                handler.removeCallbacks(check);

            if (wasBackground) {
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

            handler.postDelayed(check = new Runnable() {
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
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            count++;
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            count--;
            if (count == 0 && isForeground()) {
                SoomlaUtils.LogDebug(TAG, "destroyed weirdly");
                BusProvider.getInstance().post(new AppToBackgroundEvent());
            }
        }
    }
}