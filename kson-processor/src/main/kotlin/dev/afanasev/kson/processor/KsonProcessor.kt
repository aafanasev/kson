package dev.afanasev.kson.processor

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import dev.afanasev.kson.annotation.Kson
import javax.annotation.Generated
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

private const val REPOSITORY_URL = "https://github.com/aafanasev/kson"

// variables
internal const val GSON = "gson"
internal const val WRITER = "writer"
internal const val READER = "reader"
internal const val OBJECT = "obj"
internal const val TYPE = "type"

/**
 * Base class for other Kson processors.
 */
abstract class KsonProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        generate(roundEnv)
        return false
    }

    protected abstract fun generate(roundEnv: RoundEnvironment)

    protected fun log(msg: String, kind: Diagnostic.Kind = Diagnostic.Kind.NOTE) {
        processingEnv.messager.printMessage(kind, "${javaClass.name}: $msg")
    }

    protected fun getGeneratedAnnotation() = AnnotationSpec.builder(Generated::class.java)
            .addMember("value = [%S]", javaClass.name)
            .addMember("comments = %S", REPOSITORY_URL)
            .build()

    protected fun getTypeAdapterClassName(className: ClassName): String = "${className.simpleName}TypeAdapter"

    protected val RoundEnvironment.ksonAnnotatedClasses: Sequence<TypeElement>
        get() {
            return getElementsAnnotatedWith(Kson::class.java)
                    .asSequence()
                    .filter { it.kind == ElementKind.CLASS }
                    .filterNot { processingEnv.elementUtils.getPackageOf(it).isUnnamed }
                    .map { it as TypeElement }
        }
}
