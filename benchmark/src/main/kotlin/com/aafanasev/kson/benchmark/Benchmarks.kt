@file:JvmName("Runner")

package com.aafanasev.kson.benchmark

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    org.openjdk.jmh.Main.main(args)
}

@Suppress("unused")
open class Benchmarks {

    companion object {
        private const val FORK = 3
        private const val WARM_UPS = 4
        private const val ITERATIONS = 8
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = FORK)
    @Warmup(iterations = WARM_UPS)
    @Measurement(iterations = ITERATIONS)
    @OutputTimeUnit(value = TimeUnit.NANOSECONDS)
    fun reflectiveTypeAdapter(gsonState: GsonState, jsonState: JsonState) {
        val entity = gsonState.gson.fromJson(jsonState.json, Entity::class.java)
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = FORK)
    @Warmup(iterations = WARM_UPS)
    @Measurement(iterations = ITERATIONS)
    @OutputTimeUnit(value = TimeUnit.NANOSECONDS)
    fun generatedTypeAdapter(gsonState: GsonWithKsonState, jsonState: JsonState) {
        val entity = gsonState.gson.fromJson(jsonState.json, Entity::class.java)
    }

}
