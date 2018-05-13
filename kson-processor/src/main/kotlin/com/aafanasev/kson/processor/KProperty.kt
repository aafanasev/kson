package com.aafanasev.kson.processor

import com.squareup.kotlinpoet.TypeName

internal data class KProperty(val key: String,
                              val name: String,
                              val type: TypeName,
                              val nullable: Boolean) {

    val adapterName = "${name}TypeAdapter"

}