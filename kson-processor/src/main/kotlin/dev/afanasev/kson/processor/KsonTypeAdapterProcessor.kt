package dev.afanasev.kson.processor

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.afanasev.kson.annotation.Default
import dev.afanasev.kson.annotation.Kson
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import org.jetbrains.annotations.Nullable
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

/**
 * Generates a Gson [TypeAdapter] for all [Kson] annotated classes.
 */
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.ISOLATING)
@AutoService(Processor::class)
class KsonTypeAdapterProcessor : KsonProcessor() {

    override fun getSupportedAnnotationTypes() = setOf(Kson::class.java.name)

    override fun generate(roundEnv: RoundEnvironment) {
        val filer = processingEnv.filer
        roundEnv.ksonAnnotatedClasses.forEach {
            val adapterClass = it.asClassName()
            val adapterSpec = generateTypeAdapter(it)
            FileSpec.get(adapterClass.packageName, adapterSpec).writeTo(filer)
        }
    }

    /**
     * Generates a Gson [TypeAdapter]
     */
    private fun generateTypeAdapter(clazz: TypeElement): TypeSpec {
        log("generating a type adapter for ${clazz.simpleName}...")

        val properties = clazz.enclosedElements
            .asSequence()
            .filter { it.kind == ElementKind.FIELD && !it.modifiers.contains(Modifier.STATIC) }
            .map { it as VariableElement }
            .map {
                val serializedNameAnnotation = it.getAnnotation(SerializedName::class.java)
                val key = serializedNameAnnotation?.value ?: it.simpleName
                val alternateKeys = serializedNameAnnotation?.alternate?.toSet() ?: emptySet()
                val name = it.simpleName
                val type = it.asType().asTypeName()
                val nullable = it.getAnnotation(Nullable::class.java) != null

                KProperty(
                    key = key.toString(),
                    alternateKeys = alternateKeys,
                    name = name.toString(),
                    type = type,
                    nullable = nullable
                )
            }
            .toList()

        val typeAdapterBuilder = TypeSpec
            .classBuilder(getTypeAdapterClassName(clazz.asClassName()))
            .addOriginatingElement(clazz)
            .addAnnotation(getGeneratedAnnotation())
            .superclass(TypeAdapter::class.parameterizedBy(clazz))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(GSON, Gson::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder(GSON, Gson::class)
                    .initializer(GSON)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        // init properties
        properties
            .distinctBy {
                it.adapterName
            }
            .forEach {
                val initializer = CodeBlock.builder()
                val type = with(ParameterizedTypeName.Companion) {
                    TypeAdapter::class.asClassName().parameterizedBy(it.type)
                }

                if (it.type is ParameterizedTypeName) {
                    initializer.add("@%T(\"UNCHECKED_CAST\") ", Suppress::class)
                    initializer.add("%L.getAdapter(", GSON)
                    getParameterizedTypeToken(initializer, it.type)
                    initializer.add(") as %T", type.javaToKotlinType())
                } else {
                    initializer.add("%L.getAdapter(%T::class.javaObjectType)", GSON, it.type.javaToKotlinType())
                }

                typeAdapterBuilder.addProperty(
                    PropertySpec.builder(it.adapterName, type.javaToKotlinType(), KModifier.PRIVATE)
                        .delegate("lazy(LazyThreadSafetyMode.NONE) { %L }", initializer.build())
                        .build()
                )
            }

        // add write() function
        typeAdapterBuilder.addFunction(generateWriteFunction(clazz, properties))

        // add read() function
        typeAdapterBuilder.addFunction(generateReadFunction(clazz, properties))

        return typeAdapterBuilder.build()
    }

    /**
     * Generates write() function
     */
    private fun generateWriteFunction(clazz: TypeElement, properties: List<KProperty>): FunSpec {
        val writeFunc = FunSpec.builder("write")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(WRITER, JsonWriter::class)
            .addParameter(OBJECT, clazz.asClassName().asNullable())

        //region if
        writeFunc.beginControlFlow("if (%L == null)", OBJECT)
        writeFunc.addStatement("%L.nullValue()", WRITER)
        writeFunc.addStatement("return")
        writeFunc.endControlFlow()
        //endregion if

        writeFunc.addStatement("%L.beginObject()", WRITER)
        properties.forEach {
            writeFunc.addStatement("%L.name(%S)", WRITER, it.key)
            writeFunc.addStatement("%L.write(%L, %L.%L)", it.adapterName, WRITER, OBJECT, it.name)
        }
        writeFunc.addStatement("%L.endObject()", WRITER)

        return writeFunc.build()
    }

    /**
     * Generates read() function
     */
    private fun generateReadFunction(clazz: TypeElement, properties: List<KProperty>): FunSpec {
        val readFunc = FunSpec.builder("read")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(READER, JsonReader::class.java)
            .returns(clazz.asClassName().asNullable())

        readFunc.beginControlFlow("if (%L.peek() == %T.NULL)", READER, JsonToken::class)
        readFunc.addStatement("%L.nextNull()", READER)
        readFunc.addStatement("return null")
        readFunc.endControlFlow()

        val defaultValueProvider = clazz.enclosedElements
            .find { it.simpleName.contentEquals("Companion") && it.kind == ElementKind.CLASS }
            ?.enclosedElements
            ?.find { it.getAnnotation(Default::class.java) != null }

        properties.forEach {
            readFunc.addStatement("var ${it.key}: %L = null", it.type.javaToKotlinType().asNullable())
            if (defaultValueProvider != null) {
                readFunc.addStatement("var ${it.key}Assigned = false")
            }
        }

        readFunc.addStatement("%L.beginObject()", READER)

        //region while
        readFunc.beginControlFlow("while (%L.hasNext())", READER)

        //region if
        readFunc.beginControlFlow("if (%L.peek() == %T.NULL)", READER, JsonToken::class)
        readFunc.addStatement("%L.nextNull()", READER)
        readFunc.addStatement("continue")
        readFunc.endControlFlow()
        //endregion if

        //region when
        readFunc.beginControlFlow("when (%L.nextName())", READER)
        properties.forEach {
            val allKeys: Set<String> = setOf(it.key) + it.alternateKeys
            val keysFormat = allKeys.joinToString(separator = ", ") { "%S" }
            readFunc.addStatement("$keysFormat -> {", *allKeys.toTypedArray())
            readFunc.addStatement("  %L = %L.read(%L)", it.key, it.adapterName, READER)
            if (defaultValueProvider != null) {
                readFunc.addStatement("  %L = true", it.key + "Assigned")
            }
            readFunc.addStatement("}")
        }
        readFunc.addStatement("else -> %L.skipValue()", READER)
        readFunc.endControlFlow()
        //endregion when

        readFunc.endControlFlow()
        //endregion while

        readFunc.addStatement("%L.endObject()", READER)

        if (defaultValueProvider != null) {
            readFunc.addStatement("val default${clazz.simpleName} = ${clazz.simpleName}.${defaultValueProvider.simpleName}()")
            readFunc.addStatement("return %T(", clazz.asType())
            properties.forEach { field ->
                readFunc.addStatement(
                    "%L = if (%L) %L%L else %L.%L,",
                    field.name,
                    field.key + "Assigned",
                    field.key,
                    if (field.nullable) "" else "!!",
                    "default" + clazz.simpleName,
                    field.key,
                )
            }
            readFunc.addStatement(")")
        } else {
            readFunc.addStatement("return %T(", clazz.asType())
            properties.forEach { field ->
                readFunc.addStatement(
                    "%L = %L%L,",
                    field.name,
                    field.key,
                    if (field.nullable) "" else "!!",
                )
            }
            readFunc.addStatement(")")
        }

        return readFunc.build()
    }

    private fun getParameterizedTypeToken(
        codeBlock: CodeBlock.Builder,
        type: ParameterizedTypeName,
        asType: Boolean = false,
    ) {
        codeBlock.add("%T.getParameterized(%T::class.javaObjectType", TypeToken::class, type.rawType.javaToKotlinType())

        type.typeArguments.forEach {
            codeBlock.add(", ")

            if (it is ParameterizedTypeName) {
                getParameterizedTypeToken(codeBlock, it, true)
            } else {
                codeBlock.add("%T::class.javaObjectType", it.javaToKotlinType())
            }
        }

        codeBlock.add(")")
        if (asType) {
            codeBlock.add(".getType()")
        }
    }

    private fun TypeName.javaToKotlinType(): TypeName {
        return if (this is ParameterizedTypeName) {
            (rawType.javaToKotlinType() as ClassName).parameterizedBy(
                *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
            )
        } else {
            val className =
                JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()

            return if (className == null) {
                this
            } else {
                ClassName.bestGuess(className)
            }
        }
    }
}
