package com.aafanasev.kson.processor

import com.aafanasev.kson.annotation.Kson
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.squareup.kotlinpoet.*
import org.jetbrains.annotations.Nullable
import java.io.File
import javax.annotation.Generated
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic
import kotlin.reflect.jvm.internal.impl.name.FqName
import kotlin.reflect.jvm.internal.impl.platform.JavaToKotlinClassMap

private const val PACKAGE = "com.aafanasev.kson.generated"
private const val FILENAME = "KsonTypeAdapters.kt"

private const val REPOSITORY_URL = "https://github.com/aafanasev/kson"
private const val FACTORY_CLASS_NAME = "KsonTypeAdapterFactory"

// variables
private const val GSON = "gson"
private const val WRITER = "writer"
private const val READER = "reader"
private const val OBJECT = "obj"
private const val TYPE = "type"

/**
 * Generates a Gson [TypeAdapter] and [TypeAdapterFactory] for all [Kson] annotated classes.
 */
class KsonProcessor : AbstractProcessor() {

    private lateinit var messager: Messager
    private lateinit var elementUtils: Elements

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        messager = processingEnv.messager
        elementUtils = processingEnv.elementUtils
    }

    override fun getSupportedAnnotationTypes() = setOf(Kson::class.java.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!roundEnv.processingOver()) {
            generate(roundEnv)
        }

        return true
    }

    private fun generate(roundEnv: RoundEnvironment) {
        log("generating...")

        val fileBuilder = FileSpec.builder(PACKAGE, FILENAME)

        val classes = roundEnv.getElementsAnnotatedWith(Kson::class.java)
                .asSequence()
                .filter { it.kind == ElementKind.CLASS }
                .filter { elementUtils.getPackageOf(it).isUnnamed.not() }
                .map { it as TypeElement }
                .onEach { fileBuilder.addType(generateTypeAdapter(it)) }
                .map { it.asClassName() }
                .toList()

        if (classes.isNotEmpty()) {
            fileBuilder.addType(generateTypeAdapterFactory(classes))

            val dir = processingEnv.options["kapt.kotlin.generated"]
            val file = fileBuilder.build()

            File(dir, file.name).apply {
                parentFile.mkdirs()
                writeText(file.toString())
            }
        }
    }

    /**
     * Generates a Gson [TypeAdapter]
     */
    private fun generateTypeAdapter(clazz: TypeElement): TypeSpec {
        log("generating a type adapter for ${clazz.simpleName}...")

        val properties = clazz.enclosedElements
                .asSequence()
                .filter { it.kind == ElementKind.FIELD }
                .map { it as VariableElement }
                .map {
                    val key = it.getAnnotation(SerializedName::class.java)?.value ?: it.simpleName
                    val name = it.simpleName
                    val type = it.asType().asTypeName()
                    val nullable = it.getAnnotation(Nullable::class.java) != null

                    KProperty(key.toString(), name.toString(), type, nullable)
                }
                .toList()

        val typeAdapterBuilder = TypeSpec
                .classBuilder(getTypeAdapterClassName(clazz.asClassName()))
                .addAnnotation(getGeneratedAnnotation())
                .superclass(
                        ParameterizedTypeName.get(TypeAdapter::class.asTypeName(), clazz.asClassName())
                )
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
        properties.forEach {
            val initializer = CodeBlock.builder()
            val type = ParameterizedTypeName.get(TypeAdapter::class.asClassName(), it.type)

            if (it.type is ParameterizedTypeName) {
                getParameterizedTypeToken(initializer, it.type)
                initializer.add(" as %T", type.javaToKotlinType())
            } else {
                initializer.add("%L.getAdapter(%T::class.java)", GSON, it.type.javaToKotlinType())
            }

            typeAdapterBuilder.addProperty(
                    PropertySpec.builder(it.adapterName, type.javaToKotlinType(), KModifier.PRIVATE)
                            .delegate("lazy { %L }", initializer.build())
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

        properties.forEach {
            readFunc.addStatement("var ${it.key}: %L = null", it.type.javaToKotlinType().asNullable())
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
            readFunc.addStatement("%S -> %L = %L.read(%L)", it.key, it.key, it.adapterName, READER)
        }
        readFunc.addStatement("else -> %L.skipValue()", READER)
        readFunc.endControlFlow()
        //endregion when

        readFunc.endControlFlow()
        //endregion while

        readFunc.addStatement("%L.endObject()", READER)

        readFunc.addStatement("return %T(%>", clazz.asType())

        properties.forEachIndexed { index, field ->
            readFunc.addStatement("%L = %L%L%L",
                    field.name,
                    field.key,
                    if (field.nullable) "" else "!!",
                    if (index == properties.size - 1) "" else ","
            )
        }

        readFunc.addStatement("%<)")

        return readFunc.build()
    }

    /**
     * Generates a Gson [TypeAdapterFactory]
     */
    private fun generateTypeAdapterFactory(typeAdapters: List<ClassName>): TypeSpec {
        log("generating a factory...")

        val factoryBuilder = TypeSpec
                .classBuilder(FACTORY_CLASS_NAME)
                .addAnnotation(getGeneratedAnnotation())
                .addSuperinterface(TypeAdapterFactory::class)

        val generic = TypeVariableName.invoke("T")
        val returnType = ParameterizedTypeName.get(TypeAdapter::class.asClassName(), generic)

        val createMethod = FunSpec.builder("create")
                .addModifiers(KModifier.OVERRIDE)
                .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings::class)
                                .addMember("%S", "unchecked")
                                .build()
                )
                .addTypeVariable(generic)
                .addParameter(GSON, Gson::class)
                .addParameter(TYPE, ParameterizedTypeName.get(TypeToken::class.asClassName(), generic))
                .returns(returnType.asNullable())

        val resultVarName = "typeAdapter"

        createMethod.addStatement("val %L = when {%>", resultVarName)

        typeAdapters.forEach {
            createMethod.addStatement("%T::class.java.isAssignableFrom(%L.rawType) -> %L(%L)", it, TYPE, getTypeAdapterClassName(it), GSON)
        }

        createMethod.addStatement("else -> null")
        createMethod.addStatement("%<}")

        createMethod.addStatement("return %L as? %T", resultVarName, returnType)

        factoryBuilder.addFunction(createMethod.build())

        return factoryBuilder.build()
    }

    private fun log(msg: String, kind: Diagnostic.Kind = Diagnostic.Kind.NOTE) {
        messager.printMessage(kind, "${javaClass.name}: $msg")
    }

    private fun getGeneratedAnnotation() = AnnotationSpec.builder(Generated::class.java)
            .addMember("value = [%S]", javaClass.name)
            .addMember("comments = %S", REPOSITORY_URL)
            .build()

    private fun getTypeAdapterClassName(className: ClassName): String = "${className.simpleName()}TypeAdapter"

    private fun getParameterizedTypeToken(codeBlock: CodeBlock.Builder, type: ParameterizedTypeName, asType: Boolean = false) {
        codeBlock.add("%T.getParameterized(%T::class.java", TypeToken::class, type.rawType.javaToKotlinType())

        type.typeArguments.forEach {
            codeBlock.add(", ")

            if (it is ParameterizedTypeName) {
                getParameterizedTypeToken(codeBlock, it, true)
            } else {
                codeBlock.add("%T::class.java", it.javaToKotlinType())
            }
        }

        codeBlock.add(")")
        if (asType) {
            codeBlock.add(".getType()")
        }
    }

    private fun TypeName.javaToKotlinType(): TypeName {
        return if (this is ParameterizedTypeName) {
            ParameterizedTypeName.get(
                    rawType.javaToKotlinType() as ClassName,
                    *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
            )
        } else {
            val className = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()

            return if (className == null) {
                this
            } else {
                ClassName.bestGuess(className)
            }
        }
    }

}