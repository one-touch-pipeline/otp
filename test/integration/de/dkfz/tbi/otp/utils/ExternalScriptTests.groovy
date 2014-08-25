package de.dkfz.tbi.otp.utils

import static org.junit.Assert.*
import java.util.Map;
import org.junit.*

class ExternalScriptTests {

    final String SCRIPT_IDENTIFIER = "TEST"

    void testGetLatestVersionOfScript() {
        ExternalScript externalScript1 = createExternalScript()
        externalScript1.save()
        assertEquals(externalScript1, ExternalScript.getLatestVersionOfScript(SCRIPT_IDENTIFIER))

        externalScript1.deprecatedDate = new Date()
        assert externalScript1.save()
        ExternalScript externalScript2 = createExternalScript()
        externalScript2.save()
        assertEquals(externalScript2, ExternalScript.getLatestVersionOfScript(SCRIPT_IDENTIFIER))
    }

    private ExternalScript createExternalScript(Map properties = [:]) {
        return new ExternalScript([
            scriptIdentifier: SCRIPT_IDENTIFIER,
            scriptName :"testScript",
            location: "/tmp/testfolder",
            author: "testUser",
            comment: "lets see if it works ;)",
        ] + properties)
    }
}
