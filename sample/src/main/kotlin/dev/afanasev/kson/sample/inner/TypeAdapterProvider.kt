package dev.afanasev.kson.sample.inner

import dev.afanasev.kson.annotation.KsonFactory

@KsonFactory
object TypeAdapterProvider {

    fun get() = KsonTypeAdapterProvider()

}