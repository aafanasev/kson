![maven](https://maven-badges.herokuapp.com/maven-central/net.afanasev/kson-annotation/badge.svg?style=flat
) 
[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-KSON-green.svg?style=flat )](https://android-arsenal.com/details/1/6949)

![Kson - kotlin type adapter generator](kson-logo.png)

An annotation processor generates Gson TypeAdapter from Kotlin Data Classes

## Motivation

By default, Gson uses reflection to read/write data from JSON. It's not only slow ([benchmarks](/benchmark)), also it breaks Kotlin's null-safe types. 

For example:
```kotlin
// your entity class with non-nullable property
data class Entity(val id: Int)

// wrong response from server
val json = """{ "id": null }"""

// this won't throw error
val entity = gson.fromJson(json, Entity::class.java)

// throws NPE somewhere in runtime when you don't expect
entity.id
```

In order to avoid reflection, need to create a custom TypeAdapter for every entity class. It takes time, need to write boilerplate code, tests, etc. Here this library comes, it generates TypeAdapters automatically. You just need to register generated GsonTypeAdapterFactory in your GsonBuilder.

## Usage

Add `@Kson` annotation to your data classes and Kson will automatically generate `<class name>TypeAdapter.kt` files.

```kotlin
@Kson
data class RoleEntity(
    val id: Int, 
    @SerializedName("roleName") val name: String
)

@Kson
data class UserEntity(
    val firstname: String,
    val lastname: String,
    val roles: List<RoleEntity>
)

// etc
```

Also you can use `@KsonFactory` annotation to generate TypeAdapterFactory class

```kotlin
@KsonFactory
object FactoryProvider {

    get() = KsonFactoryProvider()

}

val gson = GsonBuilder()
    .registerTypeAdapterFactory(FactoryProvider.get())
    .create()

// gson.fromJson(...)
```

## Limitations & Known issues

Since this is an early version there are some unsupported properties

```kotlin
@Kson
data class UnsupportedDataClass(
    @JsonAdapter(CustomAdapter::class) val id: String // custom type adapter
    val name: String = "no name" // default values
    val list: List<String?> // nullable generics
    val `natural name`: String // "natural names"
)
```

## Installation

To add KSON to your project, add the following to your module's `build.gradle`:

```groovy
repositories {
    jcenter()
}

dependencies {
    compile 'dev.afanasev:kson-annotation:<version>'   
    kapt 'dev.afanasev:kson-processor:<version>'
}
```

## Mentions

- [Medium](https://medium.com/@jokuskay/kson-auto-generate-gson-adapters-for-kotlin-data-classes-17af43b6c267)
- [Android Weekly](https://androidweekly.net/issues/issue-365)
- [Habr](https://habr.com/ru/company/digital-ecosystems/blog/459062/)

## Code of Conduct

Please refer to [Code of Conduct](CODE_OF_CONDUCT.md) document.
