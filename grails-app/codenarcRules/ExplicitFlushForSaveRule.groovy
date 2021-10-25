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
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

@CompileStatic
class ExplicitFlushForSaveRule extends AbstractAstVisitorRule {
    int priority = 1
    String name = 'ExplicitFlushForSaveRule'
    String description = 'Ensures that every time .save() is executed on a DomainClass that its parameters contain flush with either true or false.'
    Class astVisitorClass = ExplicitFlushForSaveVisitor
}

@CompileStatic
class ExplicitFlushForSaveVisitor extends AbstractAstVisitor {

    @Override
    void visitMethodCallExpression(MethodCallExpression call) {
        if (call.methodAsString == 'save') {
            String parameter = call.arguments.text
            if (!parameter.contains('flush')) {
                addViolation(call, "${parameter} does not contain 'flush'")
            }
        }
    }
}
