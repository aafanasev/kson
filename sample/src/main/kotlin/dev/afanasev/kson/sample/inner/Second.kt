package dev.afanasev.kson.sample.inner

import dev.afanasev.kson.annotation.Kson

@Kson
data class Second(
    val result: Result
) {

    @Kson
    data class Result(
        val number: Int
    )
}