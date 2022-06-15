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
package de.dkfz.tbi.otp.workflow.panCancer

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
@CompileStatic
class PanCancerLinkJob extends AbstractLinkJob implements PanCancerShared {

    @Autowired
    RoddyBamFileService roddyBamFileService

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        List<LinkEntry> links = []

        links.add(new LinkEntry(link: roddyBamFileService.getFinalBamFile(roddyBamFile), target: roddyBamFileService.getWorkBamFile(roddyBamFile)))
        links.add(new LinkEntry(link: roddyBamFileService.getFinalBaiFile(roddyBamFile), target: roddyBamFileService.getWorkBaiFile(roddyBamFile)))
        links.add(new LinkEntry(link: roddyBamFileService.getFinalMd5sumFile(roddyBamFile), target: roddyBamFileService.getWorkMd5sumFile(roddyBamFile)))
        links.add(new LinkEntry(link: roddyBamFileService.getFinalMergedQADirectory(roddyBamFile),
                target: roddyBamFileService.getWorkMergedQADirectory(roddyBamFile)))

        //collect links for every execution store
        ([
                roddyBamFileService.getFinalExecutionDirectories(roddyBamFile),
                roddyBamFileService.getWorkExecutionDirectories(roddyBamFile),
        ].transpose() as List<List<Path>>).each {
            links.add(new LinkEntry(link: it[0], target: it[1]))
        }

        //collect links for the single lane qa
        Map<SeqTrack, Path> finalSingleLaneQADirectories = roddyBamFileService.getFinalSingleLaneQADirectories(roddyBamFile)
        Map<SeqTrack, Path> workSingleLaneQADirectories = roddyBamFileService.getWorkSingleLaneQADirectories(roddyBamFile)
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
