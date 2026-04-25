package com.org.commcons

data class VolunteerProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val tasksCompleted: Int = 0,
    val tasksAccepted: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)