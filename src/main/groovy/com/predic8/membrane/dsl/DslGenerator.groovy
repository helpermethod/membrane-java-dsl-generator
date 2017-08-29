package com.predic8.membrane.dsl

import com.predic8.membrane.annot.MCAttribute
import com.predic8.membrane.annot.MCChildElement
import com.predic8.membrane.annot.MCElement
import com.predic8.membrane.core.interceptor.cbr.Case
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import groovy.transform.CompileStatic
import org.reflections.Reflections

import java.lang.reflect.Method
import java.lang.reflect.Parameter

import static com.squareup.javapoet.FieldSpec.builder as fieldBuilder
import static com.squareup.javapoet.MethodSpec.constructorBuilder
import static com.squareup.javapoet.ParameterSpec.builder as parameterBuilder
import static com.squareup.javapoet.MethodSpec.methodBuilder
import static com.squareup.javapoet.TypeSpec.classBuilder
import static java.nio.file.Paths.get
import static javax.lang.model.element.Modifier.FINAL
import static javax.lang.model.element.Modifier.PRIVATE
import static javax.lang.model.element.Modifier.PUBLIC
import static org.reflections.ReflectionUtils.getAllMethods
import static org.reflections.ReflectionUtils.withAnnotation

@CompileStatic
class DslGenerator {
    static void main(String[] args) {
        new Reflections('com.predic8.membrane.core')
            .getTypesAnnotatedWith(MCElement, false)
            .minus([Case])
            .collect { generateParts(it, [Private: 'Pem']) }
            .collect(this.&generateClass)
            .each(this.&generateJavaFile)
    }

    private static <T> Parts generateParts(Class<T> type, Map<String, String> replacements) {
        def identifier = replacements.get(type.simpleName, type.simpleName).uncapitalize()

        new Parts("${type.simpleName}Specification", generateField(type, identifier), generateConstructor(type, identifier), generateMethods(type))
    }

    private static FieldSpec generateField(Class<?> type, String identifier) {
        fieldBuilder(type, identifier, PRIVATE, FINAL).build()
    }

    private static MethodSpec generateConstructor(Class<?> type, String identifier) {
        constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(type, identifier)
            .addStatement("this.${identifier} = ${identifier}")
            .build()
    }

    private static TypeSpec generateClass(Parts parts) {
        return classBuilder(parts.name)
            .addModifiers(PUBLIC)
            .addField(parts.field)
            .addMethod(parts.constructor)
            .addMethods(parts.methods)
            .build()
    }

    private static void generateJavaFile(TypeSpec specification) {
        def javaFile = JavaFile.builder('com.predic8.membrane.dsl', specification)
                               .indent(' ' * 4)
                               .build()

        javaFile.writeTo(get('build/generated'))
    }

    private static List<MethodSpec> generateMethods(Class<?> type) {
        getAllMethods(type, withAnnotation(MCAttribute))
            .collect { generateSimpleSetter(type, it, ['do': 'action']) }
        getAllMethods(type, withAnnotation(MCChildElement))
            .collect { generateComplexSetter(type, it)}
    }

    private static MethodSpec generateComplexSetter(Class<?> type, Method method) {

    }

    private static MethodSpec generateSimpleSetter(Class<?> type, Method method, Map<String, String> replacements) {
        def identifier = (method.name - ~/^set/).uncapitalize()
        def methodBuilder = methodBuilder(replacements.get(identifier, identifier)).addModifiers(PUBLIC)

        generateParameters(method).each {
            methodBuilder.addParameter(it)
                         .addStatement("this.${method.name}(${it.name})")
        }

        methodBuilder.addStatement('return this')
                     .returns(type)
                     .build()
    }

    private static List<ParameterSpec> generateParameters(Method method) {
        method.parameters.collect { Parameter parameter ->
            parameterBuilder(parameter.type, parameter.name).build()
        }
    }
}