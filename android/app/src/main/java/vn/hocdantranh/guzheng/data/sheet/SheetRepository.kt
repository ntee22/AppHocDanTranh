package vn.hocdantranh.guzheng.data.sheet

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface SheetRepository {
    /** The whole visible catalog, already ordered. Throws if Firestore is unreachable. */
    suspend fun load(): List<SheetItem>
}

/**
 * Reads the entire `sheets` collection in one query — no pagination.
 *
 * The catalog is 19 documents and search needs the full set client-side, so paging it the
 * way the Video tab does would make search silently incomplete. `isVisible` is filtered in
 * [sheetFromFields] rather than in a `where` clause, because combining it with `orderBy`
 * would demand a composite index; the single-field order uses the automatic one.
 */
class FirestoreSheetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : SheetRepository {

    override suspend fun load(): List<SheetItem> =
        firestore.collection("sheets")
            .orderBy("order")
            .get()
            .await()
            .documents
            .mapNotNull { sheetFromFields(it.id, it.data.orEmpty()) }
}
