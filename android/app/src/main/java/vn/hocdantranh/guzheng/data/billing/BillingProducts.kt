package vn.hocdantranh.guzheng.data.billing

/** Play Console product IDs. Same IDs as the iOS App Store products. */
object BillingProducts {
    /** One-time purchase unlocking lessons 8–16. */
    const val COURSE_BASIC = "course_basic_1490k"

    /** Every product that grants the course. */
    val COURSE_GRANTING: Set<String> = setOf(COURSE_BASIC)
}
