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
package de.dkfz.tbi.otp.workflow.alignment.rna

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path

@Component
@Slf4j
class RnaAlignmentLinkJob extends AbstractLinkJob implements RnaAlignmentShared {
    @Autowired
    RnaRoddyBamFileService rnaRoddyBamFileService

    /**
     * Produce a linking for all contained Files, except the hidden ones starting with '.'
     * @param workflowStep is the provided workflowStep
     * @return a list of links from working directory to base directory
     */
    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        List<LinkEntry> links = []

        Path outputDir = getWorkDirectory(workflowStep)
        Path linkDir = rnaRoddyBamFileService.getBaseDirectory(getRoddyBamFile(workflowStep))

        // Filter out all hidden files indicated by .
        DirectoryStream<Path> visibleFiles = Files.newDirectoryStream(outputDir) { path ->
            return !path.fileName.toString().startsWith(".")
        }

        visibleFiles.each { outputFile ->
            links.add(new LinkEntry(target: outputFile, link: linkDir.resolve(outputFile.fileName)))
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
