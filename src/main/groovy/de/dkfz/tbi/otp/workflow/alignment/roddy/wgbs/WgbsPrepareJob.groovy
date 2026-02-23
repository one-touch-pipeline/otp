/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.PanCancerWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.WgbsAlignmentWorkFileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.alignment.RoddyAlignmentPrepareService
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerShared
import de.dkfz.tbi.otp.workflow.jobs.AbstractPrepareJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

@Component
@Slf4j
class WgbsPrepareJob extends AbstractPrepareJob implements PanCancerShared {

    @Autowired
    RoddyConfigValueService roddyConfigValueService

    @Autowired
    RoddyAlignmentPrepareService roddyAlignmentPrepareService

    @Autowired
    PanCancerWorkFileService panCancerWorkFileService

    @Autowired
    WgbsAlignmentWorkFileService wgbsAlignmentWorkFileService

    @Override
    protected Path buildWorkDirectoryPath(WorkflowStep workflowStep) {
        return panCancerWorkFileService.getDirectoryPath(getBamFile(workflowStep))
    }

    @Override
    protected Collection<LinkEntry> generateMapForLinking(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void doFurtherPreparation(WorkflowStep workflowStep) {
        RoddyBamFile roddyBamFile = getBamFile(workflowStep)
        List<SeqTrack> seqTracks = getSeqTracks(workflowStep)

        roddyAlignmentPrepareService.prepare(roddyBamFile, seqTracks)

        Path metadataFile = wgbsAlignmentWorkFileService.getMetadataTableFile(roddyBamFile)
        String content = roddyConfigValueService.createMetadataTable(seqTracks)
        Files.deleteIfExists(metadataFile)
        fileService.createFileWithContent(metadataFile, content, roddyBamFile.project.unixGroup)
    }
}
