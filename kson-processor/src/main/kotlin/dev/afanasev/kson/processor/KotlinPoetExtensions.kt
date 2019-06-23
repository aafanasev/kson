package dev.afanasev.kson.processor

import com.squareup.kotlinpoet.*
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

fun ClassName.asNullable() = copy(nullable = true)

fun TypeName.asNullable() = copy(nullable = true)

fun KClass<*>.parameterizedBy(typeElement: TypeElement): ParameterizedTypeName {
    return with(ParameterizedTypeName.Companion) {
        asClassName().parameterizedBy(typeElement.asClassName())
    }
}

fun KClass<*>.parameterizedBy(typeVariableName: TypeVariableName): ParameterizedTypeName {
    return with(ParameterizedTypeName.Companion) {
        asClassName().parameterizedBy(typeVariableName)
    }
}

fun KClass<*>.parameterizedBy(clazz: KClass<*>, vararg moreClasses: KClass<*>): ParameterizedTypeName {
    return with(ParameterizedTypeName.Companion) {
        moreClasses.fold(plusParameter(clazz)) { parameterized, clazz -> parameterized.plusParameter(clazz) }
    }
}

fun ClassName.parameterizedBy(clazz: TypeName, vararg moreClasses: TypeName): ParameterizedTypeName {
    return with(ParameterizedTypeName.Companion) {
        moreClasses.fold(plusParameter(clazz)) { parameterized, clazz -> parameterized.plusParameter(clazz) }
    }
}