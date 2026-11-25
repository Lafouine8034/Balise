package org.balise.app

data class Place(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val savedAt: Long
)
