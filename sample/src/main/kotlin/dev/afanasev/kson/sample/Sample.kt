package dev.afanasev.kson.sample

import com.google.gson.GsonBuilder
import dev.afanasev.kson.annotation.Default
import dev.afanasev.kson.annotation.Kson
import dev.afanasev.kson.sample.inner.TypeAdapterProvider

data class TestEntity(
        val id: Int,
        val title: String
)

@Kson
data class KsonEntity(
        val id: Int,
        val title: String,
        val title2: String?,
) {
    companion object {
        @Default
        fun getDefault() = KsonEntity(23, "def", "sdf")
    }
}

const val JSON = """{
    "id": 42,
    "title": null
    }"""

fun main(args: Array<String>) {
    val gson = GsonBuilder()
            .registerTypeAdapterFactory(TypeAdapterProvider.get())
            .create()

    // Doesn't throw any exception when id = null
    val dangerEntity = gson.fromJson(JSON, TestEntity::class.java)

    try {
        // Throws exception in runtime
        dangerEntity.title.length
    } catch (e: Exception) {
        println("Not safe runtime exception")
    }

    // Throws NPE immediately
    try {
        gson.fromJson(JSON, KsonEntity::class.java)
    } catch (e: Exception) {
        println("Caught exception during JSON parsing")
    }
}
