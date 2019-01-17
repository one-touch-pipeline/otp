package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import org.junit.Test
import de.dkfz.tbi.otp.scriptTests.GroovyScriptAwareTestCase

class GroovyScriptAwareTestCaseTests extends GroovyScriptAwareTestCase {

    @Test
    void testScriptCreatingProject() {
        DomainFactory.createDefaultRealmWithProcessingOption()
        runScript(new File('scripts/TestGroovyScriptAwareIntergrationTest.groovy'))
        assert Project.findByName('AProject') : 'Did not find the project that was created in the script.'
    }
}
