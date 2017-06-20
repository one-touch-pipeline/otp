package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestFor(ChromosomeIdentifierFilteringService)
@TestMixin(GrailsUnitTestMixin)
@Mock([ReferenceGenome, ReferenceGenomeEntry])
class ChromosomeIdentifierFilteringServiceTests {

    ChromosomeIdentifierFilteringService chromosomeIdentifierFilteringService

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    ReferenceGenome referenceGenome
    ReferenceGenomeEntry referenceGenomeEntry

    @Before
    void setUp() {
        chromosomeIdentifierFilteringService = new ChromosomeIdentifierFilteringService()
        chromosomeIdentifierFilteringService.referenceGenomeService = new ReferenceGenomeService()

        referenceGenome = DomainFactory.createReferenceGenome([
                        name: "hg19_1_24",
                        path: "referenceGenome",
                        fileNamePrefix: "prefixName",
                        length: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthRefChromosomes: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthRefChromosomesWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        ])

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
