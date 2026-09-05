package vn.hocdantranh.guzheng.data.sheet

/**
 * One sheet-music document. `pages` holds the raw Firestore URLs — call
 * [directImageUrl] at the point of display rather than rewriting here, so the cached
 * payload stays byte-identical to what Firestore served.
 */
data class SheetItem(
    val id: String,
    val title: String,
    val pages: List<String>,
    val isFree: Boolean,
    val order: Int,
)

/**
 * Maps one Firestore document's fields to a [SheetItem], or null if it should not render.
 *
 * Null rather than throwing, mirroring the iOS `compactMap`: one malformed document must
 * not empty the whole tab. `isVisible` is applied here and then dropped from the model, so
 * nothing downstream re-checks it.
 *
 * `order` is read through `Number` because Firestore returns integers as `Long` on
 * Android — the iOS `as? Int` spelling would drop all 19 live documents.
 */
fun sheetFromFields(id: String, fields: Map<String, Any?>): SheetItem? {
    val isVisible = fields["isVisible"] as? Boolean ?: true
    if (!isVisible) return null

    val title = fields["title"] as? String ?: return null
    val rawPages = fields["pages"] as? List<*> ?: return null
    val pages = rawPages.filterIsInstance<String>()
    if (pages.size != rawPages.size) return null
    val order = (fields["order"] as? Number)?.toInt() ?: return null

    return SheetItem(
        id = id,
        title = title,
        pages = pages,
        isFree = fields["isFree"] as? Boolean ?: true,
        order = order,
    )
}
