import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.transform.trait.Traits
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

import java.lang.reflect.Modifier

@CompileStatic
class AnnotationsForValidatorsRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*"
    String applyToFileNames = "*/*metadatavalidation*/*"
    int priority = 1
    String name = 'AnnotationsForValidators'
    String description = 'Ensures that Validators have the Proper Annotations.'
    Class astVisitorClass = AnnotationsForValidatorsVisitor
}

@CompileStatic
class AnnotationsForValidatorsVisitor extends AbstractAstVisitor {

    @Override
    void visitClassComplete(ClassNode node) {
        if (Modifier.isAbstract(node.modifiers) || Modifier.isInterface(node.modifiers) || isEnum(node.modifiers) || Traits.isTrait(node)) {
            return
        }
        if (!(node.text =~ /^.*Validator$/)) {
            return
        }

        if (node.annotations*.classNode.text.contains('Component')) {
            return
        }
        addViolation(node, "Missing @Component Annotation.")
    }

    private static boolean isEnum(int mod) {
        return (mod & 16384) != 0
    }
}
