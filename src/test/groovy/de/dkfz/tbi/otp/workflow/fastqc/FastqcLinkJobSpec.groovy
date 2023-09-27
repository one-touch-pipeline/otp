/*
 * Copyright 2011-2023 The OTP authors
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
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Paths

class FastqcLinkJobSpec extends Specification implements DataTest, FastqcWorkflowDomainFactory, FastqcDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                RawSequenceFile,
                WorkflowStep,
        ]
    }

    void "test getLinkMap"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: createWesFastqcWorkflowVersion(),
                ]),
        ])
        FastqcProcessedFile file1 = createFastqcProcessedFile()
        FastqcProcessedFile file2 = createFastqcProcessedFile()

        FastqcLinkJob job = new FastqcLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefacts(workflowStep, FastqcLinkJob.de_dkfz_tbi_otp_workflow_fastqc_FastqcShared__OUTPUT_ROLE) >> [file1, file2]
            0 * _
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            fastqcOutputPath(file1) >> Paths.get("/file1-out-link")
            fastqcHtmlPath(file1) >> Paths.get("/file1-htm-link")
            fastqcOutputMd5sumPath(file1) >> Paths.get("/file1-md5-link")
            fastqcOutputPath(file1, PathOption.REAL_PATH) >> Paths.get("/file1-out-real")
            fastqcHtmlPath(file1, PathOption.REAL_PATH) >> Paths.get("/file1-htm-real")
            fastqcOutputMd5sumPath(file1, PathOption.REAL_PATH) >> Paths.get("/file1-md5-real")

            fastqcOutputPath(file2) >> Paths.get("/file2-out-link")
            fastqcHtmlPath(file2) >> Paths.get("/file2-htm-link")
            fastqcOutputMd5sumPath(file2) >> Paths.get("/file2-md5-link")
            fastqcOutputPath(file2, PathOption.REAL_PATH) >> Paths.get("/file2-out-real")
            fastqcHtmlPath(file2, PathOption.REAL_PATH) >> Paths.get("/file2-htm-real")
            fastqcOutputMd5sumPath(file2, PathOption.REAL_PATH) >> Paths.get("/file2-md5-real")
        }
        job.fileService = Mock(FileService) {
            fileIsReadable(_, _) >> true
        }

        expect:
        CollectionUtils.containSame(job.getLinkMap(workflowStep), [
                new LinkEntry(link: Paths.get("/file1-out-link"), target: Paths.get("/file1-out-real")),
                new LinkEntry(link: Paths.get("/file1-htm-link"), target: Paths.get("/file1-htm-real")),
                new LinkEntry(link: Paths.get("/file1-md5-link"), target: Paths.get("/file1-md5-real")),
                new LinkEntry(link: Paths.get("/file2-out-link"), target: Paths.get("/file2-out-real")),
                new LinkEntry(link: Paths.get("/file2-htm-link"), target: Paths.get("/file2-htm-real")),
                new LinkEntry(link: Paths.get("/file2-md5-link"), target: Paths.get("/file2-md5-real")),
        ])
    }

    void "test getLinkMap, when md5 and/or html files don't exist, don't link them"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: createWesFastqcWorkflowVersion(),
                ]),
        ])
        FastqcProcessedFile file1 = createFastqcProcessedFile()

        FastqcLinkJob job = new FastqcLinkJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefacts(workflowStep,
                    FastqcLinkJob.de_dkfz_tbi_otp_workflow_fastqc_FastqcShared__OUTPUT_ROLE) >> [file1]
            0 * _
        }
        job.fastqcDataFilesService = Mock(FastqcDataFilesService) {
            fastqcHtmlPath(file1, PathOption.REAL_PATH) >> Paths.get("/file1-htm-real")
            fastqcOutputMd5sumPath(file1, PathOption.REAL_PATH) >> Paths.get("/file1-md5-real")
        }
        job.fileService = Mock(FileService) {
            fileIsReadable(Paths.get("/file1-htm-real"), _) >> htmlExists
            fileIsReadable(Paths.get("/file1-md5-real"), _) >> md5Exists
        }

        when:
        List<LinkEntry> result = job.getLinkMap(workflowStep)

        then:
        result*.target.contains(Paths.get("/file1-htm-real")) == htmlExists
        result*.target.contains(Paths.get("/file1-md5-real")) == md5Exists

        where:
        htmlExists | md5Exists
        true       | true
        false      | true
        true       | false
        false      | false
    }
}
