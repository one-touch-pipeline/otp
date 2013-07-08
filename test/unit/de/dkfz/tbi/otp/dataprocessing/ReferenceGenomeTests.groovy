package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import grails.test.mixin.support.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(ReferenceGenomeService)
@TestMixin(GrailsUnitTestMixin)
@Mock([ReferenceGenome])
class ReferenceGenomeTests {

    @Test
    void testValidationMethodOfReferenceGenomeNameNotUnique() {
        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)
        assertTrue(referenceGenome.validate())

        ReferenceGenome referenceGenome1 = new ReferenceGenome()
        referenceGenome1.name = "hg19_1_24"
        referenceGenome1.path = "test"
        referenceGenome1.fileNamePrefix = "prefixName"
        referenceGenome1.save(flush: true)
        assertFalse(referenceGenome1.validate())
    }

    @Test
    void testValidationMethodOfReferenceGenomePathNotUnique() {
        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)
        assertTrue(referenceGenome.validate())

        ReferenceGenome referenceGenome1 = new ReferenceGenome()
        referenceGenome1.name = "test"
        referenceGenome1.path = "referenceGenome"
        referenceGenome1.fileNamePrefix = "prefixName"
        referenceGenome1.save(flush: true)
        assertFalse(referenceGenome1.validate())
    }

    @Test
    void testValidationMethodOfReferenceGenomePrefixIsEmpty() {
        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = "test"
        referenceGenome.path = "test"
        referenceGenome.fileNamePrefix = ""
        referenceGenome.save(flush: true)
        assertFalse(referenceGenome.validate())
    }

    @Test
    void testValidationMethodOfReferenceGenomePathIsEmpty() {
        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = "test"
        referenceGenome.path = ""
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)
        assertFalse(referenceGenome.validate())
    }

    @Test
    void testValidationMethodOfReferenceGenomeNameIsEmpty() {
        ReferenceGenome referenceGenome = new ReferenceGenome()
        referenceGenome.name = ""
        referenceGenome.path = "test"
        referenceGenome.fileNamePrefix = "prefixName"
        referenceGenome.save(flush: true)
        assertFalse(referenceGenome.validate())
    }
}
