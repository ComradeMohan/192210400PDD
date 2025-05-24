package com.simats.univalut.models

import java.io.Serializable

data class Faculty(
    val id: String,
    val name: String,
    val email: String,
    val phone_number: String,
    val college: String,
    val login_id: String,
    val password: String
) : Serializable
