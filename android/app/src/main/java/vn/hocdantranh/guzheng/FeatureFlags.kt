package vn.hocdantranh.guzheng

/**
 * Compile-time feature switches.
 *
 * Deliberately not remote: Google Play and Apple both treat a feature enabled from a server
 * after review as a policy risk (Apple guideline 2.3.1), and selling the course needs store
 * setup plus a new release anyway.
 */
object FeatureFlags {
    /**
     * Paid lessons (8–16, product [vn.hocdantranh.guzheng.data.billing.BillingProducts.COURSE_BASIC]).
     *
     * While false: Play Billing never connects, paid lessons read "Coming soon", and no price,
     * buy button, purchase sheet or restore row is reachable anywhere.
     *
     * To turn on: create `course_basic_1490k` (one-time product) in Play Console, set this to
     * true, bump versionCode, release. Mirror of iOS `FeatureFlags.iapEnabled`.
     */
    const val IAP_ENABLED = false
}
