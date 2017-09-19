package com.predic8.membrane.dsl

import com.predic8.membrane.annot.MCAttribute
import com.predic8.membrane.annot.MCChildElement
import com.predic8.membrane.annot.MCElement
import com.predic8.membrane.core.interceptor.cbr.Case
import com.predic8.membrane.core.interceptor.cbr.XPathCBRInterceptor
import com.squareup.javapoet.*
import groovy.transform.CompileStatic
import org.reflections.Reflections

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.nio.file.Paths
import java.util.function.Consumer

import static com.squareup.javapoet.MethodSpec.constructorBuilder
import static com.squareup.javapoet.MethodSpec.methodBuilder
import static com.squareup.javapoet.TypeSpec.classBuilder
import static javax.lang.model.element.Modifier.*
import static org.reflections.ReflectionUtils.getAllMethods
import static org.reflections.ReflectionUtils.withAnnotation

@CompileStatic
class Dsl {
    private final Reflections reflections
    private final Map<String, String> replacements

    Dsl(Reflections reflections, Map<String, String> replacements) {
        this.reflections = reflections
        this.replacements = replacements
    }

    void generate() {
        reflections
            .getTypesAnnotatedWith(MCElement, true)
            .minus([XPathCBRInterceptor, Case])
            .collect(this.&generateClass << this.&generateParts)
            .each(this.&generateJavaFile)
    }

    private Parts generateParts(Class<?> type) {
        def name = type.simpleName.uncapitalize()
        def identifier = replacements.get(name, name)
        def field = FieldSpec.builder(type, identifier, PRIVATE, FINAL).build()

        new Parts("${type.simpleName}Spec", field, generateConstructor(type, field), generateSimpleSetters(type) + generateComplexSetters(type, field))
    }

    private MethodSpec generateConstructor(Class<?> type, FieldSpec field) {
        constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(type, field.name)
            .addStatement("this.\$N = \$N", field, field)
            .build()
    }

    private TypeSpec generateClass(Parts parts) {
        classBuilder(parts.name)
            .addModifiers(PUBLIC)
            .addField(parts.field)
            .addMethod(parts.constructor)
            .addMethods(parts.methods)
            .build()
    }

    private void generateJavaFile(TypeSpec specification) {
        JavaFile.builder('com.predic8.membrane.dsl', specification)
                .indent(' ' * 4)
                .build()
                .writeTo(Paths.get('build/generated'))
    }

    private List<MethodSpec> generateSimpleSetters(Class<?> type) {
        getAllMethods(type, withAnnotation(MCAttribute)).collect { method ->
            def methodName = (method.name - ~/^set/).uncapitalize()
            def validMethodName = replacements.get(methodName, methodName)
            def parameter = method.parameters.first()

            methodBuilder(validMethodName)
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(parameter.type, validMethodName).build())
                .addStatement("this.${method.name}(${parameter.name})")
                .addStatement('return this')
                .returns(type)
                .build()
        }
    }

    private List<MethodSpec> generateComplexSetters(Class<?> type, FieldSpec field) {
        getAllMethods(type, withAnnotation(MCChildElement)).collect { Method method ->
            def methodName = (method.name - ~/^set/).uncapitalize()
            def parameter = method.parameters.first()
            def methodBuilder = methodBuilder(replacements.get(methodName, methodName))
                .addModifiers(PUBLIC)

            if (!(parameter.parameterizedType instanceof ParameterizedType)) {
                def lowerCaseParameterName = parameter.type.simpleName.uncapitalize()
                def name = replacements.get(lowerCaseParameterName, lowerCaseParameterName)
                def specConsumer = ParameterizedTypeName.get(
                    ClassName.get(Consumer),
                    ClassName.get('com.predic8.membrane.dsl', "${parameter.type.simpleName}Spec")
                )
                // TODO use name?
                def specParameter = ParameterSpec.builder(specConsumer, parameter.name).build()

                methodBuilder.addParameter(specParameter)
                             .addStatement("\$T $name = new \$T())", parameter.type, parameter.type)
                             .addStatement("\$N.accept($name)", specParameter)
                             .addStatement("\$N.set${parameter.type.simpleName}($name)", field)
            } else {
                def typeArgument = (parameter.parameterizedType as ParameterizedType).actualTypeArguments[0] as Class
                def assignables = reflections.getTypesAnnotatedWith(MCElement, true).findAll {
                    typeArgument.isAssignableFrom(it)
                }

                // TODO
                // create class for type
                // for each subclass
                // create a new method with subclass param
                // create consumer parameter for type

                def names = assignables*.getAnnotation(MCElement)*.name()

                println names
            }

            methodBuilder
                .addStatement('return this')
                .returns(type)
                .build()
        }
    }

    static void main(String[] args) {
        new Dsl(new Reflections('com.predic8.membrane.core'), [do: 'action', private: 'pem']).generate()
    }
}