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

package de.dkfz.tbi.otp.scriptTests

import grails.core.GrailsApplication
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.runtime.InvokerHelper

import de.dkfz.tbi.otp.integration.AbstractIntegrationTest

/**
 * This is a first version of a class helping to call groovy scripts
 * from integration tests.
 * The tests that need such functionality must extend this class.
 * it is not possible to use this class in the unit tests
 * to call scripts creating domain objects
 */
abstract class GroovyScriptAwareTestCase extends AbstractIntegrationTest {

    GrailsApplication grailsApplication


    private int counter

    /**
     * Load and execute a Groovy script, usually a work-flow definition.
     *
     * @param scriptFile the script to run
     * @return the result of the evaluation
     */
    void runScript(File script, Map<String, String> properties=null) {
        assert script.canRead()
        GroovyShell shell = new GroovyShell(
                grailsApplication.getClassLoader(),
                new Binding(ctx: grailsApplication.getMainContext(),
                        grailsApplication: grailsApplication))
        properties?.each {  Map.Entry<String, String> entry ->
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
    void runScript(String pathToScript, Map<String, String> properties=null) {
        File script = new File(pathToScript)
        runScript(script, properties)
    }


    private invokeMethod(File scriptFile, String method, List<Object> arguments, Map<String, String> properties=null) {
        assert scriptFile.canRead()
        final String scriptText = scriptFile.text
        final String codeBase = "/groovy/shell"
        ClassLoader parentLoader = grailsApplication.getClassLoader()
        Binding context = new Binding(ctx: grailsApplication.getMainContext(),
                grailsApplication: grailsApplication)

        properties?.each { Map.Entry<String, String> entry ->
            System.setProperty(entry.key, entry.value)
        }

        GroovyClassLoader loader = new GroovyClassLoader(parentLoader, CompilerConfiguration.DEFAULT)
        GroovyCodeSource codeSource = new GroovyCodeSource(scriptText, generateScriptName(scriptFile.name), codeBase)

        Script script = InvokerHelper.createScript(loader.parseClass(codeSource, false), context)
        script.setBinding(context)
        return script.invokeMethod(method, arguments.toArray())
    }

    protected synchronized String generateScriptName(String name) {
        return "${name}${++this.counter}.groovy"
    }

    /**
     * Loads and executes several Groovy scripts.
     *
     * @param scripts the scripts to run in order
     * @return the results of all evaluations, in the order of the scripts provided
     *
     * @see #runScript(File)
     */
    protected List runScript(List scripts) {
        return scripts?.collect { runScript(it) }
    }
}
