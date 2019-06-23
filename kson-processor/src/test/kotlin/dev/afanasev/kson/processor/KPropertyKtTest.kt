package dev.afanasev.kson.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class KPropertyKtTest {

    @Test
    fun testTypeToAdapterName() {
        val int = ClassName.bestGuess("Int")
        assert("int_adapter", int)

        val string = ClassName.bestGuess("String")
        assert("string_adapter", string)

        val listOfString = List::class.parameterizedBy(String::class)
        assert("list_string_adapter", listOfString)

        val mapOfStringToInt = Map::class.parameterizedBy(String::class, Int::class)
        assert("map_string_int_adapter", mapOfStringToInt)

        val mapOfMap = Map::class.asClassName().parameterizedBy(Int::class.asTypeName(), mapOfStringToInt)
        assert("map_int_map_string_int_adapter", mapOfMap)
    }

    private fun assert(expectedName: String, type: TypeName) {
        assertThat(typeToAdapterName(type)).isEqualTo(expectedName)
    }
}
