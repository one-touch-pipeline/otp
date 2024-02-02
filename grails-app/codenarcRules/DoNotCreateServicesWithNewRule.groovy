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

import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

class DoNotCreateServicesWithNewRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*"
    int priority = 1
    String name = 'DoNotCreateServicesWithNew'
    String description = 'The Services will be autowired by Spring.' +
            'Creating one with \'new\' is not allowed since it leads to unpredictable and undesired behaviour.'
    Class astVisitorClass = DoNotCreateServicesWithNewVisitor
}

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
