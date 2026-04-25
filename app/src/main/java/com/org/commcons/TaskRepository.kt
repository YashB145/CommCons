package com.org.commcons

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tasksCollection = db.collection("tasks")

    fun createTask(task: Task, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val docRef = tasksCollection.document()
        val taskWithId = task.copy(id = docRef.id)
        docRef.set(taskWithId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
    }

    fun getOpenTasks(onSuccess: (List<Task>) -> Unit, onFailure: (String) -> Unit) {
        tasksCollection
            .whereEqualTo("status", "open")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull {
                    it.toObject(Task::class.java)
                } ?: emptyList()
                onSuccess(tasks)
            }
    }

    fun getNgoTasks(ngoId: String, onSuccess: (List<Task>) -> Unit, onFailure: (String) -> Unit) {
        tasksCollection
            .whereEqualTo("ngoId", ngoId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull {
                    it.toObject(Task::class.java)
                } ?: emptyList()
                onSuccess(tasks)
            }
    }

    fun updateTaskStatus(taskId: String, status: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        tasksCollection.document(taskId)
            .update("status", status)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
    }

    fun deleteTask(taskId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        tasksCollection.document(taskId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
    }

    fun acceptTask(taskId: String, volunteerId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val assignment = hashMapOf(
            "taskId" to taskId,
            "volunteerId" to volunteerId,
            "status" to "accepted",
            "assignedAt" to System.currentTimeMillis()
        )
        db.collection("volunteersAssigned")
            .document("${taskId}_${volunteerId}")
            .set(assignment)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
    }
}