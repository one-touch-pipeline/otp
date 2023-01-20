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

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

class ScheduledServiceBugRule extends AbstractAstVisitorRule {
    String applyToFileNames = "*/*grails-app/services*/*"
    int priority = 1
    String name = 'ScheduledServiceBug'
    String description = 'Due to a Bug, the Annotation does not function Reliably. See also OTP-2926'
    Class astVisitorClass = ScheduledServiceBugVisitor
}

class ScheduledServiceBugVisitor extends AbstractAstVisitor {

    private static final String SPRING_SCHEDULED_ANNOTATION = 'Scheduled'
    private static final String ERROR_MSG = 'Do not use @Scheduled, in Services.'

    @Override
    void visitAnnotations(AnnotatedNode node) {
        node.annotations.each { AnnotationNode annotationNode ->
            String annotation = annotationNode.classNode.text
            if (annotation == SPRING_SCHEDULED_ANNOTATION) {
                addViolation(node, ERROR_MSG)
            }
        }

        super.visitAnnotations(node)
    }
}
