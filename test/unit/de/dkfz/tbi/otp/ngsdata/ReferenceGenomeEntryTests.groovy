package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Assert
import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ReferenceGenomeEntry)
@Mock([ReferenceGenomeEntry, ReferenceGenome])
class ReferenceGenomeEntryTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    @Test
    void testConstraints() {
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome([
                        name: "refGen",
                        path: "filePath",
                        fileNamePrefix: "prefix",
                        length: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthRefChromosomes: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        lengthRefChromosomesWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
                        ])

        ReferenceGenomeEntry referenceGenomeEntry = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "1",
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntry.save(flush: true)

        ReferenceGenomeEntry referenceGenomeEntryTwo = new ReferenceGenomeEntry(
                        name: "chr2",
                        alias: "2",
                        referenceGenome: referenceGenome
                        )
        referenceGenomeEntryTwo.save(flush: true)

        ReferenceGenomeEntry referenceGenomeEntryAgainName = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "3",
                        referenceGenome: referenceGenome
                        )
        Assert.assertTrue !referenceGenomeEntryAgainName.validate()

        ReferenceGenomeEntry referenceGenomeEntryAgainAlias = new ReferenceGenomeEntry(
                        name: "chr3",
                        alias: "1",
                        referenceGenome: referenceGenome
                        )
        Assert.assertTrue !referenceGenomeEntryAgainAlias.validate()

        ReferenceGenomeEntry referenceGenomeEntryAgainBoth = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "1",
                        referenceGenome: referenceGenome
                        )
        Assert.assertTrue !referenceGenomeEntryAgainBoth.validate()
    }
}
