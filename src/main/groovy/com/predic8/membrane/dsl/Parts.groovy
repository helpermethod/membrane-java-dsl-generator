package com.predic8.membrane.dsl

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import groovy.transform.CompileStatic

@CompileStatic
class Parts {
    final String name
    final FieldSpec field
    final MethodSpec constructor
    final List<MethodSpec> methods

    Parts(String name, FieldSpec field, MethodSpec constructor, List<MethodSpec> methods) {
        this.name = name
        this.field = field
        this.constructor = constructor
        this.methods = methods
    }
}