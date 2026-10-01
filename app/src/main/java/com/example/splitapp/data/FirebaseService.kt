package com.example.splitapp.data

import android.util.Log
import com.example.splitapp.domain.JoinChoice
import com.example.splitapp.domain.JoinPlan
import com.example.splitapp.domain.Member
import com.example.splitapp.domain.TransactionDraft
import com.example.splitapp.domain.addMemberUpdate
import com.example.splitapp.domain.appendTransaction
import com.example.splitapp.domain.applyJoinPlan
import com.example.splitapp.domain.memberFromMap
import com.example.splitapp.domain.planJoin
import com.example.splitapp.domain.removalBlockReason
import com.example.splitapp.domain.removeTransaction
import com.example.splitapp.domain.toMap
import com.example.splitapp.domain.transactionFromMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FirebaseGroup(
    val id: String = "",
    val name: String = "",
    val creatorUid: String? = null,
    val members: List<Map<String, Any>> = emptyList(),
    val transactions: List<Map<String, Any>> = emptyList()
)

sealed class GroupState {
    data object Loading : GroupState()
    data class Ready(val group: FirebaseGroup) : GroupState()
    data object NotFound : GroupState()
    data class Failed(val message: String) : GroupState()
}

/** A refused edit with a message that is safe to show the user as-is. */
class GroupEditException(message: String) : Exception(message)

