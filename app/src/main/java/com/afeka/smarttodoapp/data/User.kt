package com.afeka.smarttodoapp.data

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)