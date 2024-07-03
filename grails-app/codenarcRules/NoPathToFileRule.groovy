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

class NoPathToFileRule extends AbstractAstVisitorRule {
    String name = 'NoPathToFile'
    int priority = 1
    Class astVisitorClass = NoPathToFileRuleAstVisitor
    String description = 'Use FileService.toFile() instead of Path.toFile(). Path.toFile() doesn\'t work for remote file systems.'
    boolean ignoreThisReference = false
}

class NoPathToFileRuleAstVisitor extends AbstractMethodCallExpressionVisitor {

    @Override
    @SuppressWarnings("Instanceof")
    void visitMethodCallExpression(MethodCallExpression call) {
        if (isUnsafeToFileCall(call) && isNotFromFileService(call)) {
            addViolation(call, "Use FileService.toFile() instead of Path.toFile(). Expression: ${call.text}")
        }
    }

    private boolean isUnsafeToFileCall(MethodCallExpression call) {
        Expression method = call.method
        return method instanceof ConstantExpression &&
                ((ConstantExpression) method).value == 'toFile' &&
                !AstUtil.isSafe(call) &&
                !AstUtil.isSpreadSafe(call)
    }

    private boolean isNotFromFileService(MethodCallExpression call) {
        String objectText = call.objectExpression.text.toLowerCase()
        return !(objectText == "fileservice") &&
                !(objectText in ["this", "super"] &&
                        currentClassNode.name.toLowerCase() == "de.dkfz.tbi.otp.infrastructure.fileservice")
    }
}
