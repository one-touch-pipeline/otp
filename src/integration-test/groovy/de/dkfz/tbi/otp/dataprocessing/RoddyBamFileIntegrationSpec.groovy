/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class RoddyBamFileIntegrationSpec extends Specification {

    static final Long ARBITRARY_UNUSED_VALUE = 1

    static final Map ARBITRARY_QA_VALUES = [
            qcBasesMapped                  : ARBITRARY_UNUSED_VALUE,
            totalReadCounter               : ARBITRARY_UNUSED_VALUE,
            qcFailedReads                  : ARBITRARY_UNUSED_VALUE,
            duplicates                     : ARBITRARY_UNUSED_VALUE,
            totalMappedReadCounter         : ARBITRARY_UNUSED_VALUE,
            pairedInSequencing             : ARBITRARY_UNUSED_VALUE,
            pairedRead2                    : ARBITRARY_UNUSED_VALUE,
            pairedRead1                    : ARBITRARY_UNUSED_VALUE,
            properlyPaired                 : ARBITRARY_UNUSED_VALUE,
            withItselfAndMateMapped        : ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChr   : ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChrMaq: ARBITRARY_UNUSED_VALUE,
            singletons                     : ARBITRARY_UNUSED_VALUE,
            insertSizeMedian               : ARBITRARY_UNUSED_VALUE,
            insertSizeSD                   : ARBITRARY_UNUSED_VALUE,
            referenceLength                : ARBITRARY_UNUSED_VALUE,
    ].asImmutable()

    static final Map NULL_QA_VALUES = [
            qcBasesMapped                  : null,
            totalReadCounter               : null,
            qcFailedReads                  : null,
            duplicates                     : null,
            totalMappedReadCounter         : null,
            pairedInSequencing             : null,
            pairedRead2                    : null,
            pairedRead1                    : null,
            properlyPaired                 : null,
            withItselfAndMateMapped        : null,
            withMateMappedToDifferentChr   : null,
            withMateMappedToDifferentChrMaq: null,
            singletons                     : null,
            insertSizeMedian               : null,
            insertSizeSD                   : null,
            referenceLength                : null,
            percentageMatesOnDifferentChr  : null,
            insertSizeCV  : null,
    ].asImmutable()

    void "test getNumberOfReadsFromQa"() {
        given:
        long pairedRead = DomainFactory.counter++
        long numberOfReads = 2 * pairedRead
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRoddyMergedBamQa([
                abstractBamFile: roddyBamFile,
                pairedRead1    : pairedRead,
                pairedRead2    : pairedRead,
                referenceLength: 0,
        ])

        expect:
        numberOfReads == roddyBamFile.numberOfReadsFromQa
    }

    void "test getQualityAssessment"() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        DomainFactory.createRoddyMergedBamQa(
                NULL_QA_VALUES + [
                        abstractBamFile              : bamFile,
                        chromosome                   : '12',
                        referenceLength              : 1,
                        genomeWithoutNCoverageQcBases: 1,
                ]
        )

        RoddyMergedBamQa mergedQa = DomainFactory.createRoddyMergedBamQa(
                ARBITRARY_QA_VALUES + [
                        abstractBamFile              : bamFile,
                        chromosome                   : RoddyQualityAssessment.ALL,
                        insertSizeCV                 : 123,
                        percentageMatesOnDifferentChr: 0.123,
                        genomeWithoutNCoverageQcBases: 1,
                ]
        )

        expect:
        mergedQa == bamFile.qualityAssessment
    }
}
