import com.predic8.membrane.annot.MCAttribute
import com.predic8.membrane.annot.MCChildElement
import com.predic8.membrane.annot.MCElement
import com.predic8.membrane.core.interceptor.Interceptor
import com.predic8.membrane.core.interceptor.LogInterceptor
import com.predic8.membrane.core.rules.ServiceProxy
import org.reflections.Reflections
import spock.lang.Specification

import static org.reflections.ReflectionUtils.getAllMethods
import static org.reflections.ReflectionUtils.withAnnotation

class ReflectionsSpecification extends Specification {
    def 'Reflections should return all types annotated with MCElement'() {
        given:
        def reflections = new Reflections('com.predic8.membrane.core')
        when:
        def types = reflections.getTypesAnnotatedWith(MCElement, false)
        then:
        types.size() == 144
    }

    def 'Reflections should return all methods of a class annotated with MCAttribute'() {
        when:
        def methods = getAllMethods(LogInterceptor, withAnnotation(MCAttribute))
        then:
        methods.size() == 4
    }

    def 'Reflections should return all methods annotated with MCChildElement'() {
        when:
        def methods = getAllMethods(ServiceProxy, withAnnotation(MCChildElement))
        then:
        methods.size() == 4
    }

    def 'Reflections should return all sub types of a type'() {
        given:
        def reflections = new Reflections('com.predic8.membrane.core')
        when:
        def subTypes = reflections.getSubTypesOf(Interceptor)
        then:
        subTypes.size() == 71
    }
}