package com.example.splitapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FirebaseGroup(
    val id: String = "",
    val name: String = "",
    val members: List<Map<String, Any>> = emptyList(),
    val transactions: List<Map<String, Any>> = emptyList()
)

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: com.google.firebase.auth.FirebaseUser?
        get() = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signUp(email: String, password: String): Result<com.google.firebase.auth.AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            // Create user document in Firestore
            db.collection("users").document(result.user!!.uid).set(
                mapOf(
                    "email" to email,
                    "groups" to emptyList<String>()
                )
            ).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<com.google.firebase.auth.AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun createGroup(groupName: String, creatorName: String): Result<String> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val groupRef = db.collection("groups").document()
            val groupId = groupRef.id

            val initialMembers = listOf(
                mapOf("id" to 1, "name" to creatorName)
            )

            val groupData = mapOf(
                "id" to groupId,
                "name" to groupName,
                "members" to initialMembers,
                "transactions" to emptyList<Map<String, Any>>()
            )

            db.runBatch { batch ->
                batch.set(groupRef, groupData)
                batch.update(db.collection("users").document(userId), "groups", FieldValue.arrayUnion(groupId))
            }.await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGroup(groupId: String, userName: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return try {
            val groupRef = db.collection("groups").document(groupId)
            val snapshot = groupRef.get().await()
            
            if (!snapshot.exists()) {
                return Result.failure(Exception("Group not found"))
            }

            val membersList = snapshot.get("members") as? List<Map<String, Any>> ?: emptyList()
            // Generate next member ID
            val maxId = membersList.mapNotNull { (it["id"] as? Number)?.toInt() }.maxOrNull() ?: 0
            val nextId = maxId + 1

            val newMember = mapOf("id" to nextId, "name" to userName)

            db.runBatch { batch ->
                batch.update(groupRef, "members", FieldValue.arrayUnion(newMember))
                batch.update(db.collection("users").document(userId), "groups", FieldValue.arrayUnion(groupId))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserGroups(): Flow<List<Map<String, Any>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { userSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val groupIds = userSnapshot?.get("groups") as? List<String> ?: emptyList()
                if (groupIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Fetch details for each group
                // Note: Firestore 'in' query supports up to 30 items
                db.collection("groups").whereIn("id", groupIds)
                    .addSnapshotListener { groupsSnapshot, groupsError ->
                        if (groupsError != null) {
                            close(groupsError)
                            return@addSnapshotListener
                        }
                        val list = groupsSnapshot?.documents?.mapNotNull { doc ->
                            doc.data?.plus("id" to doc.id)
                        } ?: emptyList()
                        trySend(list)
                    }
            }
        awaitClose { listener.remove() }
    }

    fun observeGroup(groupId: String): Flow<FirebaseGroup?> = callbackFlow {
        val listener = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val group = snapshot.toObject(FirebaseGroup::class.java)
                    trySend(group)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateGroupData(groupId: String, members: List<Map<String, Any>>, transactions: List<Map<String, Any>>): Result<Unit> {
        return try {
            db.collection("groups").document(groupId).update(
                mapOf(
                    "members" to members,
                    "transactions" to transactions
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
