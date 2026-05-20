package com.autosort.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.autosort.data.config.ExternalConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicInteger

object AdManager {
    private const val TAG = "AdManager"
    private const val MAX_LOAD_RETRIES = 3

    @Volatile
    private var rewardedAd: RewardedAd? = null
    @Volatile
    private var isLoading = false
    private val loadRetryCount = AtomicInteger(0)

    fun isAiScanAdRequired(): Boolean {
        return ExternalConfig.AdConfig.GLOBAL_ADS_ENABLED && ExternalConfig.AdConfig.REQUIRE_AD_FOR_AI_SCAN
    }

    fun loadRewardedAd(context: Context) {
        if (!isAiScanAdRequired()) return

        synchronized(this) {
            if (rewardedAd != null || isLoading) return
            if (loadRetryCount.get() >= MAX_LOAD_RETRIES) return
            isLoading = true
        }

        val appContext = context.applicationContext
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            appContext,
            ExternalConfig.AdConfig.ID_REWARDED_AI_SCAN,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    synchronized(this@AdManager) {
                        rewardedAd = null
                        isLoading = false
                    }
                    loadRetryCount.incrementAndGet()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    synchronized(this@AdManager) {
                        rewardedAd = ad
                        isLoading = false
                    }
                    loadRetryCount.set(0)
                    
                    // Setup callback to reload ad once it is dismissed
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            synchronized(this@AdManager) {
                                rewardedAd = null
                            }
                            loadRewardedAd(appContext) // Pre-load the next one
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            synchronized(this@AdManager) {
                                rewardedAd = null
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Shows the rewarded ad if it's loaded.
     * @param activity The current activity.
     * @param onRewardEarned Callback fired when the user successfully watches the ad.
     * @param onFailed Callback fired if the ad isn't ready or fails to show.
     */
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onFailed: () -> Unit) {
        if (!isAiScanAdRequired()) {
            onRewardEarned()
            return
        }
        
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned the reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            onFailed()
            loadRetryCount.set(0) // Reset retry counter for user-initiated reload
            loadRewardedAd(activity) // Try loading it again
        }
    }
}
