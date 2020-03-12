package dev.afanasev.kson.sample.inner

import dev.afanasev.kson.annotation.Kson

@Kson
data class Third(
    val result: Fourth
) {

    @Kson
    data class Fourth(
        val result: Result
    ) {

        @Kson
        data class Result(
            val number: Int
        )
    }
}