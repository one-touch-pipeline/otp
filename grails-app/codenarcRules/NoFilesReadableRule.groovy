/*
 * Copyright 2011-2023 The OTP authors
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
class NoFilesReadableRule extends AbstractAstVisitorRule {
    int priority = 1
    String name = 'NoFilesReadableRule'
    String description = "Ensures that Files.isReadable is not used as it is not guaranteed to work with files and directories under ACL"
    Class astVisitorClass = NoFilesReadableVisitor
    String doNotApplyToFileNames = "*/*otp/jobs*/*"
}

@CompileStatic
class NoFilesReadableVisitor extends AbstractAstVisitor {

    @Override
    void visitMethodCallExpression(MethodCallExpression expression) {
        if (expression.methodAsString == 'isReadable' && expression.objectExpression.text == 'Files') {
            addViolation(expression, "${expression.objectExpression.text}.${expression.methodAsString} is not allowed, use fileIsReadable from FileService")
        }
    }
}
