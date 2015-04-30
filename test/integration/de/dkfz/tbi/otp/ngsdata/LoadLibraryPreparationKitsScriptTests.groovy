package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase

class LoadLibraryPreparationKitsScriptTests extends GroovyScriptAwareTestCase {

    void testScript() {
        runScript("scripts/LibraryPreparationKit/LoadLibraryPreparationKits.groovy")
        // find the first in the list
        LibraryPreparationKit kit = LibraryPreparationKit.findByName("Agilent SureSelect V3")
        assertNotNull kit
        // find the last in the list
        kit = LibraryPreparationKit.findByName("Ion AmpliSeq Exome Kit")
        assertNotNull kit
        runScript("scripts/LibraryPreparationKit/LoadLibraryPreparationKits.groovy")
        List kits = LibraryPreparationKit.list()
        assertTrue (kits.size == 12)
    }
}
