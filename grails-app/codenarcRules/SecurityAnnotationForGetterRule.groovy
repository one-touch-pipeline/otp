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
import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule

@CompileStatic
class SecurityAnnotationForGetterRule extends AbstractAstVisitorRule {
    int priority = 1
    String name = 'SecurityAnnotationForGetter'
    String description = 'Security annotation (Pre|Post)(Authorize|Filter) must not be used with getters/setters because security restrictions are ' +
            'circumvented when using Groovy-style property reference'
    Class astVisitorClass = SecurityAnnotationForGetterVisitor
}

@CompileStatic
@Slf4j
class SecurityAnnotationForGetterVisitor extends AbstractAstVisitor {

    private static final CharSequence[] METHOD_PREFIXES = ["get", "set"]
    private static final List<String> ANNOTATIONS = [
            "PreAuthorize",
            "PreFilter",
            "PostAuthorize",
            "PostFilter",
    ]
    private static final String ERROR_MSG = 'Security annotations must not be used with getter/setter'

    @Override
    protected void visitClassEx(ClassNode node) {
        getMethods(node).each { MethodNode method ->
            if (method.name.startsWithAny(METHOD_PREFIXES) &&
                    method.parameters.size() == 0 &&
                    method.annotations.any { it.classNode.name in ANNOTATIONS }) {
                addViolation(method, ERROR_MSG)
            }
        }
    }

    private List<MethodNode> getMethods(ClassNode node) {
        return node.methods.findAll { it.public }
    }
}
