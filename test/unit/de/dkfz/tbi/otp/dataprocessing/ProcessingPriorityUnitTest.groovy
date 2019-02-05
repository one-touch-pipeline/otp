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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*

@Build([
        DataFile,
        ProcessedMergedBamFile,
        QualityAssessmentPass,
])
@Mock([
        QualityAssessmentMergedPass,
        MergingCriteria,
])
class ProcessingPriorityUnitTest {

    private void assertPriority(def domainObject) {
        assert ProcessingPriority.NORMAL.priority == domainObject.processingPriority

        domainObject.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert ProcessingPriority.FAST_TRACK.priority == domainObject.processingPriority
    }


    @Test
    void testGetProcessingPriorityForAlignmentPass() {
        assertPriority(DomainFactory.createAlignmentPass())
    }

    @Test
    void testGetProcessingPriorityForSeqtrack() {
        assertPriority(DomainFactory.createSeqTrack())
    }

    @Test
    void testGetProcessingPriorityForMergingPass() {
        assertPriority(MergingPass.build())
    }

    @Test
    void testGetProcessingPriorityForQualityAssessmentPass() {
        assertPriority(QualityAssessmentPass.build())
    }

    @Test
    void testGetProcessingPriorityForQualityAssessmentMergedPass() {
        assertPriority(new QualityAssessmentMergedPass(
                abstractMergedBamFile: DomainFactory.createProcessedMergedBamFile(),
        ))
    }

    @Test
    void testGetProcessingPriorityForProcessedBamFile() {
        assertPriority(ProcessedBamFile.build())
    }

    @Test
    void testGetProcessingPriorityForProcessedMergedBamFile() {
        assertPriority(DomainFactory.createProcessedMergedBamFile())
    }

    @Test
    void testGetProcessingPriority() {
        Run run = Run.build()
        DataFile datafile = DomainFactory.createDataFile(run: run)
        datafile.project.processingPriority = ProcessingPriority.NORMAL.priority
        assert ProcessingPriority.NORMAL.priority == run.processingPriority

        DataFile datafileFasttrack = DomainFactory.createDataFile(run: run)
        datafileFasttrack.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert ProcessingPriority.FAST_TRACK.priority == run.processingPriority
    }
}
