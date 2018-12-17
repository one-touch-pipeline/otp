import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.transform.trait.Traits
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

import java.lang.reflect.Modifier

@CompileStatic
class AnnotationsForStartJobsRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*"
    String applyToFileNames = "*/*StartJob.groovy"
    int priority = 1
    String name = 'AnnotationsForStartJobs'
    String description = 'Ensures that StartJobs have the Proper Annotations.'
    Class astVisitorClass = AnnotationsForStartJobsVisitor
}

@CompileStatic
class AnnotationsForStartJobsVisitor extends AbstractAstVisitor {

    @Override
    void visitClassEx(ClassNode node) {
        if (Modifier.isAbstract(node.modifiers) || Modifier.isInterface(node.modifiers) || isEnum(node.modifiers) || Traits.isTrait(node)) {
            return
        }

        boolean hasComponent = false
        boolean hasScope = false

        node.annotations.each { AnnotationNode annotationNode ->
            switch (annotationNode.classNode.text) {
                case 'Component':
                    hasComponent = true
                    if (!annotationNode.members['value']?.text) {
                        addViolation(node, "The @Component annotation is missing a value.")
                    }
                    break
                case 'Scope':
                    hasScope = true
                    if (annotationNode.members['value']?.text != 'singleton') {
                        addViolation(node, "The @Scope annotation is missing the value 'singleton'.")
                    }
                    break
                default:
                    return
            }
        }

        if (!hasComponent) {
            addViolation(node, buildErrorString("@Component"))
        }
        if (!hasScope) {
            addViolation(node, buildErrorString("@Scope"))
        }
    }

    private static String buildErrorString(String value) {
        "Missing ${value} Annotation."
    }

    private static boolean isEnum(int mod) {
        return (mod & 16384) != 0
    }
}
