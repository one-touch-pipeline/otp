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
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

class DoNotUseCompileDynamicWithClasses extends AbstractAstVisitorRule {
    int priority = 1
    String applyToClassNames = "*Service"
    String applyToFileNames = "*/*grails-app/services*/*"
    String name = 'DoNotUseCompileDynamicWithClasses'
    String description = 'Do not use compileDynamic with classes.'
    Class astVisitorClass = CompileDynamicAnnotationForServicesRuleVisitor
}

class CompileDynamicAnnotationForServicesRuleVisitor extends AbstractAstVisitor implements IsAnnotationVisitor {

    private static final String SPRING_COMPILE_DYNAMIC_ANNOTATION = 'CompileDynamic'
    private static final String SPRING_DEPRECATED_ANNOTATION = 'Deprecated'

    @Override
    protected void visitClassEx(ClassNode node) {
        if (isNoOrdinaryClass(node) || node.annotations*.classNode.text.contains(SPRING_DEPRECATED_ANNOTATION)) {
            return
        }

        if (node.annotations*.classNode.text.contains(SPRING_COMPILE_DYNAMIC_ANNOTATION)) {
            String message = "Service class ${node.name} should not be annotated as CompileDynamic. " +
                    "Please annotate individual methods that really need dynamic compilation."
            addViolation(node, message)
        }
    }
}
