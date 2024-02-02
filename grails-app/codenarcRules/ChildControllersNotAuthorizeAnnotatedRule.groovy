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

import org.codehaus.groovy.ast.*
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.codehaus.groovy.control.Phases

class ChildControllersNotAuthorizeAnnotatedRule extends AbstractAstVisitorRule {
    int priority = 1
    String name = 'ChildControllersNotAuthorizeAnnotated'
    String description = 'Ensures that a child class is not annotated if its parent class is already annotated.'
    Class astVisitorClass = ChildControllersNotAuthorizeAnnotatedRuleVisitor
    int compilerPhase = Phases.SEMANTIC_ANALYSIS
}

class ChildControllersNotAuthorizeAnnotatedRuleVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    private static final List<String> ANNOTATIONS = [
            "PreAuthorize",
            "PostAuthorize",
    ]

    @Override
    protected void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node)) {
            return
        }

        if (!node.annotations*.classNode*.nameWithoutPackage.disjoint(ANNOTATIONS)) {
            ClassNode parentNode = node.superClass.redirect()
            if (!parentNode.annotations*.classNode*.nameWithoutPackage.disjoint(ANNOTATIONS)) {
                String message = "Child class ${node.name} " +
                        "should not have Security annotation (Pre|Post)Authorize if it's parent class " +
                        "${parentNode.name} is already annotated."
                addViolation(node, message)
            }
        }
    }
}
