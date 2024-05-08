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

// this file uses groovy.transform.CompilationCustomizer
// see documentation: https://melix.github.io/blog/2011/05/12/customizing_groovy_compilation_process.html

import groovy.transform.CompileStatic
import groovy.transform.Generated
import org.codehaus.groovy.ast.*

withConfig(configuration) {
    source(unitValidator: { unit ->
        String fileName = unit.source.file.toString()
        [
                '/services/',
                '/src/main/',
                '/src/init/',
                '/codenarcRules/',
        ].any {
            fileName.contains(it)
        }
    }) {
        ast(CompileStatic)
    }
}

// mark generated code with annotation @Generated
withConfig(configuration) {
    source(unitValidator: { unit ->
        // filter code to apply change only for production code and not for test code
        String fileName = unit.source.file.toString()
        [
                '/grails-app/',
                '/src/main/',
                '/src/init/',
        ].any {
            fileName.contains(it)
        }
    }) {
        inline(phase: 'INSTRUCTION_SELECTION') { source, context, ClassNode classNode ->
            classNode.methods.each { MethodNode methodNode ->
                if (methodNode.lineNumber == -1) {
                    if (!methodNode.getAnnotations().any {
                        // checking on class level don't work correctly
                        it.classNode.name == Generated.name
                    }) {
                        methodNode.addAnnotation(new AnnotationNode(new ClassNode(Generated)))
                    }
                }
            }
        }
    }
}
