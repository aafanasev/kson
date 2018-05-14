# KSON

[ ![Download](https://api.bintray.com/packages/aafanasev/maven/kson-processor/images/download.svg) ](https://bintray.com/aafanasev/maven/kson-processor/_latestVersion) [![CircleCI](https://circleci.com/gh/aafanasev/kson.svg?style=shield)](https://circleci.com/gh/aafanasev/kson)

An annotation processor generates GSON TypeAdapter from Kotlin Data Classes

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
import com.aafanasev.kson.generated.KsonTypeAdapterFactory // generated factory

val gson = GsonBuilder()
    .registerTypeAdapterFactory(KsonTypeAdapterFactory())
    .create()

// gson.fromJson(...)
```

## Limitations

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

## Install

```groovy
// add repository
repositories {
    maven {
        url 'https://dl.bintray.com/aafanasev/maven'
    }
}

// add dependency
compile 'com.aafanasev:kson-annotation:<version>'   
kapt 'com.aafanasev:kson-processor:<version>'
```