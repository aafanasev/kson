package dev.afanasev.kson.processor

import com.squareup.kotlinpoet.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class KPropertyKtTest {

    @Test
    fun testTypeToAdapterName() {
        val int = ClassName.bestGuess("Int")
        assert("int_adapter", int)

        val string = ClassName.bestGuess("String")
        assert("string_adapter", string)

        val listOfString = ParameterizedTypeName.get(List::class, String::class)
        assert("list_string_adapter", listOfString)

        val mapOfStringToInt = ParameterizedTypeName.get(Map::class, String::class, Int::class)
        assert("map_string_int_adapter", mapOfStringToInt)

        val mapOfMap = ParameterizedTypeName.get(Map::class.asClassName(), Int::class.asTypeName(), mapOfStringToInt)
        assert("map_int_map_string_int_adapter", mapOfMap)
    }

    private fun assert(expectedName: String, type: TypeName) {
        assertThat(typeToAdapterName(type)).isEqualTo(expectedName)
    }

}
