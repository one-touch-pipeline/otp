package de.dkfz.tbi.otp.utils

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


@TestMixin(GrailsUnitTestMixin)
@TestFor(ExternalScript)
@Mock([ExternalScript])
class ExternalScriptTests {

    void testConstraintScriptName() {
        ExternalScript externalScript = createExternalScript([scriptName: null])
        assertFalse(externalScript.validate())
        externalScript.scriptName = ""
        assertFalse(externalScript.validate())
        externalScript.scriptName = "testScript"
        assertTrue(externalScript.validate())
    }

    void testScriptNameAndDeprecatedDate() {
        ExternalScript externalScript1 = createExternalScript()
        assertTrue(externalScript1.validate())
        externalScript1.save()

        ExternalScript externalScript2 = createExternalScript()
        assertFalse(externalScript2.validate())

        externalScript2.scriptName = "testScript_version2"
        assertTrue(externalScript2.validate())

        externalScript1.deprecatedDate = new Date()
        externalScript1.save()
        ExternalScript externalScript3 = createExternalScript()
        assertTrue(externalScript3.validate())
    }

    void testConstraintLocation() {
        ExternalScript externalScript = createExternalScript([location: null])
        assertFalse(externalScript.validate())
        externalScript.location = ""
        assertFalse(externalScript.validate())
        externalScript.location = "tmp/testfolder"
        assertFalse(externalScript.validate())
        externalScript.location = "/tmp/testfolder"
        assertTrue(externalScript.validate())
    }

    void testConstraintAuthor() {
        ExternalScript externalScript = createExternalScript([author: null])
        assertFalse(externalScript.validate())
        externalScript.author = ""
        assertFalse(externalScript.validate())
        externalScript.author = "testUser"
        assertTrue(externalScript.validate())
    }

    void testIsDeprecated() {
        ExternalScript externalScript1 = createExternalScript()
        assertFalse(externalScript1.isDeprecated())
        externalScript1.deprecatedDate = new Date()
        assertTrue(externalScript1.isDeprecated())
    }


    void testToString() {
        Date deprecatedDate = new Date()
        ExternalScript externalScript1 = createExternalScript()
        externalScript1.deprecatedDate = deprecatedDate
        String expectedString = "external script name: testScript, path: /tmp/testfolder, deprecatedDate: ${deprecatedDate}"
        assertEquals(externalScript1.toString(), expectedString)
    }


    private ExternalScript createExternalScript(Map properties = [:]) {
        return new ExternalScript([
            scriptName :"testScript",
            location: "/tmp/testfolder",
            author: "testUser",
            comment: "lets see if it works ;)",
        ] + properties)
    }
}
