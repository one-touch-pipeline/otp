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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class MergingSetAssignmentSpec extends Specification implements DataTest {

    MergingSet mergingSet = null
    ProcessedBamFile processedBamFile = null

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MergingCriteria,
                MergingSet,
                MergingSetAssignment,
                ProcessedBamFile,
        ]
    }

    void createData() {
        processedBamFile = DomainFactory.createProcessedBamFile([
                alignmentPass: DomainFactory.createAlignmentPass([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createDefaultOtpPipeline(),
                        ]),
                ]),
        ])

        mergingSet = DomainFactory.createMergingSet([
                mergingWorkPackage: processedBamFile.mergingWorkPackage,
        ])
    }

    void testSave() {
        given:
        createData()

        when:
        MergingSetAssignment mtm = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedBamFile,
        )

        then:
        mtm.validate()
        mtm.save(flush: true)
    }

    void "testConstraints, when mergingSet is null, then validation should fail"() {
        given:
        createData()

        when:
        MergingSetAssignment mtm = new MergingSetAssignment(
                bamFile: processedBamFile,
        )

        then:
        TestCase.assertAtLeastExpectedValidateError(mtm, 'mergingSet', 'nullable', null)
    }

    void "testConstraints, when bamFile is null, then validation should fail"() {
        given:
        createData()

        when:
        MergingSetAssignment mtm = new MergingSetAssignment(
                mergingSet: mergingSet,
        )

        then:
        TestCase.assertValidateError(mtm, 'bamFile', 'nullable', null)
    }

    void "testConstraints, when mergingWorkPackage of mergingSet and processedBamFile differ, then validation should fail"() {
        given:
        createData()

        when:
        MergingSetAssignment mtm = new MergingSetAssignment(
                bamFile: processedBamFile,
                mergingSet: DomainFactory.createMergingSet(),
        )

        then:
        TestCase.assertValidateError(mtm, 'bamFile', 'validator.invalid', processedBamFile)
    }
}
