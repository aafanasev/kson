package com.aafanasev.kson.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal data class KProperty(
        val key: String,
        val name: String,
        val type: TypeName,
        val nullable: Boolean
) {
    val adapterName = typeToAdapterName(type)
}

internal fun typeToAdapterName(type: TypeName): String {
    val names = mutableListOf<String>()

    if (type is ParameterizedTypeName) {
        names.addAll(type.getClassNamesRecursively())
    } else {
        names.add((type as ClassName).simpleName())
    }

    names.add("adapter")

    return names.joinToString(separator = "_").toLowerCase()
}

private fun ParameterizedTypeName.getClassNamesRecursively(): List<String> {
    val names = mutableListOf<String>()
    names.add(rawType.simpleName())

    typeArguments.forEach {
        if (it is ParameterizedTypeName) {
            names.addAll(it.getClassNamesRecursively())
        } else {
            names.add((it as ClassName).simpleName())
        }
    }

    return names
}
