package com.example.petcarekotlin.data.model

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val pets: List<String> = listOf()
)
