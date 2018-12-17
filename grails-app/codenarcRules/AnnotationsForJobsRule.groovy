import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.transform.trait.Traits
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

import java.lang.reflect.Modifier

@CompileStatic
class AnnotationsForJobsRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*,*/*StartJob.groovy,*/*domain*/*"
    String applyToFileNames = "*/*Job.groovy"
    int priority = 1
    String name = 'AnnotationsForJobs'
    String description = 'Ensures that Jobs have the Proper Annotations.'
    Class astVisitorClass = AnnotationsForJobsVisitor
}

@CompileStatic
class AnnotationsForJobsVisitor extends AbstractAstVisitor {

    @Override
    void visitClassEx(ClassNode node) {
        if (Modifier.isAbstract(node.modifiers) || Modifier.isInterface(node.modifiers) || isEnum(node.modifiers) || Traits.isTrait(node)) {
            return
        }

        boolean hasComponent = false
        boolean hasScope = false
        boolean hasJobLog = false

        node.annotations.each { AnnotationNode annotationNode ->
            switch (annotationNode.classNode.text) {
                case 'Component':
                    hasComponent = true
                    break
                case 'Scope':
                    hasScope = true
                    if (annotationNode.members['value']?.text != 'prototype') {
                        addViolation(node, "The @Scope annotation is missing the value 'prototype'")
                    }
                    break
                case 'UseJobLog':
                    hasJobLog = true
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
        if (!hasJobLog) {
            addViolation(node, buildErrorString("@UseJobLog"))
        }
    }

    private static String buildErrorString(String value) {
        "Missing ${value} Annotation"
    }

    private static boolean isEnum(int mod) {
        return (mod & 16384) != 0
    }
}
