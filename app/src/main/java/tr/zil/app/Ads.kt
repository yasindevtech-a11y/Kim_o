package tr.zil.app

import android.app.Activity
import android.content.Context
import android.widget.LinearLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Ana ekranın altındaki banner reklam. Önce kullanıcı onayı (Google UMP) alınır,
 * onay gerekmiyorsa ya da verildiyse reklam yüklenir. Hata olursa reklam sessizce atlanır;
 * uygulamanın geri kalanı etkilenmez. Uyarı/zil ekranında reklam gösterilmez.
 */
object Ads {

    fun setup(activity: Activity, container: LinearLayout) {
        try {
            val info = UserMessagingPlatform.getConsentInformation(activity)
            val params = ConsentRequestParameters.Builder().build()
            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                        if (info.canRequestAds()) show(activity, container)
                    }
                },
                { _ ->
                    if (info.canRequestAds()) show(activity, container)
                }
            )
            // Önceki oturumda alınan onay varsa beklemeden başla
            if (info.canRequestAds()) show(activity, container)
        } catch (_: Throwable) {
        }
    }

    private fun show(activity: Activity, container: LinearLayout) {
        activity.runOnUiThread {
            try {
                if (container.childCount > 0 || activity.isFinishing) return@runOnUiThread
                MobileAds.initialize(activity) { }
                val ad = AdView(activity)
                ad.setAdSize(AdSize.BANNER)
                ad.adUnitId = BuildConfig.ADMOB_BANNER_ID
                container.addView(ad)
                ad.loadAd(AdRequest.Builder().build())
            } catch (_: Throwable) {
            }
        }
    }

    fun destroy(container: LinearLayout) {
        try {
            for (i in 0 until container.childCount) {
                (container.getChildAt(i) as? AdView)?.destroy()
            }
            container.removeAllViews()
        } catch (_: Throwable) {
        }
    }

    /** AB/İngiltere gibi bölgelerde "Reklam gizlilik seçenekleri" gösterilmesi gerekir. */
    fun privacyOptionsRequired(c: Context): Boolean = try {
        UserMessagingPlatform.getConsentInformation(c).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    } catch (_: Throwable) {
        false
    }

    fun showPrivacyOptions(activity: Activity) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
        } catch (_: Throwable) {
        }
    }
}
