package vn.hocdantranh.guzheng.data.sheet

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import vn.hocdantranh.guzheng.data.prefs.dataStore

/**
 * Key name matches the iOS `cached_sheets_v1` UserDefaults key. Bump the suffix if
 * [SheetItem] ever changes shape — a stale payload then reads as a miss rather than
 * decoding into something wrong.
 */
private val CACHED_SHEETS_KEY = stringPreferencesKey("cached_sheets_v1")

/** `org.json` rather than kotlinx.serialization — five flat fields do not justify a plugin. */
fun encodeSheets(items: List<SheetItem>): String {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("pages", JSONArray(item.pages))
                .put("isFree", item.isFree)
                .put("order", item.order)
        )
    }
    return array.toString()
}

/**
 * Returns an empty list for anything it cannot read — malformed JSON, or an entry missing
 * a field. A cache is an optimisation; a corrupt one must degrade to a network fetch, never
 * take the tab down.
 */
fun decodeSheets(json: String): List<SheetItem> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        val pagesArray = obj.getJSONArray("pages")
        SheetItem(
            id = obj.getString("id"),
            title = obj.getString("title"),
            pages = (0 until pagesArray.length()).map { pagesArray.getString(it) },
            isFree = obj.getBoolean("isFree"),
            order = obj.getInt("order"),
        )
    }
}.getOrDefault(emptyList())

/** Last-known catalog, so the list paints before Firestore answers. */
class SheetCache(private val context: Context) {

    suspend fun read(): List<SheetItem> {
        val json = context.dataStore.data.first()[CACHED_SHEETS_KEY] ?: return emptyList()
        return decodeSheets(json)
    }

    suspend fun write(items: List<SheetItem>) {
        context.dataStore.edit { prefs -> prefs[CACHED_SHEETS_KEY] = encodeSheets(items) }
    }
}
