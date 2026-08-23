package vn.hocdantranh.guzheng

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreConnectionTest {

    @Before
    fun setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun initializesAgainstTheSharedProject() {
        val app = FirebaseApp.getInstance()
        assertEquals("apphocdantranh", app.options.projectId)
    }

    @Test
    fun readsTheSheetsCollectionWrittenByTheIosApp() = runBlocking {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("sheets")
            .limit(1)
            .get()
            .await()
        assertTrue(
            "sheets collection returned no documents — check Firestore rules allow unauthenticated reads",
            snapshot.documents.isNotEmpty()
        )
    }
}
