import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.transform.trait.Traits

import java.lang.reflect.Modifier

trait IsAnnotationVisitor {

    boolean isNoOrdinaryClass(ClassNode node) {
        return Modifier.isAbstract(node.modifiers) || Modifier.isInterface(node.modifiers) || isEnum(node.modifiers) || Traits.isTrait(node)
    }
    String buildErrorString(String value) {
        "Missing ${value} Annotation."
    }

    boolean isEnum(int mod) {
        return (mod & 16384) != 0
    }
}
