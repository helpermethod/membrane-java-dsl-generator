package com.predic8.membrane.dsl

import com.predic8.membrane.annot.MCAttribute
import com.predic8.membrane.annot.MCElement
import com.predic8.membrane.core.interceptor.AbstractInterceptor
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.ParameterSpec
import groovy.transform.CompileStatic
import org.reflections.Reflections

import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

import static com.squareup.javapoet.MethodSpec.constructorBuilder
import static com.squareup.javapoet.MethodSpec.methodBuilder
import static com.squareup.javapoet.TypeSpec.classBuilder
import static java.lang.reflect.Modifier.FINAL
import static java.lang.reflect.Modifier.PRIVATE
import static java.nio.file.Paths.get
import static javax.lang.model.element.Modifier.FINAL
import static javax.lang.model.element.Modifier.PRIVATE
import static javax.lang.model.element.Modifier.PUBLIC
import static org.reflections.ReflectionUtils.*

@CompileStatic
class DslGenerator {
    static void main(String[] args) {
        def reflections = new Reflections('com.predic8.membrane.core.interceptor')
        def interceptors = reflections.getSubTypesOf(AbstractInterceptor).findAll { it.isAnnotationPresent(MCElement) }

        interceptors.each { interceptor ->
            def interceptorName = interceptor.simpleName.uncapitalize()
            def constructor = constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(interceptor, interceptorName)
                    .addStatement("this.${interceptorName} = ${interceptorName}")
                    .build()
            def methods = getAllMethods(interceptor, withAnnotation(MCAttribute), withPrefix('set')).collect { method ->
                def parameters = method.parameters.collect { Parameter parameter ->
                    ParameterSpec
                            .builder(parameter.type, parameter.name)
                            .build()
                }
                def methodBuilder = methodBuilder((method.name - ~/^set/).uncapitalize())
                        .addModifiers(PUBLIC)
                parameters.each {
                    methodBuilder
                            .addParameter(it)
                            .addStatement("${interceptorName}.${method.name}($it.name)")
                }
                methodBuilder.build()
            }

            def field = FieldSpec.builder(interceptor, interceptorName, PRIVATE, FINAL).build()
            def specificationBuilder = classBuilder("${interceptor.simpleName}Specification")
                    .addModifiers(PUBLIC)
                    .addField(field)
                    .addMethod(constructor)

            methods.each { method -> specificationBuilder.addMethod(method) }

            def specification = specificationBuilder.build()
            def javaFile = JavaFile.builder('com.predic8.membrane.dsl', specification)
                    .indent(' ' * 4)
                    .build()

            javaFile.writeTo(get('build/generated'))
        }
    }
}