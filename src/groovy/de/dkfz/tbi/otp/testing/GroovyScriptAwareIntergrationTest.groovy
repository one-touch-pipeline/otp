package de.dkfz.tbi.otp.testing

import org.codehaus.groovy.grails.commons.GrailsApplication
import static org.springframework.util.Assert.*


/**
 * <p>
 * This is a first version of a class helping to call groovy scripts
 * from integration tests.
 * The tests that need such functionality must extend this class.
 * it is not possible to use this class in the unit tests
 * to call scripts creating domain objects
 * 
 *
 */
abstract class GroovyScriptAwareIntergrationTest {

    GrailsApplication grailsApplication

    void run(File script) {
        isTrue script.canRead()
        GroovyShell shell = new GroovyShell(
                grailsApplication.getClassLoader(),
                new Binding(ctx: grailsApplication.getMainContext(),
                grailsApplication: grailsApplication))
        shell.evaluate(script.text)
    }

    void run(String pathToScript) {
        File script = new File(pathToScript)
        run(script)
    }
}
