package de.dkfz.tbi.otp.testing

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.runtime.InvokerHelper

import static org.springframework.util.Assert.*

import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * This is a first version of a class helping to call groovy scripts
 * from integration tests.
 * The tests that need such functionality must extend this class.
 * it is not possible to use this class in the unit tests
 * to call scripts creating domain objects
 *
 *
 */
abstract class GroovyScriptAwareIntegrationTest extends AbstractIntegrationTest {

    GrailsApplication grailsApplication
    private int counter

    void run(File script, Map<String, String> properties=null) {
        isTrue script.canRead()
        GroovyShell shell = new GroovyShell(
                grailsApplication.getClassLoader(),
                new Binding(ctx: grailsApplication.getMainContext(),
                grailsApplication: grailsApplication))
        properties?.each {  Map.Entry<String, String> entry ->
            System.setProperty(entry.key, entry.value)
        }
        shell.evaluate(script.text)
    }

    void run(String pathToScript, Map<String, String> properties=null) {
        File script = new File(pathToScript)
        run(script, properties)
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
}
