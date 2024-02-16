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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class FastqcValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, FastqcDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                WorkflowStep,
                WorkflowRun,
        ]
    }

    void "test getExpectedFiles"() {
        given:
        Path file1 = Paths.get('/file1')
        Path file2 = Paths.get('/file2')
        SeqTrack seqTrack = createSeqTrackWithTwoFastqFile()
        List<FastqcProcessedFile> fastqcProcessedFiles = seqTrack.sequenceFiles.collect {
            createFastqcProcessedFile([
                    sequenceFile: it,
            ])
        }
        WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: BashFastQcWorkflow.WORKFLOW
                ])
        ])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])

        FastqcClusterValidationJob job = new FastqcClusterValidationJob()

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputPath(fastqcProcessedFiles.first(), PathOption.REAL_PATH) >> file1
            1 * fastqcOutputPath(fastqcProcessedFiles.last(), PathOption.REAL_PATH) >> file2
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefacts(workflowStep, BashFastQcWorkflow.OUTPUT_FASTQC) >> fastqcProcessedFiles
            0 * _
        }

        when:
        List<Path> result = job.getExpectedFiles(workflowStep)

        then:
        TestCase.assertContainSame(result, [file1, file2])
    }

    void "test getExpectedDirectories"() {
        given:
        FastqcClusterValidationJob job = new FastqcClusterValidationJob()
        WorkflowStep workflowStep = createWorkflowStep()

        expect:
        [] == job.getExpectedDirectories(workflowStep)
    }

    void "test doFurtherValidation"() {
        given:
        FastqcClusterValidationJob job = new FastqcClusterValidationJob()
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        job.doFurtherValidation(workflowStep)

        then:
        notThrown(ValidationJobFailedException)
    }
}
