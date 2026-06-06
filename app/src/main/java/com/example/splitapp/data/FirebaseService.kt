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
            
            // Ensure user document exists (important for manually created accounts like Super Admin)
            val user = result.user
            if (user != null) {
                val userDoc = db.collection("users").document(user.uid).get().await()
                if (!userDoc.exists()) {
                    db.collection("users").document(user.uid).set(
                        mapOf(
                            "email" to (user.email ?: email),
                            "groups" to emptyList<String>(),
                            "isBanned" to false
                        )
                    ).await()
                }
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<com.google.firebase.auth.AuthResult> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            
            // Check if user exists in Firestore, if not create them
            val userDoc = db.collection("users").document(result.user!!.uid).get().await()
            if (!userDoc.exists()) {
                db.collection("users").document(result.user!!.uid).set(
                    mapOf(
                        "email" to (result.user!!.email ?: ""),
                        "groups" to emptyList<String>(),
                        "isBanned" to false
                    )
                ).await()
            }
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
                batch.set(
                    db.collection("users").document(userId),
                    mapOf("groups" to FieldValue.arrayUnion(groupId)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }.await()

            Result.success(groupId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
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
                batch.set(
                    db.collection("users").document(userId),
                    mapOf("groups" to FieldValue.arrayUnion(groupId)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
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
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val groupIds = userSnapshot?.get("groups") as? List<String> ?: emptyList()
                if (groupIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Fetch details for each group
                // Note: Firestore 'in' query supports up to 30 items
                val safeGroupIds = groupIds.take(30)
                db.collection("groups").whereIn("id", safeGroupIds)
                    .addSnapshotListener { groupsSnapshot, groupsError ->
                        if (groupsError != null) {
                            trySend(emptyList())
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
                    trySend(null)
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

    suspend fun banUser(uid: String, ban: Boolean): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("isBanned", ban).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            db.collection("groups").document(groupId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

const val ADMIN_EMAIL = "kalimateym2@gmail.com"
