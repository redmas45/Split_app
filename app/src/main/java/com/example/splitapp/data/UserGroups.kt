package com.example.splitapp.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart

/**
 * The user's groups, live: one listener per group, driven by the user's list of group ids.
 *
 * `flatMapLatest` cancels the previous per-group listeners (each one removes itself in its `awaitClose`) whenever the
 * id list changes, and when the collector goes away. So no listener outlives the list it belonged to, and an old
 * listener can never push a stale list over a newer one. Per-document reads only, so no 30-group cap and no `list` query.
 *
 * Emits null while any group has not reported yet (show a spinner, not "no groups"); a deleted or unreadable group is left out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun userGroupsFlow(groupIds: Flow<List<String>>, observe: (String) -> Flow<GroupState>): Flow<List<FirebaseGroup>?> =
    groupIds.distinctUntilChanged().flatMapLatest { ids ->
        val unique = ids.distinct()
        if (unique.isEmpty()) flowOf(emptyList())
        else combine(unique.map { id -> observe(id).onStart { emit(GroupState.Loading) } }) { states ->
            if (states.any { it is GroupState.Loading }) null
            else states.mapNotNull { (it as? GroupState.Ready)?.group }
        }
    }
