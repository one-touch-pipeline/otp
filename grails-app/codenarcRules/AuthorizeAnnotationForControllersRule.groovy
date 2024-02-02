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

class AuthorizeAnnotationForControllersRule extends AbstractAstVisitorRule {
    int priority = 1
    String applyToClassNames = "*Controller"
    String name = 'AuthorizeAnnotationForControllers'
    String description = 'Ensures that either the controller or its parent controller is annotated with an authorized annotation or all the methods ' +
            'of the controller are annotated with an authorized annotation.'
    Class astVisitorClass = AuthorizeAnnotationForControllersRuleVisitor
    int compilerPhase = Phases.SEMANTIC_ANALYSIS
}

class AuthorizeAnnotationForControllersRuleVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    private static final List<String> ANNOTATIONS = [
            "PreAuthorize",
            "PostAuthorize",
    ]

    @Override
    protected void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node)) {
            return
        }

        if (node.annotations*.classNode*.nameWithoutPackage.disjoint(ANNOTATIONS)) {
            ClassNode parentNode = node.superClass.redirect()
            if (!parentNode || parentNode.annotations*.classNode*.nameWithoutPackage.disjoint(ANNOTATIONS)) {
                node.methods.each { MethodNode method ->
                    if (method.annotations*.classNode*.nameWithoutPackage.disjoint(ANNOTATIONS)) {
                        String message = "${method.name} method should be annotated if the class ${node.name} or it's parent is not annotated. " +
                                "Please annotate the class or all it's methods."
                        addViolation(node, message)
                    }
                }
            }
        }
    }
}
