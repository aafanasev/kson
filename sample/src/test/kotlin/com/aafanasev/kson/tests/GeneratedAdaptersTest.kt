package com.aafanasev.kson.tests

import com.aafanasev.kson.annotation.Kson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@Kson
data class Entity(val id: Int, val name: String)

@Kson
data class EntityWithNulls(val id: Int?, val name: String?)

@Kson
data class EntityWithList(val items: List<String>)

@Kson
data class EntityWithMap(val items: Map<String, Int>)

@Kson
data class EntityWithEntity(val entity: Entity, val entityWithNulls: EntityWithNulls)

@Kson
data class EntityWithCustomKeys(
        @SerializedName("customKey1") val key1: String,
        @SerializedName("customKey2") val key2: String)

class GeneratedAdaptersTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder()
                .registerTypeAdapterFactory(FactoryProvider.get())
                .create()
    }

    @Test
    fun `should parse correctly`() {
        val json = """
            {
                "id": 1,
                "name": "John"
            }
            """

        val entity = gson.fromJson(json, Entity::class.java)

        assertThat(entity.id).isEqualTo(1)
        assertThat(entity.name).isEqualTo("John")
    }

    @Test
    @Throws(NullPointerException::class)
    fun `should throw NPE when property is not nullable`() {
        val json = """
            {
                "id": 1,
                "name": null
            }
            """

        gson.fromJson(json, Entity::class.java)
    }

    @Test
    fun `should parse null values`() {
        val json = """
            {
                "id": null,
                "name": null
            }
            """

        val entity = gson.fromJson(json, EntityWithNulls::class.java)

        assertThat(entity.id).isNull()
        assertThat(entity.name).isNull()
    }

    @Test
    fun `should parse list`() {
        val json = """
            {
                "items": ["first", "second", "third"]
            }
            """

        val entity = gson.fromJson(json, EntityWithList::class.java)

        assertThat(entity.items.size).isEqualTo(3)
        assertThat(entity.items[0]).isEqualTo("first")
        assertThat(entity.items[1]).isEqualTo("second")
        assertThat(entity.items[2]).isEqualTo("third")
    }

    @Test
    fun `should parse map`() {
        val json = """
            {
                "items": {
                    "first": 1,
                    "second": 2,
                    "third": 3
                }
            }
            """

        val entity = gson.fromJson(json, EntityWithMap::class.java)

        assertThat(entity.items.size).isEqualTo(3)
        assertThat(entity.items["first"]).isEqualTo(1)
        assertThat(entity.items["second"]).isEqualTo(2)
        assertThat(entity.items["third"]).isEqualTo(3)
    }

    @Test
    fun `should parse complex entity`() {
        val json = """
            {
                "entity": {
                    "id": 1,
                    "name": "John"
                },
                "entityWithNulls": {
                    "id": 2,
                    "name": "Wick"
                }
            }
            """

        val entity = gson.fromJson(json, EntityWithEntity::class.java)

        assertThat(entity.entity.id).isEqualTo(1)
        assertThat(entity.entity.name).isEqualTo("John")
        assertThat(entity.entityWithNulls.id).isEqualTo(2)
        assertThat(entity.entityWithNulls.name).isEqualTo("Wick")
    }

    @Test
    fun `should not ignore SerializedName annotation`() {
        val json = """
            {
                "customKey1": "first",
                "customKey2": "second"
            }
            """

        val entity = gson.fromJson(json, EntityWithCustomKeys::class.java)

        assertThat(entity.key1).isEqualTo("first")
        assertThat(entity.key2).isEqualTo("second")

        val generatedJson = gson.toJson(entity)

        assertThat(generatedJson).isEqualToIgnoringWhitespace(json)
    }

}