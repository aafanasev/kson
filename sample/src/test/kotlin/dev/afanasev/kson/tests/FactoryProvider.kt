package dev.afanasev.kson.tests

import dev.afanasev.kson.annotation.KsonFactory

@KsonFactory
object FactoryProvider {

    fun get() = KsonFactoryProvider()

}