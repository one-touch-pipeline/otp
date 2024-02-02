/*
 * Copyright 2011-2024 The OTP authors
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
import org.codehaus.groovy.ast.expr.*
import org.codenarc.rule.AbstractAstVisitorRule
import org.codenarc.rule.AbstractMethodCallExpressionVisitor
import org.codenarc.util.AstUtil

class AvoidFindWithoutAllRule extends AbstractAstVisitorRule {
    String name = 'AvoidFindWithoutAll'
    int priority = 1
    Class astVisitorClass = AvoidFindWithoutAllRuleAstVisitor
    String description = 'This Rule shows the usage of "findBy" and "findWhere" instead of "findAllBy" and "findAllWhere". ' +
            'Since the former can lead to unforeseen results.'
    boolean ignoreThisReference = false
}

class AvoidFindWithoutAllRuleAstVisitor extends AbstractMethodCallExpressionVisitor {

    @Override
    @SuppressWarnings("Instanceof")
    void visitMethodCallExpression(MethodCallExpression call) {
        Expression method = call.method
        if (method instanceof ConstantExpression) {
            if (((ConstantExpression) method).value instanceof String) {
                if (!AstUtil.isSafe(call) && !AstUtil.isSpreadSafe(call)) {
                    if (!call.objectExpression.text.toLowerCase().endsWith("service") &&
                            !(call.objectExpression.text in ["this", "super"] && currentClassNode.name.toLowerCase().endsWith("service"))) {
                        if (((String) ((ConstantExpression) method).value).startsWith('findBy')) {
                            addViolation(call, "Avoid using findBy, instead use findAllBy. Expression: ${call.text}")
                        } else if (((String) ((ConstantExpression) method).value).startsWith('findWhere')) {
                            addViolation(call, "Avoid using findWhere, instead use findAllWhere. Expression: ${call.text}")
                        }
                    }
                }
            }
        }
    }
}
