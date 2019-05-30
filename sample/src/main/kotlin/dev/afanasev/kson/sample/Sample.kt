package dev.afanasev.kson.sample

import dev.afanasev.kson.annotation.Kson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.afanasev.kson.generated.KsonTypeAdapterFactory // generated factory

@Kson
data class TestEntity1(val id: Int,
                       val title: String)

@Kson
data class TestEntity2(@SerializedName("idVal") val id: Int,
                       val title: String?,
                       val list: List<String>,
                       val map: Map<String, List<Double>>?)

const val JSON = """{
    "id": 42,
    "title": "Hello world"
    }"""

fun main(args: Array<String>) {
    val gson = GsonBuilder()
            .registerTypeAdapterFactory(KsonTypeAdapterFactory())
            .create()

    val dto: TestEntity1 = gson.fromJson(JSON, TestEntity1::class.java)

    println(dto)
}