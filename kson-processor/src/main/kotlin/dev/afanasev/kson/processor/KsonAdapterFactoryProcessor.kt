package dev.afanasev.kson.processor

import com.google.auto.service.AutoService
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.squareup.kotlinpoet.*
import dev.afanasev.kson.annotation.Kson
import dev.afanasev.kson.annotation.KsonFactory
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * Generates a Gson [TypeAdapterFactory] for all [Kson] annotated classes.
 */
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
@AutoService(Processor::class)
class KsonAdapterFactoryProcessor : KsonProcessor() {

    override fun getSupportedAnnotationTypes() = setOf(
            Kson::class.java.name,
            KsonFactory::class.java.name
    )

    override fun generate(roundEnv: RoundEnvironment) {
        val classes = roundEnv.ksonAnnotatedClasses.toList()
        if (classes.isNotEmpty()) {

            val factories = roundEnv.getElementsAnnotatedWith(KsonFactory::class.java)
                    .filter { it.kind == ElementKind.CLASS }
                    .map { it as TypeElement }

            when (factories.size) {
                0 -> log("Consider using ${KsonFactory::class.qualifiedName} annotation to generate"
                        + " a factory class for all type adapters", Diagnostic.Kind.WARNING)
                1 -> {
                    val factoryClass = factories.first()
                    val factorySpec = generateTypeAdapterFactory(
                            factoryClass = factoryClass,
                            typeAdapterClasses = classes
                    )
                    FileSpec.get(factoryClass.asClassName().packageName, factorySpec).writeTo(processingEnv.filer)
                }
                else -> error("Only one class can be annotated with ${KsonFactory::class.qualifiedName}")
            }
        }
    }

    /**
     * Generates a Gson [TypeAdapterFactory]
     */
    private fun generateTypeAdapterFactory(factoryClass: TypeElement, typeAdapterClasses: List<TypeElement>): TypeSpec {
        log("generating a factory...")

        val factoryBuilder = TypeSpec
                .classBuilder("Kson${factoryClass.simpleName}")
                .addOriginatingElement(factoryClass)
                .addAnnotation(getGeneratedAnnotation())
                .addSuperinterface(TypeAdapterFactory::class)

        val generic = TypeVariableName.invoke("T")
        val returnType = TypeAdapter::class.parameterizedBy(generic).asNullable()

        val createMethod = FunSpec.builder("create")
                .addModifiers(KModifier.OVERRIDE)
                .addAnnotation(
                        AnnotationSpec.builder(Suppress::class)
                                .addMember("%S", "UNCHECKED_CAST")
                                .build()
                )
                .addTypeVariable(generic)
                .addParameter(GSON, Gson::class)
                .addParameter(TYPE, TypeToken::class.parameterizedBy(generic))
                .returns(returnType)

        val resultVarName = "typeAdapter"

        createMethod.addStatement("val %L = when {", resultVarName)

        typeAdapterClasses
                .onEach { factoryBuilder.addOriginatingElement(it) }
                .map { it.asClassName() }
                .forEach {
                    val adapter = ClassName(it.packageName, getTypeAdapterClassName(it))
                    createMethod.addStatement(
                            "%T::class.java.isAssignableFrom(%L.rawType) -> %T(%L)", it, TYPE, adapter, GSON)
                }

        createMethod.addStatement("else -> null")
        createMethod.addStatement("}")

        createMethod.addStatement("return %L as? %T", resultVarName, returnType)

        factoryBuilder.addFunction(createMethod.build())

        return factoryBuilder.build()
    }
}