package dev.afanasev.kson.tests

import dev.afanasev.kson.annotation.Kson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

/**
 * @see ReflectiveTypeAdapterFactory
 */
private const val REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME = "ReflectiveTypeAdapterFactory"

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

@Kson
data class EntityWithNestedCollections(val items: List<List<String>>)

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

        val adapter = gson.getAdapterName(Entity::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
    }

    @Test
    fun `should throw NPE when property is not nullable`() {
        val json = """
            {
                "id": 1,
                "name": null
            }
            """

        assertFailsWith<NullPointerException> {
            gson.fromJson(json, Entity::class.java)
        }
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

        val adapter = gson.getAdapterName(EntityWithNulls::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
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

        val adapter = gson.getAdapterName(EntityWithList::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
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

        val adapter = gson.getAdapterName(EntityWithMap::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
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


        val adapter = gson.getAdapterName(EntityWithEntity::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
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

        val adapter = gson.getAdapterName(EntityWithCustomKeys::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
    }

    @Test
    fun `should parse nested collections`() {
        val json = """
            {
                "items": [
                    [
                        "val1",
                        "val2"
                    ],
                    [
                        "val3"
                    ]
                ]
            }
            """

        val entity = gson.fromJson(json, EntityWithNestedCollections::class.java)

        assertThat(entity.items.size).isEqualTo(2)

        assertThat(entity.items[0].size).isEqualTo(2)
        assertThat(entity.items[0][0]).isEqualTo("val1")
        assertThat(entity.items[0][1]).isEqualTo("val2")

        assertThat(entity.items[1].size).isEqualTo(1)
        assertThat(entity.items[1][0]).isEqualTo("val3")

        val adapter = gson.getAdapterName(EntityWithNestedCollections::class)
        assertThat(adapter).doesNotContain(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
    }

    @Test
    fun `should use reflective adapter if factory is not registered`() {
        val adapter = Gson().getAdapterName(EntityWithNestedCollections::class)
        assertThat(adapter).contains(REFLECTIVE_TYPE_ADAPTER_FACTORY_NAME)
    }

    private fun Gson.getAdapterName(cls: KClass<*>) = getAdapter(cls.java).javaClass.name

}