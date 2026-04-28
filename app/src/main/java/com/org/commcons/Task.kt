package com.org.commcons

data class Task(
    val id: String = "",
    val ngoId: String = "",
    val title: String = "",
    val description: String = "",
    val requiredSkills: List<String> = emptyList(),
    val locationName: String = "",
    val priority: String = "medium",
    val status: String = "open",
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: String = "",
    val completedAt: Long = 0
)