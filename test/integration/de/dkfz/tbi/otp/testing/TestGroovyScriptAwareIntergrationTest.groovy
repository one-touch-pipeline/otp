package de.dkfz.tbi.otp.testing

import org.junit.*
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

class TestGroovyScriptAwareIntergrationTest extends GroovyScriptAwareIntegrationTest {

    @Test
    void testCreateCorrect() {
        File script = new File("scripts/TestGroovyScriptAwareIntergrationTest.groovy")
        run(script)
        Project project = Project.findByName("HIPO")
        println "found  project ${project}"
    }

}
