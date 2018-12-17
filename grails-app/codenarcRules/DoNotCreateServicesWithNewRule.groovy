import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

@CompileStatic
class DoNotCreateServicesWithNewRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*"
    int priority = 1
    String name = 'DoNotCreateServicesWithNew'
    String description = 'The Services will be autowired by Spring.' +
            'Creating one with \'new\' is not allowed since it leads to unpredictable and undesired behaviour.'
    Class astVisitorClass = DoNotCreateServicesWithNewVisitor
}

@CompileStatic
class DoNotCreateServicesWithNewVisitor extends AbstractAstVisitor {

    private static final String ERROR_MSG = 'Do not create Services with \'new\'.'

    @Override
    void visitConstructorCallExpression(ConstructorCallExpression call) {
        if (isFirstVisit(call)) {
            if (call.type.name ==~ /.*Service/) {
                addViolation(call, ERROR_MSG)
            }
        }
        super.visitConstructorCallExpression(call)
    }
}
