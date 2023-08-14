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

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
class FastqcLinkJob extends AbstractLinkJob implements FastqcShared {

    FastqcDataFilesService fastqcDataFilesService

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        List<FastqcProcessedFile> files = getFastqcProcessedFiles(workflowStep)

        List<LinkEntry> links = []
        files.each {
            links.add(new LinkEntry(link: fastqcDataFilesService.fastqcOutputPath(it),
                    target: fastqcDataFilesService.fastqcOutputPath(it, PathOption.REAL_PATH)))
            if (fileService.fileIsReadable(fastqcDataFilesService.fastqcHtmlPath(it, PathOption.REAL_PATH), workflowStep.realm)) {
                links.add(new LinkEntry(link: fastqcDataFilesService.fastqcHtmlPath(it),
                        target: fastqcDataFilesService.fastqcHtmlPath(it, PathOption.REAL_PATH)))
            }
            if (fileService.fileIsReadable(fastqcDataFilesService.fastqcOutputMd5sumPath(it, PathOption.REAL_PATH), workflowStep.realm)) {
                links.add(new LinkEntry(link: fastqcDataFilesService.fastqcOutputMd5sumPath(it),
                        target: fastqcDataFilesService.fastqcOutputMd5sumPath(it, PathOption.REAL_PATH)))
            }
        }

        return links
    }

    @Override
    protected void doFurtherWork(WorkflowStep workflowStep) {
    }

    @Override
    protected void saveResult(WorkflowStep workflowStep) {
    }
}
