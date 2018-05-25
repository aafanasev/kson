package com.aafanasev.kson.benchmark

import org.openjdk.jmh.annotations.Benchmark

fun main(args: Array<String>) {
    org.openjdk.jmh.Main.main(args)
}

@Suppress("unused")
open class Benchmarks {

    @Benchmark
    fun reflectiveTypeAdapter(gsonState: GsonState, jsonState: JsonState) {
        val entity = gsonState.gson.fromJson(jsonState.json, Entity::class.java)
    }

    @Benchmark
    fun generatedTypeAdapter(gsonState: GsonWithKsonState, jsonState: JsonState) {
        val entity = gsonState.gson.fromJson(jsonState.json, Entity::class.java)
    }

}