private const val MAX_MEMBERS = 30

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: com.google.firebase.auth.FirebaseUser?
        get() = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /** The signed-in user's uid, or null when signed out. Emits on every login, logout and account switch. */
    fun authState(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    // Creates a missing user document (needed for manually created accounts like the Super Admin). It only ever
    // writes email + groups: isBanned is the admin's to set, and writing it here used to silently unban people.
    // The exists-check stays because a merge of groups = [] onto an existing document would wipe the user's groups.
    // Best-effort: the sign-in already succeeded, and createGroup/joinGroup also create the document when needed.
    private suspend fun ensureUserDoc(user: com.google.firebase.auth.FirebaseUser?, fallbackEmail: String) {
        if (user == null) return
        try {
            val ref = db.collection("users").document(user.uid)
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf("email" to (user.email ?: fallbackEmail), "groups" to emptyList<String>()),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("FirebaseService", "Could not ensure user document for ${user.uid}", e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<com.google.firebase.auth.AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            ensureUserDoc(result.user, email)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<com.google.firebase.auth.AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            ensureUserDoc(result.user, email)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<com.google.firebase.auth.AuthResult> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            ensureUserDoc(result.user, "")
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Emails a password-reset link. The caller shows [authErrorMessage] on failure. */
    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut() {
        auth.signOut()
    }

    /** The id a new group will get, known before the write finishes (offline, a write only finishes once synced). */
    fun newGroupId(): String = db.collection("groups").document().id

    // Cancellation must propagate (see `attempt`): the hub gives up waiting after a timeout when offline, and the
    // write itself stays queued in Firestore's local cache and syncs later.
    suspend fun createGroup(groupId: String, groupName: String, creatorName: String): Result<Unit> = attempt {
        val userId = requireUid()
        val groupRef = db.collection("groups").document(groupId)

        val groupData = mapOf(
            "id" to groupId,
            "name" to groupName,
            "creatorUid" to userId,
            "members" to listOf(Member(1, creatorName, userId).toMap()),
            "transactions" to emptyList<Map<String, Any>>()
        )

        db.runBatch { batch ->
            batch.set(groupRef, groupData)
            batch.set(
                db.collection("users").document(userId),
                mapOf("groups" to FieldValue.arrayUnion(groupId)),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }.await()
        Unit
    }

    // ---- Group edits ----
    // Every edit is one Firestore transaction: re-read the latest group, apply ONE change (decided by the pure
    // functions in domain/GroupOps.kt), write it back. Two people editing at once can no longer overwrite each
    // other or get the same id. Transactions need a connection, so offline they fail with a clear message.

    private fun requireUid(): String = auth.currentUser?.uid ?: throw GroupEditException("You're signed out. Please log in again.")

    @Suppress("UNCHECKED_CAST")
    private fun rawList(snapshot: DocumentSnapshot, field: String): List<Map<String, Any>> =
        snapshot.get(field) as? List<Map<String, Any>> ?: emptyList()

    private fun existingGroup(t: com.google.firebase.firestore.Transaction, ref: com.google.firebase.firestore.DocumentReference): DocumentSnapshot =
        t.get(ref).also { if (!it.exists()) throw GroupEditException("This group no longer exists.") }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // A refusal thrown inside runTransaction may arrive wrapped, so look for it down the cause chain.
        val refusal = generateSequence<Throwable>(e) { it.cause }.firstOrNull { it is GroupEditException }
        Result.failure(
            when {
                refusal != null -> refusal
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                    GroupEditException("You're offline — try again.")
                else -> e
            }
        )
    }

    suspend fun addTransaction(groupId: String, draft: TransactionDraft): Result<Unit> = attempt {
        val uid = requireUid()
        val ref = db.collection("groups").document(groupId)
        db.runTransaction { t ->
            val snap = existingGroup(t, ref)
            t.update(ref, "transactions", appendTransaction(rawList(snap, "transactions"), draft, System.currentTimeMillis(), uid))
            null
        }.await()
        Unit
    }

    suspend fun deleteTransaction(groupId: String, txId: Int): Result<Unit> = attempt {
        requireUid()
        val ref = db.collection("groups").document(groupId)
        db.runTransaction { t ->
            val snap = existingGroup(t, ref)
            t.update(ref, "transactions", removeTransaction(rawList(snap, "transactions"), txId))
            null
        }.await()
        Unit
    }

    suspend fun addMember(groupId: String, name: String): Result<Unit> = attempt {
        requireUid()
        if (name.isBlank()) throw GroupEditException("Enter a name.")
        val ref = db.collection("groups").document(groupId)
        db.runTransaction { t ->
            val snap = existingGroup(t, ref)
            val members = rawList(snap, "members")
            if (members.size >= MAX_MEMBERS) throw GroupEditException("A group can have up to $MAX_MEMBERS members.")
            val update = addMemberUpdate(members, rawList(snap, "transactions"), name)
            t.update(ref, mapOf("members" to update.members, "transactions" to update.transactions))
            null
        }.await()
        Unit
    }

    suspend fun removeMember(groupId: String, memberId: Int): Result<Unit> = attempt {
        requireUid()
        val ref = db.collection("groups").document(groupId)
        db.runTransaction { t ->
            val snap = existingGroup(t, ref)
            val members = rawList(snap, "members")
            val target = members.mapNotNull { memberFromMap(it) }.firstOrNull { it.id == memberId }
                ?: throw GroupEditException("That member is no longer in this group.")
            if (members.size <= 1) throw GroupEditException("A group needs at least one member.")
            removalBlockReason(target, rawList(snap, "transactions").mapNotNull { transactionFromMap(it) })
                ?.let { throw GroupEditException(it) }
            t.update(ref, "members", members.filter { (it["id"] as? Number)?.toInt() != memberId })
            null
        }.await()
        Unit
    }

    /** Joins (or re-opens) a group: links this account to a member and adds the group to the user's list, in one transaction. */
    suspend fun joinGroup(groupId: String, choice: JoinChoice): Result<Unit> = attempt {
        val uid = requireUid()
        val groupRef = db.collection("groups").document(groupId)
        val userRef = db.collection("users").document(uid)
        db.runTransaction { t ->
            val snap = t.get(groupRef)
            if (!snap.exists()) throw GroupEditException("No group with that code.")
            val members = rawList(snap, "members")
            val txs = rawList(snap, "transactions")
            val plan = planJoin(members.mapNotNull { memberFromMap(it) }, uid, choice)
            if (plan is JoinPlan.Rejected) throw GroupEditException(plan.message)
            if (plan !is JoinPlan.AlreadyMember) {
                val update = applyJoinPlan(members, txs, plan, uid)
                t.update(groupRef, mapOf("members" to update.members, "transactions" to update.transactions))
            }
            t.set(userRef, mapOf("groups" to FieldValue.arrayUnion(groupId)), com.google.firebase.firestore.SetOptions.merge())
            null
        }.await()
        Unit
    }

    /** Step 1 of joining: look the group up by its code so the user can say which member they are. */
    suspend fun fetchGroupForJoin(code: String): Result<FirebaseGroup> = attempt {
        requireUid()
        // A code with '/' or nothing at all can't be a document id (and would crash `document()`).
        if (code.isEmpty() || '/' in code) throw GroupEditException("No group with that code.")
        val snap = db.collection("groups").document(code).get().await()
        if (!snap.exists()) throw GroupEditException("No group with that code.")
        snap.toObject(FirebaseGroup::class.java)?.copy(id = snap.id) ?: throw GroupEditException("That group couldn't be read.")
    }

    /** Leaves a group: it disappears from this user's list. The member entry stays, so history keeps working. */
    suspend fun leaveGroup(groupId: String): Result<Unit> = attempt {
        val uid = requireUid()
        db.collection("users").document(uid)
            .set(mapOf("groups" to FieldValue.arrayRemove(groupId)), com.google.firebase.firestore.SetOptions.merge())
            .await()
        Unit
    }

    // The ids of the groups this user belongs to (users/{uid}.groups), live.
    private fun observeMyGroupIds(): Flow<List<String>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { userSnapshot, error ->
                val ids = if (error != null) emptyList()
                else (userSnapshot?.get("groups") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    /** The user's groups, live (null = still loading). One listener per group, all removed when collection stops. */
    fun observeUserGroups(): Flow<List<FirebaseGroup>?> = userGroupsFlow(observeMyGroupIds(), ::observeGroup)

    fun observeGroup(groupId: String): Flow<GroupState> = callbackFlow {
        val listener = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseService", "observeGroup($groupId) failed", error)
                    trySend(GroupState.Failed("Couldn't load this group. Check your connection and try again."))
                    return@addSnapshotListener
                }
                when {
                    snapshot == null -> Unit
                    snapshot.exists() -> {
                        val group = snapshot.toObject(FirebaseGroup::class.java)?.copy(id = snapshot.id)
                        trySend(group?.let { GroupState.Ready(it) } ?: GroupState.Failed("This group couldn't be read."))
                    }
                    // Not in the local cache yet: wait for the server before claiming it was deleted.
                    snapshot.metadata.isFromCache -> Unit
                    else -> trySend(GroupState.NotFound)
                }
            }
        awaitClose { listener.remove() }
    }

    fun isAdmin(): Boolean = auth.currentUser?.email == ADMIN_EMAIL

    fun getAllUsers(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("uid" to doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getAllGroups(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = db.collection("groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("id" to doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun checkIsBanned(uid: String): Boolean {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getBoolean("isBanned") ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun banUser(uid: String, ban: Boolean): Result<Unit> = attempt {
        db.collection("users").document(uid).update("isBanned", ban).await()
        Unit
    }

    /**
     * Deletes the group AND removes its id from every member's list, in one batch, so nobody is left with a dangling
     * reference. (There is deliberately no "delete user": that only removed the Firestore record, left the login
     * account in place, and un-banned the person. Real account deletion needs the Admin SDK; ban is the tool here.)
     */
    suspend fun deleteGroup(groupId: String): Result<Unit> = attempt {
        val members = db.collection("users").whereArrayContains("groups", groupId).get().await()
        db.runBatch { batch ->
            // ponytail: one batch holds at most 500 writes, so this covers a group of up to 499 members; chunk the
            // updates across batches if groups ever get that large.
            members.documents.forEach { batch.update(it.reference, "groups", FieldValue.arrayRemove(groupId)) }
            batch.delete(db.collection("groups").document(groupId))
        }.await()
        Unit
    }
}

const val ADMIN_EMAIL = "kalimateym2@gmail.com"
