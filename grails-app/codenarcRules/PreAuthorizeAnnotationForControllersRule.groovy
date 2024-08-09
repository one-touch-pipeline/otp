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

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.Phases
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.codehaus.groovy.ast.AnnotationNode

class PreAuthorizeAnnotationForControllersRule extends AbstractAstVisitorRule {
    int priority = 1
    String applyToClassNames = "*Controller"
    String name = 'PreAuthorizeAnnotationForControllers'
    String description = 'Ensures that no controller which has a PreAuthorize annotation uses the hasPermission check.'
    Class astVisitorClass = PreAuthorizeAnnotationForControllersRuleVisitor
    int compilerPhase = Phases.SEMANTIC_ANALYSIS
}

class PreAuthorizeAnnotationForControllersRuleVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    private static final String ANNOTATION = "PreAuthorize"

    @Override
    protected void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node)) {
            return
        }
        node.annotations.each { AnnotationNode annotationNode ->
            if (annotationNode.classNode.text.contains(ANNOTATION)) {
                if (annotationNode.members['value']?.text.contains("hasPermission")) {
                    addViolation(node, "The @PreAuthorize annotation in controller is not allowed to check for 'hasPermission'")
                }
            }
        }
    }

    @Override
    protected void visitMethodEx(MethodNode node) {
        node.annotations.each { AnnotationNode annotationNode ->
            if (annotationNode.classNode.text.contains(ANNOTATION)) {
                if (annotationNode.members['value']?.text.contains("hasPermission")) {
                    addViolation(node, "The @PreAuthorize annotation in controller is not allowed to check for 'hasPermission'")
                }
            }
        }
    }
}
