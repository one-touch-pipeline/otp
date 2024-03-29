/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class ReferenceGenomeEntrySpec extends Specification implements DataTest, DomainUnitTest<ReferenceGenomeEntry> {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                ReferenceGenomeEntry,
                ReferenceGenome,
        ]
    }

    void testConstraints() {
        given:
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

        ReferenceGenomeEntry referenceGenomeEntryAgainAlias = new ReferenceGenomeEntry(
                        name: "chr3",
                        alias: "1",
                        referenceGenome: referenceGenome
                        )

        ReferenceGenomeEntry referenceGenomeEntryAgainBoth = new ReferenceGenomeEntry(
                        name: "chr1",
                        alias: "1",
                        referenceGenome: referenceGenome
                        )

        expect:
        referenceGenomeEntryAgainName.validate() == false
        referenceGenomeEntryAgainAlias.validate() == false
        referenceGenomeEntryAgainBoth.validate() == false
    }
}
