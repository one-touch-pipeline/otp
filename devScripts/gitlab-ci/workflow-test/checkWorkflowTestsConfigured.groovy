/*
 * Copyright 2011-2026 The OTP authors
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
import groovy.yaml.YamlSlurper

import java.nio.file.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Checks that every workflow test is run by the gitlab job 'workflow tests'.
 *
 * The job runs the workflow tests in groups: the groups are defined by the matrix of the job in '.gitlab-ci-core.yml',
 * the test patterns of each group are defined in 'runWorkflowTests.sh'. The check fails if
 * - a group exists only in one of both files
 * - a workflow test is not matched by any pattern
 * - a pattern does not match any workflow test
 */
@CompileStatic
@SuppressWarnings(['ClassNameSameAsFilename', 'JavaIoPackageAccess'])
class CheckWorkflowTestsConfigured {

    static final Path CI_FILE = Paths.get('.gitlab-ci-core.yml')
    static final String CI_JOB = 'workflow tests'
    static final String CI_MATRIX_VARIABLE = 'WORKFLOW_TEST_GROUP'
    static final Path RUN_SCRIPT = Paths.get('devScripts/gitlab-ci/workflow-test/runWorkflowTests.sh')
    static final Path TEST_SOURCE_DIR = Paths.get('src/workflow-test/groovy')

    static final Pattern CASE_BRANCH = ~/(?ms)^\s*([A-Z_]+)\)\s*$(.*?)^\s*;;/
    static final Pattern QUOTED_PATTERN = ~/'([^']+)'/

    @SuppressWarnings(['Println', 'SystemExit'])
    static void main(String... args) {
        List<String> problems = []

        List<String> matrixGroups = readMatrixGroups()
        Map<String, List<String>> scriptGroups = readScriptGroups()

        (matrixGroups - scriptGroups.keySet()).each {
            problems << "Group '${it}' is part of the matrix of the job '${CI_JOB}', but not defined in '${RUN_SCRIPT}'".toString()
        }
        (scriptGroups.keySet() - matrixGroups).each {
            problems << "Group '${it}' is defined in '${RUN_SCRIPT}', but not part of the matrix of the job '${CI_JOB}'".toString()
        }
        scriptGroups.findAll { !it.value }.each {
            problems << "Group '${it.key}' in '${RUN_SCRIPT}' has no test patterns".toString()
        }

        List<String> testClasses = findTestClasses()
        Map<String, Pattern> patterns = scriptGroups.values().flatten().collectEntries { Object pattern ->
            [(pattern): toRegex(pattern as String)]
        } as Map<String, Pattern>

        testClasses.findAll { String testClass ->
            !patterns.values().any { it.matcher(testClass).matches() }
        }.each {
            problems << "Workflow test '${it}' is not part of any group of the job '${CI_JOB}', add it to '${RUN_SCRIPT}'".toString()
        }
        patterns.findAll { Map.Entry<String, Pattern> entry ->
            !testClasses.any { entry.value.matcher(it).matches() }
        }.each {
            problems << "Pattern '${it.key}' in '${RUN_SCRIPT}' does not match any workflow test".toString()
        }

        if (problems) {
            println problems.join("\n")
            System.exit(1)
        }
        println "All ${testClasses.size()} workflow tests are part of the job '${CI_JOB}' (groups: ${matrixGroups.join(', ')})"
    }

    static List<String> readMatrixGroups() {
        Map ciConfig = new YamlSlurper().parse(CI_FILE) as Map
        Map job = ciConfig[CI_JOB] as Map
        assert job: "Job '${CI_JOB}' not found in '${CI_FILE}'"
        List<Map> matrix = (job['parallel'] as Map)?.get('matrix') as List<Map>
        assert matrix: "Job '${CI_JOB}' in '${CI_FILE}' has no parallel matrix"
        return matrix.collectMany { it[CI_MATRIX_VARIABLE] as List<String> ?: [] }
    }

    static Map<String, List<String>> readScriptGroups() {
        Map<String, List<String>> groups = [:]
        Matcher matcher = CASE_BRANCH.matcher(RUN_SCRIPT.text)
        while (matcher.find()) {
            groups[matcher.group(1)] = QUOTED_PATTERN.matcher(matcher.group(2)).findAll().collect { (it as List<String>)[1] }
        }
        assert groups: "No groups found in '${RUN_SCRIPT}'"
        return groups
    }

    /**
     * Returns the fully qualified names of all non-abstract workflow test classes.
     */
    static List<String> findTestClasses() {
        List<String> testClasses = []
        Files.walk(TEST_SOURCE_DIR).withCloseable { stream ->
            stream.filter { Path path -> path.toString().endsWith('Spec.groovy') }.each { Path path ->
                String className = path.fileName.toString() - '.groovy'
                Matcher declaration = path.text =~ /(?m)^(abstract\s+)?class\s+${className}\b/
                assert declaration.find(): "No top level class '${className}' found in '${path}'"
                if (!declaration.group(1)) {
                    testClasses << TEST_SOURCE_DIR.relativize(path).toString().replace(File.separator, '.') - '.groovy'
                }
            }
        }
        return testClasses.sort()
    }

    /**
     * Converts a gradle test filter pattern into a regex, the wildcard '*' matches any characters, including dots.
     */
    static Pattern toRegex(String pattern) {
        return Pattern.compile(pattern.split(/\*/, -1).collect { Pattern.quote(it) }.join('.*'))
    }
}
