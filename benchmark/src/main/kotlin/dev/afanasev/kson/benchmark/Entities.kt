package dev.afanasev.kson.benchmark

import dev.afanasev.kson.annotation.Kson
import com.google.gson.annotations.SerializedName
import java.util.*

@Kson
data class Entity(val employees: List<Employee>)

@Kson
data class Employee(
        val _id: String,
        val index: Int,
        val guid: String,
        @SerializedName("isActive") val active: Boolean,
        val balance: String,
        val picture: String,
        val age: Int,
        val eyeColor: String,
        val name: String,
        val gender: String,
        val company: String,
        val email: String,
        val phone: String,
        val address: String,
        val about: String,
        val registered: Date,
        val latitude: Double,
        val longitude: Double,
        val tags: Set<String>,
        val friends: List<Friend>,
        val greeting: String,
        val favoriteFruit: String
)

@Kson
data class Friend(
        val id: Int,
        val name: String
)
