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
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

@CompileStatic
class AnnotationsForJobsRule extends AbstractAstVisitorRule {
    String doNotApplyToFileNames = "*/*test*/*,*/*StartJob.groovy,*/*domain*/*"
    String applyToFileNames = "*/*Job.groovy"
    int priority = 1
    String name = 'AnnotationsForJobs'
    String description = 'Ensures that Jobs have the Proper Annotations.'
    Class astVisitorClass = AnnotationsForJobsVisitor
}

@CompileStatic
class AnnotationsForJobsVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    @Override
    void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node)) {
            return
        }

        boolean hasComponent = false
        boolean hasScope = false
        boolean hasLog = false

        node.annotations.each { AnnotationNode annotationNode ->
            switch (annotationNode.classNode.text) {
                case 'Component':
                    hasComponent = true
                    break
                case 'Scope':
                    hasScope = true
                    if (annotationNode.members['value']?.text != 'prototype') {
                        addViolation(node, "The @Scope annotation is missing the value 'prototype'")
                    }
                    break
                case 'Slf4j':
                    hasLog = true
                    break
                default:
                    return
            }
        }

        if (!hasComponent) {
            addViolation(node, buildErrorString("@Component"))
        }
        if (!hasScope) {
            addViolation(node, buildErrorString("@Scope"))
        }
        if (!hasLog) {
            addViolation(node, buildErrorString("@Slf4j"))
        }
    }
}
