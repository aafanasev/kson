package com.aafanasev.kson.benchmark

import com.aafanasev.kson.generated.KsonTypeAdapterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
class GsonState {

    lateinit var gson: Gson

    @Setup(Level.Invocation)
    fun setUp() {
        gson = GsonBuilder().create()
    }
}

@State(Scope.Benchmark)
class GsonWithKsonState {

    lateinit var gson: Gson

    @Setup(Level.Invocation)
    fun setUp() {
        gson = GsonBuilder()
                .registerTypeAdapterFactory(KsonTypeAdapterFactory())
                .create()
    }
}

@State(Scope.Benchmark)
class JsonState {

    lateinit var json: String

    @Setup(Level.Invocation)
    fun setUp() {
        json = javaClass.getResource("/data.json").readText()
    }
}
