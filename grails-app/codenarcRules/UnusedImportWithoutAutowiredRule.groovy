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
import org.codehaus.groovy.ast.ImportNode
import org.codenarc.rule.AbstractRule
import org.codenarc.rule.Violation
import org.codenarc.source.SourceCode
import org.codenarc.util.GroovyVersion

import java.util.regex.Pattern

/**
 * Copy of org.codenarc.rule.imports.UnusedImportRule
 */
class UnusedImportWithoutAutowiredRule extends AbstractRule {

    String name = 'UnusedImportWithoutAutowiredRule'
    int priority = 1
    String description = "A copy of codenarcs UnusedImportRule but it ignores unused imports of the Autowired annotation"

    @Override
    void applyTo(SourceCode sourceCode, List<Violation> violations) {
        processImports(sourceCode, violations)
        processStaticImports(sourceCode, violations)
    }

    private void processImports(SourceCode sourceCode, List violations) {
        sourceCode.ast?.imports?.each { importNode ->
            if (importNode.className != "org.springframework.beans.factory.annotation.Autowired" &&
                !findReference(sourceCode, importNode.alias, importNode.className)) {
                violations.add(createViolationForImport(sourceCode, importNode, "The [${importNode.className}] import is never referenced"))
            }
        }
    }

    private void processStaticImports(SourceCode sourceCode, List violations) {
        if (GroovyVersion.isGroovy1_8_OrGreater()) {
            sourceCode.ast?.staticImports?.each { alias, ImportNode classNode ->
                if (!findReference(sourceCode, alias)) {
                    violations.add(createViolationForImport(sourceCode, classNode.className, alias, "The [${classNode.className}] import is never referenced"))
                }
            }
        } else {
            sourceCode.ast?.staticImportAliases?.each { alias, classNode ->
                if (!findReference(sourceCode, alias)) {
                    violations.add(createViolationForImport(sourceCode, classNode.name, alias, "The [${classNode.name}] import is never referenced"))
                }
            }
        }
    }

    private String findReference(SourceCode sourceCode, String alias, String className = null) {
        def aliasSameAsNonQualifiedClassName = className && className.endsWith(alias)
        sourceCode.lines.find { line ->
            String aliasPattern = Pattern.quote(alias)
            if (!isImportStatementForAlias(line, aliasPattern)) {
                def aliasCount = countUsage(line, aliasPattern)
                return aliasSameAsNonQualifiedClassName ?
                        aliasCount && aliasCount > countUsage(line, Pattern.quote(className)) : aliasCount
            }
        }
    }

    private boolean isImportStatementForAlias(String line, String pattern) {
        final IMPORT_PATTERN = /import\s+.*/ + pattern
        line =~ IMPORT_PATTERN
    }

    private int countUsage(String line, String pattern) {
        final INVALID = '[^a-zA-Z0-9_\\$]'
        def regexp = /($INVALID|^|\$)${pattern}($INVALID|$)/
        return (line =~ regexp).count
    }
}
