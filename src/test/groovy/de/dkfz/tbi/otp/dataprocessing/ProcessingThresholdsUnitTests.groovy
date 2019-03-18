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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(ProcessingThresholds)
@Mock([Project, ProjectCategory, Realm, SeqType, SampleType])
class ProcessingThresholdsUnitTests {

    Double coverage = 30.00

    Integer numberOfLanes = 3

    @Test
    void testSaveNoProject() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.project = DomainFactory.createProject()
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNoSeqType() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.seqType = new SeqType()
        assertTrue(processingThresholds.validate())

    }

    @Test
    void testSaveNoSampleType() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        seqType: new SeqType(),
                        project: DomainFactory.createProject(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.sampleType = new SampleType()
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNoCoverageAndNumberOfLanes() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.coverage = coverage
        processingThresholds.numberOfLanes = numberOfLanes
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNoCoverage() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        )
        assertTrue(processingThresholds.validate())
    }


    @Test
    void testSaveCoverageBelowZero() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: -30.00,
                        )
        assertFalse(processingThresholds.validate())
    }

    @Test
    void testSaveNoNumberOfLanes() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        numberOfLanes: numberOfLanes,
                        )
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNumberOfLanesBelowZero() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        numberOfLanes: -3,
                        )
        assertFalse(processingThresholds.validate())
    }

    @Test
    void testSaveAllCorrect() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertTrue(processingThresholds.validate())
    }
}
