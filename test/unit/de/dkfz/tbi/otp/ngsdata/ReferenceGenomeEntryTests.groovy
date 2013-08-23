package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ReferenceGenomeEntry)
@Mock([ReferenceGenomeEntry, ReferenceGenome])
class ReferenceGenomeEntryTests {

    void testConstraints() {
        ReferenceGenome referenceGenome = new ReferenceGenome(
                        name: "refGen",
                        path: "filePath",
                        fileNamePrefix: "prefix"
                        )
        referenceGenome.save(flush: true)

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
