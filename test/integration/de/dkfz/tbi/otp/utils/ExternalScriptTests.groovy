package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.testing.AbstractIntegrationTest

import static org.junit.Assert.*
import java.util.Map;
import org.junit.*
import de.dkfz.tbi.TestCase

class ExternalScriptTests {

    final String SCRIPT_IDENTIFIER = "TEST"

    void testGetLatestVersionOfScript() {
        final File testDir = TestCase.uniqueNonExistentPath

        ExternalScript externalScript1 = createExternalScript(filePath: new File(testDir, 'v1/testScript.sh').path)
        externalScript1.save()
        assertEquals(externalScript1, ExternalScript.getLatestVersionOfScript(SCRIPT_IDENTIFIER, "v1"))

        externalScript1.deprecatedDate = new Date()
        assert externalScript1.save()
        ExternalScript externalScript2 = createExternalScript(filePath: new File(testDir, 'v2/testScript.sh').path)
        externalScript2.save()
        assertEquals(externalScript2, ExternalScript.getLatestVersionOfScript(SCRIPT_IDENTIFIER, "v1"))
    }

    @Test(expected = AssertionError)
    void testGetLatestVersionOfScript_NoScriptIdentifier_shouldFail() {
        ExternalScript.getLatestVersionOfScript(SCRIPT_IDENTIFIER, null)
    }

    @Test(expected = AssertionError)
    void testGetLatestVersionOfScript_NoScriptVersion_shouldFail() {
        ExternalScript.getLatestVersionOfScript(null, "v1")
    }

    @Test(expected = AssertionError)
    void testGetLatestVersionOfScript_NoScriptInDatabaseForInputParameter_shouldFail() {
        ExternalScript.getLatestVersionOfScript(SCRIPT_IDENTIFIER, "v1")
    }

    private ExternalScript createExternalScript(Map properties = [:]) {
        return new ExternalScript([
            scriptIdentifier: SCRIPT_IDENTIFIER,
            scriptVersion: 'v1',
            author: "testUser",
            comment: "lets see if it works ;)",
        ] + properties)
    }
}
