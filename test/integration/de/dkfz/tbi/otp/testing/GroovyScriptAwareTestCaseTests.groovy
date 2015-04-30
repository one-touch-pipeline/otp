package de.dkfz.tbi.otp.testing

import de.dkfz.tbi.otp.ngsdata.Project

class GroovyScriptAwareTestCaseTests extends GroovyScriptAwareTestCase {

    void testScriptCreatingProject() {
        runScript(new File('scripts/TestGroovyScriptAwareIntergrationTest.groovy'))
        assert Project.findByName('HIPO') : 'Did not find the project that was created in the script.'
    }
}
