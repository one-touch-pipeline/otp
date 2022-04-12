/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

@CompileStatic
class ControllerMethodNotInAllowedMethodsRule extends AbstractAstVisitorRule {
    String applyToFileNames = "*/*grails-app/controllers*/*"
    int priority = 1
    String name = 'ControllerMethodNotInAllowedMethods'
    String description = 'Public methods in controller must be listed in allowedMethods'
    Class astVisitorClass = ControllerMethodNotInAllowedMethodsVisitor
}

@CompileStatic
class ControllerMethodNotInAllowedMethodsVisitor extends AbstractAstVisitor {

    private static final String CONTROLLER = 'Controller'
    private static final String ALLOWED_METHODS_MAP = 'allowedMethods'
    private static final String ERROR_MSG = 'Method name is not in allowedMethods map'

    @Override
    protected void visitClassEx(ClassNode node) {
        if (!node.name.endsWith(CONTROLLER)) {
            return
        }

        List<String> allowedMethods = getAllowedMethods(node)

        getMethods(node).each { MethodNode method ->
            if (!(method.name in allowedMethods)) {
                addViolation(method, ERROR_MSG)
            }
        }
    }

    private List<String> getAllowedMethods(ClassNode node) {
        FieldNode field = node.fields.find { it.static && it.name == ALLOWED_METHODS_MAP }
        if (!field) {
            return []
        }
        Expression expression = field.initialValueExpression
        if (!(expression instanceof MapExpression)) {
            return []
        }
        return ((MapExpression) expression).mapEntryExpressions.collect { MapEntryExpression entry ->
            if (!((entry.valueExpression instanceof ConstantExpression) &&
                    ((ConstantExpression) entry.valueExpression).value instanceof String)) {
                return null
            }
            Expression keyExpression = entry.keyExpression
            if (!(keyExpression instanceof ConstantExpression)) {
                return null
            }
            Object key = ((ConstantExpression)keyExpression).value
            if (!(key instanceof String)) {
                return null
            }
            return (String)key
        }.findAll()
    }

    private List<MethodNode> getMethods(ClassNode node) {
        return node.methods.findAll { it.public }
    }
}
