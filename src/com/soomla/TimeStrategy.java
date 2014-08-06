package com.soomla;

import com.soomla.data.JSONConsts;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by refaelos on 05/08/14.
 */
public class TimeStrategy {
    private static String TAG = "SOOMLA TimeStrategy";

    public enum Strategy {
        EVERYMONTH,
        EVERYDAY,
        EVERYHOUR,
        CUSTOM,
        ALWAYS
    }



    public static TimeStrategy Once() {
        return Once(null);
    }

    public static TimeStrategy Once(Date startTime) {
        return Custom(startTime, 1);
    }

    public static TimeStrategy EveryMonth(Date startTime, int repeatTimes) {
        return new TimeStrategy(Strategy.EVERYMONTH, startTime, repeatTimes);
    }

    public static TimeStrategy EveryDay(Date startTime, int repeatTimes) {
        return new TimeStrategy(Strategy.EVERYDAY, startTime, repeatTimes);
    }

    public static TimeStrategy EveryHour(Date startTime, int repeatTimes) {
        return new TimeStrategy(Strategy.EVERYHOUR, startTime, repeatTimes);
    }

    public static TimeStrategy Custom(int repeatTimes) {
        return Custom(null, repeatTimes);
    }

    public static TimeStrategy Custom(Date startTime, int repeatTimes) {
        return new TimeStrategy(Strategy.CUSTOM, startTime, repeatTimes);
    }

    public static TimeStrategy Always() {
        return Always(null);
    }

    public static TimeStrategy Always(Date startTime) {
        return new TimeStrategy(Strategy.ALWAYS, startTime, 0);
    }

    private TimeStrategy(Strategy strategy, Date startTime, int repeatTimes)
    {
        mRequiredStrategy = strategy;
        mStartTime = startTime;
        mRepeatTimes = repeatTimes;
    }

    public TimeStrategy(JSONObject jsonTs) throws JSONException {
        mRequiredStrategy = Strategy.values()[jsonTs.getInt(JSONConsts.SOOM_TS_KIND)];
        mRepeatTimes = jsonTs.getInt(JSONConsts.SOOM_TS_REPEAT);
        long startTimeMillis = jsonTs.optLong(JSONConsts.SOOM_TS_START);
        mStartTime = new Date();
        mStartTime.setTime(startTimeMillis);
    }

    public JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JSONConsts.SOOM_TS_KIND, mRequiredStrategy.ordinal());
            jsonObject.put(JSONConsts.SOOM_TS_REPEAT, mRepeatTimes);
            jsonObject.put(JSONConsts.SOOM_TS_START, mStartTime.getTime());
        } catch (JSONException e) {
            SoomlaUtils.LogError(TAG, "An error occurred while generating JSON object.");
        }

        return jsonObject;
    }

    public boolean approve(Date lastTime, int timesApproved) {

        Date now = new Date();

        if (mStartTime != null && now.before(mStartTime)) {
            SoomlaUtils.LogDebug(TAG, "Time to start approval hasn't come yet.");
            return false;
        }

        if (mRequiredStrategy == Strategy.ALWAYS) {
            SoomlaUtils.LogDebug(TAG, "The strategy is ALWAYS.");
            return true;
        }

        if (mRepeatTimes>0 && timesApproved >= mRepeatTimes) {
            SoomlaUtils.LogDebug(TAG, "Approval limit exceeded.");
            return false;
        }

        if (mRequiredStrategy == Strategy.CUSTOM) {
            SoomlaUtils.LogDebug(TAG, "The strategy is CUSTOM and approval limit not reached.");
            return true;
        }

        if (mStartTime == null) {
            SoomlaUtils.LogError(TAG, "The strategy is related to times but StartTime is null.");
            return false;
        }

        if (lastTime == null) {
            SoomlaUtils.LogError(TAG, "The strategy is related to times and we didn't get a valid lastTime. This means that it's the first time.");
            return true;
        }

        if (mRequiredStrategy == Strategy.EVERYHOUR) {
            SoomlaUtils.LogDebug(TAG, "The strategy is EVERYHOUR.");
            long diffSeconds = (now.getTime() - lastTime.getTime()) / 1000;
            long hours = diffSeconds / 3600;
            return hours >= 1;
        }

        if (mRequiredStrategy == Strategy.EVERYDAY) {
            SoomlaUtils.LogDebug(TAG, "The strategy is EVERYDAY.");
            long diffSeconds = (now.getTime() - lastTime.getTime()) / 1000;
            long days = (diffSeconds / 3600) / 24;
            return days >= 1;
        }

        if (mRequiredStrategy == Strategy.EVERYMONTH) {
            SoomlaUtils.LogDebug(TAG, "The strategy is EVERYMONTH.");

            Calendar startCalendar = new GregorianCalendar();
            startCalendar.setTime(lastTime);
            Calendar endCalendar = new GregorianCalendar();
            endCalendar.setTime(now);

            int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
            int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

            return diffMonth >= 1;
        }

        return false;
    }

    public Strategy getRequiredStrategy() {
        return mRequiredStrategy;
    }

    public Date getStartTime() {
        return mStartTime;
    }

    public int getRepeatTimes() {
        return mRepeatTimes;
    }

    private Strategy mRequiredStrategy;
    private Date     mStartTime;
    private int      mRepeatTimes;

}
