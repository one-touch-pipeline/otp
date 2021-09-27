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
class NoExplicitFlushForDeleteRule extends AbstractAstVisitorRule {
    int priority = 1
    String name = 'NoExplicitFlushForDeleteRule'
    String description = 'Ensures that every time .delete() is executed on a DomainClass that its parameters don\'t contain flush: ' +
            'Setting it doesn\'t have an effect. The default is auto; to disable flushing use "SessionUtils.manualFlush".'
    Class astVisitorClass = NoExplicitFlushForDeleteVisitor
    String doNotApplyToFilesMatching = "(.*/test/.*)|(.*/test-helper/.*)"
}

@CompileStatic
class NoExplicitFlushForDeleteVisitor extends AbstractAstVisitor {

    @Override
    void visitMethodCallExpression(MethodCallExpression call) {
        if (call.methodAsString == 'delete') {
            String parameter = call.arguments.text
            if (parameter.contains('flush')) {
                addViolation(call, "${parameter} contains 'flush'")
            }
        }
    }
}
