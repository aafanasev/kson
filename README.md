[![Download](https://api.bintray.com/packages/aafanasev/maven/kson-processor/images/download.svg)](https://bintray.com/aafanasev/maven/kson-processor/_latestVersion) 
[![CircleCI](https://circleci.com/gh/aafanasev/kson.svg?style=shield)](https://circleci.com/gh/aafanasev/kson) 
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

Add `@Kson` annotation to your data classes

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

Register generated TypeAdapterFactory

```kotlin
import dev.afanasev.kson.generated.KsonTypeAdapterFactory // generated factory

val gson = GsonBuilder()
    .registerTypeAdapterFactory(KsonTypeAdapterFactory())
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
