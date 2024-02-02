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
package de.dkfz.tbi.otp

import grails.core.GrailsApplication

/**
 * This is a trait helping to call groovy scripts from integration tests.
 * The tests that need such functionality must extend this class.
 * it is not possible to use this class in the unit tests
 * to call scripts creating domain objects
 */
trait GroovyScriptAwareTestCase {

    GrailsApplication grailsApplication

    /**
     * Load and execute a Groovy script, usually a work-flow definition.
     *
     * @param scriptFile the script to run
     * @return the result of the evaluation
     */
    void runScript(File script, Map<String, String> properties = [:]) {
        assert script.canRead()
        GroovyShell shell = new GroovyShell(
                grailsApplication.classLoader,
                new Binding(
                        ctx: grailsApplication.mainContext,
                        grailsApplication: grailsApplication
                )
        )
        properties?.each { Map.Entry<String, String> entry ->
            System.setProperty(entry.key, entry.value)
        }
        shell.evaluate(script.text)
    }

    /**
     * Convenience method, please use {@link #runScript(File)} instead.
     *
     * @param pathToScript the path to the script file
     * @return the result of the evaluation
     */
    void runScript(String pathToScript, Map<String, String> properties = [:]) {
        File script = new File(pathToScript)
        runScript(script, properties)
    }

    /**
     * Loads and executes several Groovy scripts.
     *
     * @param scripts the scripts to run in order
     * @return the results of all evaluations, in the order of the scripts provided
     *
     * @see #runScript(File)
     */
    void runScript(List scripts) {
        scripts?.each { runScript(it) }
    }
}
