package vn.hocdantranh.guzheng.ui.settings

/**
 * Every outbound destination the Settings screen offers.
 *
 * Kept as plain constants in one file so the URLs can be checked at a glance and unit
 * tested — a typo here is invisible until someone taps the row on a real phone.
 */
object SettingsLinks {
    const val FACEBOOK = "https://www.facebook.com/dantranhhcm"
    const val TIKTOK = "https://www.tiktok.com/@danthanhdantranh"
    const val YOUTUBE = "https://www.youtube.com/@danthanhdantranh"
    const val SHOPEE =
        "https://shopee.vn/danthanhdantranh?categoryId=100630&entryPoint=ShopByPDP&itemId=26115698334"
    const val EMAIL = "danthanhdantranh@gmail.com"

    /** The app's own id; both store links are built from it. */
    const val PACKAGE = "vn.hocdantranh.guzheng"

    /**
     * Opens the Play Store app directly. Resolves only once the listing is live, so the
     * caller must fall back to [playStoreWebUrl] when no activity handles it.
     */
    fun playStoreAppUrl(packageName: String = PACKAGE): String =
        "market://details?id=$packageName"

    /** Browser form of the listing, and the URL shared in the share sheet. */
    fun playStoreWebUrl(packageName: String = PACKAGE): String =
        "https://play.google.com/store/apps/details?id=$packageName"
}
