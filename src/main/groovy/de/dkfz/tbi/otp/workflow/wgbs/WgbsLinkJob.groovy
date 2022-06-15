/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflow.wgbs

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
@CompileStatic
class WgbsLinkJob extends PanCancerLinkJob {

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        List<LinkEntry> links = super.getLinkMap(workflowStep)

        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        links.add(new LinkEntry(link: roddyBamFileService.getFinalMetadataTableFile(roddyBamFile),
                target: roddyBamFileService.getWorkMetadataTableFile(roddyBamFile)))
        links.add(new LinkEntry(link: roddyBamFileService.getFinalMergedMethylationDirectory(roddyBamFile),
                target: roddyBamFileService.getWorkMergedMethylationDirectory(roddyBamFile)))

        if (roddyBamFile.hasMultipleLibraries()) {
            ([
                    roddyBamFileService.getFinalLibraryQADirectories(roddyBamFile).values().asList().sort(),
                    roddyBamFileService.getWorkLibraryQADirectories(roddyBamFile).values().asList().sort(),
            ].transpose() as List<List<Path>>).each {
                links.add(new LinkEntry(link: it[0], target: it[1]))
            }

            ([
                    roddyBamFileService.getFinalLibraryMethylationDirectories(roddyBamFile).values().asList().sort(),
                    roddyBamFileService.getWorkLibraryMethylationDirectories(roddyBamFile).values().asList().sort(),
            ].transpose() as List<List<Path>>).each {
                links.add(new LinkEntry(link: it[0], target: it[1]))
            }
        }

        return links
    }
}
