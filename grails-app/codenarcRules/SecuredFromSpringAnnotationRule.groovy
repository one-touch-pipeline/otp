/*
 * Copyright 2011-2022 The OTP authors
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

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ImportNode
import org.codenarc.rule.AbstractAstVisitorRule
import org.codenarc.rule.Violation
import org.codenarc.source.SourceCode

class SecuredFromSpringAnnotationRule extends AbstractAstVisitorRule {

    String name = 'SecuredFromSpringAnnotationRule'
    int priority = 1
    String description = "All secured annotations should be used from spring"

    @Override
    void applyTo(SourceCode sourceCode, List<Violation> violations) {
        processImports(sourceCode, violations)
        processStaticImports(sourceCode, violations)
    }

    private void processImports(SourceCode sourceCode, List violations) {
        sourceCode.ast?.imports?.each { importNode ->
            if (importNode.className.contains("Secured") && importNode.className != "org.springframework.security.access.annotation.Secured") {
                violations.add(createViolationForImport(sourceCode, importNode,
                "The [${importNode.className}] is not the valid Secured annotations import. " +
                "Use [org.springframework.security.access.annotation.Secured] instead."
                ))
            }
        }
    }

    private void processStaticImports(SourceCode sourceCode, List violations) {
        sourceCode.ast?.staticImports?.each { alias, ImportNode importNode ->
            if (importNode.className.contains("Secured") && importNode.className != "org.springframework.security.access.annotation.Secured") {
                violations.add(createViolationForImport(sourceCode, importNode,
                "The [${importNode.className}] is not the valid Secured annotations import. " +
                "Use [org.springframework.security.access.annotation.Secured] instead."))
            }
        }
    }

    @Override
    protected boolean shouldApplyThisRuleTo(ClassNode classNode) {
        List<AnnotationNode> annotationNodeList = classNode.annotations
        return annotationNodeList.any {
            it.classNode.name == "Secured"
        }
    }

}
