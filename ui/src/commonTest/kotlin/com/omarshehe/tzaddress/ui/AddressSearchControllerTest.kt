package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AddressSearchControllerTest {
    private val repo = FakeRepository()

    private fun TestScope.controller() = AddressSearchController(repo, backgroundScope)

    // advanceUntilIdle() ignores backgroundScope work, so move virtual time explicitly.
    private fun TestScope.settle() {
        advanceTimeBy(2_000)
        runCurrent()
    }

    @Test fun startsIdle() = runTest {
        assertEquals(SearchUiState.Idle, controller().state.value)
    }

    @Test fun rapidTypingSearchesOnceWithTheLastQuery() = runTest {
        val c = controller()
        c.onQueryChange("k"); advanceTimeBy(100)
        c.onQueryChange("ki"); advanceTimeBy(100)
        c.onQueryChange("kiv"); advanceTimeBy(249)
        assertTrue(repo.searchQueries.isEmpty(), "nothing before the 250 ms debounce")
        advanceTimeBy(2); runCurrent()
        assertEquals(listOf("kiv"), repo.searchQueries)
    }

    @Test fun blankQueryIsIdleAndNeverSearches() = runTest {
        val c = controller()
        c.onQueryChange("   "); settle()
        assertEquals(SearchUiState.Idle, c.state.value)
        assertTrue(repo.searchQueries.isEmpty())
    }

    @Test fun clearingTheQueryDropsShownResultsImmediately() = runTest {
        repo.searchHandler = { listOf(repo.match(Level.WARD, "11101", "Kivukoni")) }
        val c = controller()
        c.onQueryChange("kiv"); settle()
        assertIs<SearchUiState.Results>(c.state.value)
        c.onQueryChange("")
        assertEquals(SearchUiState.Idle, c.state.value)
    }

    @Test fun resultsAreCappedAtTen() = runTest {
        repo.searchHandler = { List(25) { repo.match(Level.WARD, "11101", "Kivukoni $it") } }
        val c = controller()
        c.onQueryChange("kiv"); settle()
        assertEquals(10, assertIs<SearchUiState.Results>(c.state.value).matches.size)
    }

    @Test fun noHitsIsNoMatches() = runTest {
        val c = controller()
        c.onQueryChange("zzzz"); settle()
        assertEquals(SearchUiState.NoMatches, c.state.value)
    }

    @Test fun repositoryFailureIsAnInlineErrorAndSearchKeepsWorking() = runTest {
        repo.searchHandler = { error("db closed") }
        val c = controller()
        c.onQueryChange("kiv"); settle()
        assertIs<SearchUiState.Error>(c.state.value)
        repo.searchHandler = { listOf(repo.match(Level.WARD, "11101", "Kivukoni")) }
        c.onQueryChange("kivu"); settle()
        assertIs<SearchUiState.Results>(c.state.value)
    }

    @Test fun newerQueryWinsOverASlowOlderOne() = runTest {
        repo.searchHandler = { q ->
            if (q == "slow") { delay(1000); listOf(repo.match(Level.WARD, "11101", "STALE")) }
            else listOf(repo.match(Level.WARD, "11106", "Kariakoo"))
        }
        val c = controller()
        c.onQueryChange("slow"); advanceTimeBy(300)
        c.onQueryChange("fast"); settle()
        val labels = assertIs<SearchUiState.Results>(c.state.value).matches.map { it.label }
        assertEquals(listOf("Kariakoo"), labels)
    }

    @Test fun cancellationIsNotReportedAsAnError() = runTest {
        repo.searchHandler = { throw CancellationException("cancelled") }
        val c = controller()
        c.onQueryChange("kiv"); settle()
        assertTrue(c.state.value !is SearchUiState.Error)
    }
}
