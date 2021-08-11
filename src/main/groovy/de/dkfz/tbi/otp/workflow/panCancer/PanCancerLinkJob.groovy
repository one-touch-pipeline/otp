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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.jobs.AbstractLinkJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.FileSystem

@Component
@Slf4j
@CompileStatic
class PanCancerLinkJob extends AbstractLinkJob implements PanCancerShared {

    @Override
    protected List<LinkEntry> getLinkMap(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)
        FileSystem fs = getFileSystem(workflowStep)

        List<LinkEntry> links = []

        links.add(new LinkEntry(link: fileService.toPath(roddyBamFile.finalBamFile, fs), target: fileService.toPath(roddyBamFile.workBamFile, fs)))
        links.add(new LinkEntry(link: fileService.toPath(roddyBamFile.finalBaiFile, fs), target: fileService.toPath(roddyBamFile.workBaiFile, fs)))
        links.add(new LinkEntry(link: fileService.toPath(roddyBamFile.finalMd5sumFile, fs), target: fileService.toPath(roddyBamFile.workMd5sumFile, fs)))
        links.add(new LinkEntry(link: fileService.toPath(roddyBamFile.finalMergedQADirectory, fs),
                target: fileService.toPath(roddyBamFile.workMergedQADirectory, fs)))

        //collect links for every execution store
        ([
                roddyBamFile.finalExecutionDirectories,
                roddyBamFile.workExecutionDirectories,
        ].transpose() as List<List<File>>).each {
            links.add(new LinkEntry(link: fileService.toPath(it[0], fs), target: fileService.toPath(it[1], fs)))
        }

        //collect links for the single lane qa
        Map<SeqTrack, File> finalSingleLaneQADirectories = roddyBamFile.finalSingleLaneQADirectories
        Map<SeqTrack, File> workSingleLaneQADirectories = roddyBamFile.workSingleLaneQADirectories
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            File singleLaneQaFinalDir = finalSingleLaneQADirectories.get(seqTrack)
            links.add(new LinkEntry(link: fileService.toPath(singleLaneQaFinalDir, fs), target: fileService.toPath(singleLaneQaWorkDir, fs)))
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
