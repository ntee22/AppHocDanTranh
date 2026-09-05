package vn.hocdantranh.guzheng.data.sheet

/** Matches `/d/<id>`, stopping at the next `/` or `?`. */
private val DRIVE_PATH_ID = Regex("""/d/([^/?]+)""")

/** Matches `id=<value>` in a query string. */
private val DRIVE_QUERY_ID = Regex("""[?&]id=([^&]+)""")

/**
 * Rewrites a Google Drive sharing URL to one that serves the raw image bytes.
 *
 * Drive's `/file/d/<id>/view` links return an HTML viewer page with HTTP 200, so Coil
 * decodes markup instead of an image and the sheet renders blank. Every page URL in the
 * live collection is such a link, which makes this conversion load-bearing rather than
 * defensive. Port of the iOS `directImageURL`.
 *
 * Anything that is not a Drive URL, or is one with no extractable file id, is returned
 * unchanged — a malformed link should degrade to a broken image, never to a crash.
 */
fun directImageUrl(raw: String): String {
    if (!raw.contains("drive.google.com")) return raw

    val id = DRIVE_PATH_ID.find(raw)?.groupValues?.get(1)
        ?: DRIVE_QUERY_ID.find(raw)?.groupValues?.get(1)
        ?: return raw

    return "https://lh3.googleusercontent.com/d/$id"
}
