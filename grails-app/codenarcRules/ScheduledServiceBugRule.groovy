import groovy.transform.*
import org.codehaus.groovy.ast.*
import org.codenarc.rule.*

@CompileStatic
class ScheduledServiceBugRule extends AbstractAstVisitorRule {
    String applyToFileNames = "*/*grails-app/services*/*"
    int priority = 1
    String name = 'ScheduledServiceBug'
    String description = 'Due to a Bug, the Annotation does not function Reliably. See also OTP-2926'
    Class astVisitorClass = ScheduledServiceBugVisitor
}

@CompileStatic
class ScheduledServiceBugVisitor extends AbstractAstVisitor {

    private static final String SPRING_SCHEDULED_ANNOTATION = 'Scheduled'
    private static final String ERROR_MSG = 'Do not use @Scheduled, in Services.'

    @Override
    void visitAnnotations(AnnotatedNode node) {
        node.annotations.each { AnnotationNode annotationNode ->
            String annotation = annotationNode.classNode.text
            if (annotation == SPRING_SCHEDULED_ANNOTATION) {
                addViolation(node, ERROR_MSG)
            }
        }

        super.visitAnnotations(node)
    }
}