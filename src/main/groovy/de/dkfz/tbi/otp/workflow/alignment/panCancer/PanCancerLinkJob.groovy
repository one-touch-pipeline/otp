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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class PanCancerLinkJob extends AbstractLinkJob implements PanCancerShared {

    @Autowired
    PanCancerLinkFileService panCancerLinkFileService

    @Autowired
    PanCancerWorkFileService panCancerWorkFileService

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        List<LinkEntry> links = []

        links.add(new LinkEntry(link: panCancerLinkFileService.getBamFile(roddyBamFile), target: panCancerWorkFileService.getBamFile(roddyBamFile)))
        links.add(new LinkEntry(link: panCancerLinkFileService.getBaiFile(roddyBamFile), target: panCancerWorkFileService.getBaiFile(roddyBamFile)))
        links.add(new LinkEntry(link: panCancerLinkFileService.getMd5sumFile(roddyBamFile), target: panCancerWorkFileService.getMd5sumFile(roddyBamFile)))
        links.add(new LinkEntry(link: panCancerLinkFileService.getMergedQADirectory(roddyBamFile),
                target: panCancerWorkFileService.getMergedQADirectory(roddyBamFile)))

        // collect links for every execution store
        ([
                panCancerLinkFileService.getExecutionDirectories(roddyBamFile),
                panCancerWorkFileService.getExecutionDirectories(roddyBamFile),
        ].transpose() as List<List<Path>>).each {
            links.add(new LinkEntry(link: it[0], target: it[1]))
        }

        // collect links for the single lane qa
        Map<SeqTrack, Path> finalSingleLaneQADirectories = panCancerLinkFileService.getSingleLaneQADirectories(roddyBamFile)
        Map<SeqTrack, Path> workSingleLaneQADirectories = panCancerWorkFileService.getSingleLaneQADirectories(roddyBamFile)
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            Path singleLaneQaFinalDir = finalSingleLaneQADirectories.get(seqTrack)
            links.add(new LinkEntry(link: singleLaneQaFinalDir, target: singleLaneQaWorkDir))
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
