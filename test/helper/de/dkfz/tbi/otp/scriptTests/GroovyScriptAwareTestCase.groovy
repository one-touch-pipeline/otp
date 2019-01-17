package de.dkfz.tbi.otp.scriptTests

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.runtime.InvokerHelper

import de.dkfz.tbi.otp.integration.AbstractIntegrationTest

/**
 * This is a first version of a class helping to call groovy scripts
 * from integration tests.
 * The tests that need such functionality must extend this class.
 * it is not possible to use this class in the unit tests
 * to call scripts creating domain objects
 *
 */
@TestMixin(IntegrationTestMixin)
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
