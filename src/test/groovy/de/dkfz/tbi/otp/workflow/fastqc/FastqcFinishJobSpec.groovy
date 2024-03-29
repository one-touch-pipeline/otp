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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.jobs.JobStage
import de.dkfz.tbi.otp.workflowExecution.*

class FastqcFinishJobSpec extends Specification implements DataTest, FastqcWorkflowDomainFactory {

    RawSequenceFile rawSequenceFile1
    RawSequenceFile rawSequenceFile2

    FastqcProcessedFile fastqcProcessedFile1 = new FastqcProcessedFile([
            sequenceFile: rawSequenceFile1,
    ])
    FastqcProcessedFile fastqcProcessedFile2 = new FastqcProcessedFile([
            sequenceFile: rawSequenceFile2,
    ])

    List<FastqcProcessedFile> fastqcProcessedFileList = [
            fastqcProcessedFile1,
            fastqcProcessedFile2,
    ]

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                RawSequenceFile,
                WorkflowStep,
        ]
    }

    void "test updateDomain method"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateWesFastqcWorkflow(),
                ]),
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()

        FastqcFinishJob job = new FastqcFinishJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefact(workflowStep, WesFastQcWorkflow.INPUT_FASTQ) >> seqTrack
            _ * getOutputArtefacts(workflowStep, WesFastQcWorkflow.OUTPUT_FASTQC) >> fastqcProcessedFileList
            0 * _
        }

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * updateFastqcProcessedFiles(fastqcProcessedFileList) >> { params ->
                List<FastqcProcessedFile> fileList = params.flatten()
                assert fileList == fastqcProcessedFileList
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
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateWesFastqcWorkflow(),
                ]),
        ])
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()

        FastqcFinishJob job = new FastqcFinishJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getInputArtefact(workflowStep, WesFastQcWorkflow.INPUT_FASTQ) >> seqTrack
            _ * getOutputArtefacts(workflowStep, WesFastQcWorkflow.OUTPUT_FASTQC) >> fastqcProcessedFileList
            0 * _
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * updateFastqcProcessedFiles(_) >> { param ->
                List<FastqcProcessedFile> fileList = param.flatten()
                assert fileList == fastqcProcessedFileList
            }
        }

        when:
        job.execute(workflowStep)

        then:
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(_, _) >> { WorkflowStep step, String message ->
                message == 'Finish the workflow.'
            }
        }
        job.seqTrackService = Mock(SeqTrackService) {
            1 * fillBaseCount(seqTrack)
            1 * markFastqcFinished(seqTrack)
        }
        job.jobStage == JobStage.FINISH
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
        }
    }
}
