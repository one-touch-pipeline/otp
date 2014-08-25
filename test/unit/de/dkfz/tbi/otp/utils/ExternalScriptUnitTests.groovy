package de.dkfz.tbi.otp.utils

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


@TestMixin(GrailsUnitTestMixin)
@TestFor(ExternalScript)
class ExternalScriptUnitTests {

    final String SCRIPT_IDENTIFIER = "TEST"
    final String SCRIPT_FILE_PATH = "/tmp/testfolder/testScript.sh"

    void testConstraintScriptIdentifier() {
        ExternalScript externalScript = createExternalScript([scriptIdentifier: null])
        assertFalse(externalScript.validate())
        externalScript.scriptIdentifier = ""
        assertFalse(externalScript.validate())
        externalScript.scriptIdentifier = SCRIPT_IDENTIFIER
        assertTrue(externalScript.validate())
    }

    void testConstraintFilePathUnique() {
        ExternalScript externalScriptFirst = createExternalScript()
        assertTrue(externalScriptFirst.validate())
        assert externalScriptFirst.save()

        ExternalScript externalScriptSecond = createExternalScript(scriptIdentifier: "${SCRIPT_IDENTIFIER}_other")
        assertFalse(externalScriptSecond.validate())

        externalScriptSecond.filePath = "/tmp/otherPath/testScript"
        assertTrue(externalScriptSecond.validate())
    }

    void testScriptIdentifierAndDeprecatedDate() {
        ExternalScript externalScript1 = createExternalScript()
        assertTrue(externalScript1.validate())
        externalScript1.save()

        ExternalScript externalScript2 = createExternalScript(filePath: "/tmp/otherPath/testScript")
        assertFalse(externalScript2.validate())

        externalScript2.scriptIdentifier = "testScript_version2"
        assertTrue(externalScript2.validate())

        externalScript1.deprecatedDate = new Date()
        externalScript1.save()
        ExternalScript externalScript3 = createExternalScript(filePath: "/tmp/otherOtherPath/testScript")
        assertTrue(externalScript3.validate())
    }

    void testConstraintFilePath() {
        ExternalScript externalScript = createExternalScript([filePath: null])
        assertFalse(externalScript.validate())
        externalScript.filePath = ""
        assertFalse(externalScript.validate())
        externalScript.filePath = "tmp/testfolder/testScript.sh"
        assertFalse(externalScript.validate())
        externalScript.filePath = "/tmp/testfolder/testScript.sh"
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
        String expectedString = "external script identifier: ${SCRIPT_IDENTIFIER}, path: ${SCRIPT_FILE_PATH}, deprecatedDate: ${deprecatedDate}"
        assertEquals(externalScript1.toString(), expectedString)
    }

    void testNotMoreThanOneNotWithdrawnExternalScript() {
        ExternalScript externalScript1 = createExternalScript()
        externalScript1.save()
        ExternalScript externalScript2 = createExternalScript()
        assertFalse(externalScript2.validate())
    }

    @Test
    void testGetScriptFilePath() {
        ExternalScript externalScript = createExternalScript()
        String expectedPath = externalScript.filePath
        String actualPath = externalScript.getScriptFilePath().path
        assertEquals(expectedPath, actualPath)
    }

    private ExternalScript createExternalScript(Map properties = [:]) {
        return new ExternalScript([
            scriptIdentifier: SCRIPT_IDENTIFIER,
            filePath: SCRIPT_FILE_PATH,
            author: "testUser",
            comment: "lets see if it works ;)",
        ] + properties)
    }
}
