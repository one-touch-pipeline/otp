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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class FastqcValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, FastqcDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqcProcessedFile,
                WorkflowStep,
                WorkflowRun,
        ]
    }

    void "test getExpectedFiles"() {
        given:
        Path file1 = Paths.get('/file1')
        Path file2 = Paths.get('/file2')
        SeqTrack seqTrack = createSeqTrackWithTwoDataFile()
        List<FastqcProcessedFile> fastqcProcessedFiles = seqTrack.dataFiles.collect {
            createFastqcProcessedFile([
                    dataFile: it,
            ])
        }
        WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: FastqcWorkflow.WORKFLOW
                ])
        ])
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])

        FastqcValidationJob job = new FastqcValidationJob()

        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputPath(fastqcProcessedFiles.first()) >> file1
            1 * fastqcOutputPath(fastqcProcessedFiles.last()) >> file2
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefacts(workflowStep, FastqcWorkflow.OUTPUT_FASTQC) >> fastqcProcessedFiles
        }

        when:
        List<Path> result = job.getExpectedFiles(workflowStep)

        then:
        TestCase.assertContainSame(result, [file1, file2])
    }

    void "test getExpectedDirectories"() {
        given:
        FastqcValidationJob job = new FastqcValidationJob()
        WorkflowStep workflowStep = createWorkflowStep()

        expect:
        [] == job.getExpectedDirectories(workflowStep)
    }

    void "test doFurtherValidationAndReturnProblems"() {
        given:
        FastqcValidationJob job = new FastqcValidationJob()
        WorkflowStep workflowStep = createWorkflowStep()

        expect:
        [] == job.doFurtherValidationAndReturnProblems(workflowStep)
    }
}
