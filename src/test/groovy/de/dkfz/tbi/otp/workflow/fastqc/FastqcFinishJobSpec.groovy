/*
 * Copyright 2011-2021 The OTP authors
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
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.jobs.JobStage
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class FastqcFinishJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                WorkflowStep,
        ]
    }

    DataFile dataFile
    FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile([
            dataFile: dataFile,
    ])

    void "test updateDomain method"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()

        FastqcFinishJob job = Spy(FastqcFinishJob) {
            getSeqTrack(workflowStep) >> seqTrack
        }

        job.fastqcJobService = Mock(FastqcJobService) {
            getFastqcProcessedFile(_) >> fastqcProcessedFile
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * updateFastqcProcessedFile(_) >> { FastqcProcessedFile file ->
                assert file == fastqcProcessedFile
            }
        }

        when:
        job.updateDomains(workflowStep)

        then:
        job.seqTrackService = Mock(SeqTrackService) {
            1 * fillBaseCount(seqTrack)
            1 * markFastqcFinished(seqTrack)
        }
        job.jobStage == JobStage.FINISH
    }

    void "test inherited method execute(), JobStage is in finished state"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()

        FastqcFinishJob job = Spy(FastqcFinishJob) {
            getSeqTrack(workflowStep) >> seqTrack
        }

        job.fastqcJobService = Mock(FastqcJobService) {
            getFastqcProcessedFile(_) >> fastqcProcessedFile
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * updateFastqcProcessedFile(_) >> { FastqcProcessedFile file ->
                assert file == fastqcProcessedFile
            }
        }

        when:
        job.execute(workflowStep)

        then:
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(_, _) >> { WorkflowStep step, String message ->
                message == 'Finish the workflow.' }
        }
        job.seqTrackService = Mock(SeqTrackService) {
            1 * fillBaseCount(seqTrack)
            1 * markFastqcFinished(seqTrack)
        }
        job.jobStage == JobStage.FINISH
    }
}
