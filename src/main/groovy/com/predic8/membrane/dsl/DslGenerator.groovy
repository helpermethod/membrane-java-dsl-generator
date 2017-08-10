package com.predic8.membrane.dsl

import com.predic8.membrane.annot.MCAttribute
import com.predic8.membrane.annot.MCElement
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import groovy.transform.CompileStatic
import org.reflections.Reflections

import static com.squareup.javapoet.JavaFile.builder
import static com.squareup.javapoet.TypeSpec.classBuilder
import static java.nio.file.Paths.get
import static javax.lang.model.element.Modifier.PUBLIC
import static org.reflections.ReflectionUtils.getAllMethods
import static org.reflections.ReflectionUtils.withAnnotation

@CompileStatic
class DslGenerator {
    static void main(String[] args) {
        def reflections = new Reflections('com.predic8.membrane.core.interceptor')
        def interceptors = reflections.getTypesAnnotatedWith MCElement

        interceptors.each {
            def name = it.getAnnotation(MCElement).name()

            getAllMethods(it, withAnnotation(MCAttribute)).each {

            }

            def specification = classBuilder("${it.getSimpleName()}Specification").addModifiers(PUBLIC).build()
            def javaFile = builder("com.predic8.dsl", specification)
                    .build()

            javaFile.writeTo(get('build/src'))
        }
    }
}