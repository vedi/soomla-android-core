/*
 * Copyright (C) 2012-2014 Soomla Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soomla.data;

import com.soomla.BusProvider;
import com.soomla.SoomlaConfig;
import com.soomla.SoomlaUtils;
import com.soomla.events.RewardGivenEvent;
import com.soomla.events.RewardTakenEvent;
import com.soomla.rewards.Reward;
import com.soomla.rewards.SequenceReward;

/**
 * A utility class for persisting and querying the state of rewards.
 * Use this class to check if a certain reward was given, or to
 * set its state.
 * This class uses the <code>KeyValueStorage</code> internally for storage.
 *
 * Created by refaelos on 13/05/14.
 */
public class RewardStorage {

    private static final String TAG = "SOOMLA RewardStorage";

    private static String keyRewards(String rewardId, String postfix) {
        return SoomlaConfig.DB_KEY_PREFIX + "rewards." + rewardId + "." + postfix;
    }

    private static String keyRewardGiven(String rewardId) {
        return keyRewards(rewardId, "given");
    }

    private static String keyRewardIdxSeqGiven(String rewardId) {
        return keyRewards(rewardId, "seq.idx");
    }


    /** Badges **/

    /**
     * Sets the reward status of the given reward\
     *
     * @param reward the reward to set status
     * @param give <code>true</code>
     */
    public static void setRewardStatus(Reward reward, boolean give) {
        setRewardStatus(reward, give, true);
    }

    public static void setRewardStatus(Reward reward, boolean give, boolean notify) {
        String rewardId = reward.getID();
        String key = keyRewardGiven(rewardId);

        // todo: also consider take + not owned (with respect to repeatable..)
        boolean giveAndOwned = give && reward.isOwned() && !reward.isRepeatable();
        if(giveAndOwned) {
            notify = false;
            SoomlaUtils.LogWarning(TAG, "non repeatable reward already given [suppress notify]:" + rewardId);
        }

        if (give) {
            KeyValueStorage.setValue(key, "yes");

            if (notify) {
                BusProvider.getInstance().post(new RewardGivenEvent(reward));
            }
        } else {
            KeyValueStorage.deleteKeyValue(key);
            if (notify) {
                BusProvider.getInstance().post(new RewardTakenEvent(reward));
            }
        }
    }

    /**
     * Checks whether the given reward was given.
     *
     * @param reward the reward to check
     * @return <code>true</code> if the reward was already given,
     * <code>false</code> otherwise
     */
    public static boolean isRewardGiven(Reward reward) {
        String rewardId = reward.getID();
        String key = keyRewardGiven(rewardId);

        String val = KeyValueStorage.getValue(key);

        return val != null;
    }


    /** Sequence Reward **/

    /**
     * Retrieves the index of the last reward given in a sequence of rewards.
     *
     * @param sequenceReward the reward to check
     * @return the index of the reward in the sequence
     */
    public static int getLastSeqIdxGiven(SequenceReward sequenceReward) {
        String rewardId = sequenceReward.getID();
        String key = keyRewardIdxSeqGiven(rewardId);

        String val = KeyValueStorage.getValue(key);

        if (val == null) {
            return -1;
        }
        return Integer.parseInt(val);
    }

    /**
     * Sets the index of the last reward given in a sequence of rewards.
     *
     * @param sequenceReward the reward who's index is to be set
     * @param idx the index to set
     */
    public static void setLastSeqIdxGiven(SequenceReward sequenceReward, int idx) {
        String rewardId = sequenceReward.getID();
        String key = keyRewardIdxSeqGiven(rewardId);

        KeyValueStorage.setValue(key, String.valueOf(idx));
    }
}
