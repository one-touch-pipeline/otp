package de.dkfz.tbi.otp.testing

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.junit.*

import de.dkfz.tbi.otp.ngsdata.Project;
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntergrationTest;

class TestGroovyScriptAwareIntergrationTest extends GroovyScriptAwareIntergrationTest {

    @Test
    void testCreateCorrect() {
        File script = new File("scripts/TestGroovyScriptAwareIntergrationTest.groovy")
        run(script)
        Project project = Project.findByName("HIPO")
        println "found  project ${project}"
    }

}
