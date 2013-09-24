package de.dkfz.tbi.otp.ngsdata;

import static org.junit.Assert.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

class LoadExomeEnrichmentKitsScriptTests extends GroovyScriptAwareIntegrationTest {

    void testScript() {
        run("scripts/ExomeEnrichmentKit/LoadExomeEnrichmentKits.groovy")
        // find the first in the list
        ExomeEnrichmentKit kit = ExomeEnrichmentKit.findByName("Agilent SureSelect V3")
        assertNotNull kit
        // find the last in the list
        kit = ExomeEnrichmentKit.findByName("Ion AmpliSeq Exome Kit")
        assertNotNull kit
        run("scripts/ExomeEnrichmentKit/LoadExomeEnrichmentKits.groovy")
        List kits = ExomeEnrichmentKit.list()
        assertTrue (kits.size == 11)
    }
}
