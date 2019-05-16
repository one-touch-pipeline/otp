/*
 * Copyright 2011-2019 The OTP authors
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

package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*

class CellRangerMergingWorkPackageSpec extends Specification implements CellRangerFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                CellRangerMergingWorkPackage,
                Individual,
                Pipeline,
                Project,
                Realm,
                SeqType,
        ]
    }

    @Unroll
    void "CellRangerMergingWorkPackage, test mutually exclusive validator, fails validation (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        when:
        createMergingWorkPackage(expectedCells: expectedCells, enforcedCells: enforcedCells)

        then:
        ValidationException e = thrown()
        e.message =~ CellRangerMergingWorkPackage.MUTUAL_EXCLUSIVITY_ERROR

        where:
        expectedCells | enforcedCells
        null          | null
        5000          | 5000
    }

    @Unroll
    void "CellRangerMergingWorkPackage, test mutually exclusive validator, succeeds validation (expectedCells=#expectedCells, enforcedCells=#enforcedCells)"() {
        when:
        createMergingWorkPackage(expectedCells: expectedCells, enforcedCells: enforcedCells)

        then:
        noExceptionThrown()

        where:
        expectedCells | enforcedCells
        5000          | null
        null          | 5000
    }

    @Unroll
    void "CellRangerMergingWorkPackage, expectedCells and enforcedCells are editable"() {
        given:
        CellRangerMergingWorkPackage mwp = createMergingWorkPackage(expectedCells: 5000, enforcedCells: null)

        when:
        mwp.expectedCells = null
        mwp.enforcedCells = 5000
        mwp.save(flush: true)

        then:
        CellRangerMergingWorkPackage refreshedMwp = CellRangerMergingWorkPackage.get(mwp.id)
        refreshedMwp.expectedCells == null
        refreshedMwp.enforcedCells == 5000

        when:
        mwp.expectedCells = 5000
        mwp.enforcedCells = 5000
        mwp.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message =~ CellRangerMergingWorkPackage.MUTUAL_EXCLUSIVITY_ERROR
    }
}
