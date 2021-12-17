/*
 * Copyright 2011-2020 The OTP authors
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
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

class SecureAllControllersRule extends AbstractAstVisitorRule {
    String name = 'SecureAllControllers'
    int priority = 1
    String applyToFileNames = "*/*grails-app/controllers*/*Controller.groovy"
    String description = "All controllers should be secured with the \"@Secured\" annotation!"
    Class astVisitorClass = SecureAllControllerRuleVisitor
}

class SecureAllControllerRuleVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    private static final String CONTROLLER = 'Controller'

    @Override
    protected void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node) || !node.name.endsWith(CONTROLLER)) {
            return
        }
        checkRule(node)
    }

    private void checkRule(ClassNode node) {
        List<AnnotationNode> annotationNodeList = node.annotations

        if (!annotationNodeList.any {
            it.classNode.name == "Secured"
        }) {
            addViolation(node, buildErrorString("@Secured"))
        }
    }
}
