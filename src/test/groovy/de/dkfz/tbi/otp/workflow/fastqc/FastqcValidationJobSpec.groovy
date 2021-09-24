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
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class FastqcValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {
    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqImportInstance,
                SampleType,
                Sample,
                WorkflowStep,
        ]
    }

    void "test getExpectedFiles"() {
        given:
        SeqTrack seqTrack = createSeqTrackWithOneDataFile()
        WorkflowStep workflowStep = createWorkflowStep()
        FastqcValidationJob job = Spy(FastqcValidationJob) {
            1 * getSeqTrack(workflowStep) >> seqTrack
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            1 * fastqcOutputPath(_) >> { DataFile dataFile -> Paths.get(dataFile.fileName) }
        }

        when:
        List<Path> result = job.getExpectedFiles(workflowStep)

        then:
        containSame(result*.fileName*.toString(), seqTrack.dataFiles*.fileName)
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
