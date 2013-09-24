package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

@TestFor(ChromosomeIdentifierFilteringService)
@TestMixin(GrailsUnitTestMixin)
@Mock([ReferenceGenome, ReferenceGenomeEntry])
class ChromosomeIdentifierFilteringServiceTests {

    ChromosomeIdentifierFilteringService chromosomeIdentifierFilteringService

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry

    @Before
    void setUp() {
        chromosomeIdentifierFilteringService = new ChromosomeIdentifierFilteringService()
        chromosomeIdentifierFilteringService.referenceGenomeService = new ReferenceGenomeService()

        referenceGenome = new ReferenceGenome(
                        name: "hg19_1_24",
                        path: "referenceGenome",
                        fileNamePrefix: "prefixName"
                        )
        referenceGenome.save(flush: true, failOnError: true)

        referenceGenomeEntry = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "1",
                        classification: Classification.CHROMOSOME,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntry.save(flush: true, failOnError: true)

        ReferenceGenomeEntry referenceGenomeEntryTwo = new ReferenceGenomeEntry(
                        name: "chr2",
                        alias: "2",
                        classification: Classification.UNDEFINED,
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntryTwo.save(flush: true, failOnError: true)
    }

    @After
    void tearDown() {
        referenceGenome = null
        referenceGenomeEntry = null
        chromosomeIdentifierFilteringService = null
    }

    @Test
    void testfilteringCoverage() {
        List<String> chromosomesNamesExp = [referenceGenomeEntry.alias]
        List<String> chromosomesNamesAct = chromosomeIdentifierFilteringService.filteringCoverage(referenceGenome)
        assertEquals(chromosomesNamesExp, chromosomesNamesAct)
    }

    @Test(expected = IllegalArgumentException)
    void testfilteringCoverageReferenceGeomeIsNull() {
        referenceGenome = null
        List<String> chromosomesNamesAct = chromosomeIdentifierFilteringService.filteringCoverage(referenceGenome)
    }
}
