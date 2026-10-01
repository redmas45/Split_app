package com.example.splitapp.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserGroupsTest {

    /** Stands in for observeGroup(id): records which Firestore-style listeners are open right now. */
    private class FakeGroups {
        val open = mutableListOf<String>()      // listeners currently attached (one entry per attach)
        val opened = mutableListOf<String>()    // every attach ever
        private val sinks = mutableListOf<Pair<String, SendChannel<GroupState>>>()

        fun observe(id: String): Flow<GroupState> = callbackFlow {
            val sink = id to channel
            open += id; opened += id; sinks += sink
            awaitClose { open.remove(id); sinks.remove(sink) }
        }

        // Like Firestore: every attached listener for this id receives the update (a leaked one too).
        fun emit(id: String, state: GroupState) {
            val targets = sinks.filter { it.first == id }
            check(targets.isNotEmpty()) { "listener for $id is not open" }
            targets.forEach { it.second.trySend(state) }
        }
    }

    private fun g(id: String, name: String = "N$id") = FirebaseGroup(id = id, name = name)
    private fun ready(id: String, name: String = "N$id") = GroupState.Ready(g(id, name))

    private class Harness(val fake: FakeGroups, val ids: MutableSharedFlow<List<String>>, val results: MutableList<List<FirebaseGroup>?>, val job: kotlinx.coroutines.Job)

    private fun TestScope.start(): Harness {
        val fake = FakeGroups()
        val ids = MutableSharedFlow<List<String>>(replay = 1)
        val results = mutableListOf<List<FirebaseGroup>?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { userGroupsFlow(ids, fake::observe).toList(results) }
        return Harness(fake, ids, results, job)
    }

    @Test fun noGroups_emitsAnEmptyListImmediately() = runTest {
        val h = start()
        h.ids.tryEmit(emptyList())
        assertEquals(listOf(emptyList<FirebaseGroup>()), h.results)
        h.job.cancel()
    }

    @Test fun waitsForEveryGroupBeforeShowingTheList() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a", "b"))
        h.fake.emit("a", ready("a"))
        assertTrue("list must stay 'loading' (null) until b reports", h.results.all { it == null })
        h.fake.emit("b", ready("b"))
        assertEquals(listOf(g("a"), g("b")), h.results.last())
        h.job.cancel()
    }

    @Test fun aDeletedGroupIsLeftOut() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a", "b"))
        h.fake.emit("a", ready("a"))
        h.fake.emit("b", GroupState.NotFound)
        assertEquals(listOf(g("a")), h.results.last())
        h.job.cancel()
    }

    @Test fun changingTheGroupListClosesEveryOldListener() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a"))
        h.fake.emit("a", ready("a"))
        h.ids.tryEmit(listOf("a", "b"))
        assertEquals(listOf("a", "b"), h.fake.open.sorted())   // no second, stale listener for a
        h.ids.tryEmit(listOf("b"))
        assertEquals(listOf("b"), h.fake.open)                 // a is gone, nothing left behind
        h.job.cancel()
    }

    @Test fun leavingTheScreenClosesEveryListener() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a", "b", "c"))
        assertEquals(3, h.fake.open.size)
        h.job.cancel()
        assertTrue(h.fake.open.isEmpty())
    }

    @Test fun activityInAnOldGroupCannotRemoveANewlyJoinedGroup() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a"))
        h.fake.emit("a", ready("a"))
        h.ids.tryEmit(listOf("a", "b"))                        // user joins b
        h.fake.emit("a", ready("a"))
        h.fake.emit("b", ready("b"))
        val afterJoin = h.results.size
        h.fake.emit("a", ready("a", name = "A edited"))        // someone adds a transaction in a
        assertEquals(listOf(g("a", "A edited"), g("b")), h.results.last())
        // Not just the last one: no emission after the join may be a stale list without b.
        assertTrue(h.results.drop(afterJoin - 1).all { list -> list?.any { it.id == "b" } == true })
        h.job.cancel()
    }

    @Test fun noLimitOf30Groups() = runTest {
        val h = start()
        val ids = (0 until 35).map { "g$it" }
        h.ids.tryEmit(ids)
        ids.forEach { h.fake.emit(it, ready(it)) }
        assertEquals(ids, h.results.last()!!.map { it.id })
        h.job.cancel()
    }

    @Test fun duplicateIdsAreListenedToOnce() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a", "a"))
        assertEquals(listOf("a"), h.fake.open)
        h.fake.emit("a", ready("a"))
        assertEquals(listOf(g("a")), h.results.last())
        h.job.cancel()
    }

    @Test fun anUnrelatedUserDocumentChangeDoesNotReopenListeners() = runTest {
        val h = start()
        h.ids.tryEmit(listOf("a"))
        h.ids.tryEmit(listOf("a"))      // user document changed, group list did not
        assertEquals(listOf("a"), h.fake.opened)
        h.job.cancel()
    }
}
