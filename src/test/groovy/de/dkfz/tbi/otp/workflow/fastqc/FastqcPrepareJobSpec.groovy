/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.*

class FastqcPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                WorkflowStep,
        ]
    }

    void "test doFurtherPreparation"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()

        FastqcPrepareJob job = Spy(FastqcPrepareJob) {
            1 * getSeqTrack(workflowStep) >> seqTrack
        }
        job.notificationCreator = Mock(NotificationCreator)

        when:
        job.doFurtherPreparation(workflowStep)

        then:
        1 * job.notificationCreator.setStartedForSeqTracks([seqTrack], OtrsTicket.ProcessingStep.FASTQC)
        seqTrack.fastqcState == SeqTrack.DataProcessingState.IN_PROGRESS
    }

    void "test buildWorkDirectoryPath"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()
        Path workDirectory = Paths.get('/workDirectory')

        FastqcPrepareJob job = Spy(FastqcPrepareJob) {
            1 * getSeqTrack(workflowStep) >> seqTrack
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputDirectory(_) >> workDirectory
        }
        job.fileSystemService = Mock(FileSystemService) {
            1 * getRemoteFileSystem(_) >> FileSystems.default
        }

        expect:
        workDirectory == job.buildWorkDirectoryPath(workflowStep)
    }
}
