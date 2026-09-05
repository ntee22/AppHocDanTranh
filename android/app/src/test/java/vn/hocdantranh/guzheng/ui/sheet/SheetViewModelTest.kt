package vn.hocdantranh.guzheng.ui.sheet

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import vn.hocdantranh.guzheng.data.sheet.SheetItem
import vn.hocdantranh.guzheng.data.sheet.SheetRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SheetViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String, title: String = "Sheet $id") =
        SheetItem(id, title, listOf("https://drive.google.com/file/d/$id/view"), true, id.toInt())

    private class FakeRepository(
        private val result: Result<List<SheetItem>>,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : SheetRepository {
        var calls = 0
            private set

        override suspend fun load(): List<SheetItem> {
            calls++
            gate?.await()
            return result.getOrThrow()
        }
    }

    @Test
    fun `loads the catalog on construction`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.success(listOf(item("1")))), cache = null)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.sheets.size)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `is loading until the repository answers`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val vm = SheetViewModel(FakeRepository(Result.success(emptyList()), gate), cache = null)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `a failure with no cached sheets surfaces an error`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.failure(RuntimeException("offline"))), cache = null)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.sheets.isEmpty())
    }

    /** Stale sheets beat an error screen — the user can still read what they had. */
    @Test
    fun `a failure keeps sheets already on screen and shows no error`() = runTest {
        val good = FakeRepository(Result.success(listOf(item("1"))))
        val vm = SheetViewModel(good, cache = null)
        advanceUntilIdle()

        vm.replaceRepositoryForTest(FakeRepository(Result.failure(RuntimeException("offline"))))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.sheets.size)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `refresh replaces the catalog`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.success(listOf(item("1")))), cache = null)
        advanceUntilIdle()

        vm.replaceRepositoryForTest(FakeRepository(Result.success(listOf(item("2"), item("3")))))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf("2", "3"), vm.uiState.value.sheets.map { it.id })
    }

    @Test
    fun `retry after a failure clears the error`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.failure(RuntimeException("offline"))), cache = null)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        vm.replaceRepositoryForTest(FakeRepository(Result.success(listOf(item("1")))))
        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.errorMessage)
        assertEquals(1, vm.uiState.value.sheets.size)
    }

    @Test
    fun `search updates the query`() = runTest {
        val vm = SheetViewModel(FakeRepository(Result.success(listOf(item("1", "Nhỏ ơi")))), cache = null)
        advanceUntilIdle()

        vm.search("nhỏ")
        assertEquals("nhỏ", vm.uiState.value.query)
        assertEquals(1, vm.uiState.value.filtered.size)

        vm.search("zzz")
        assertTrue(vm.uiState.value.filtered.isEmpty())
    }
}
