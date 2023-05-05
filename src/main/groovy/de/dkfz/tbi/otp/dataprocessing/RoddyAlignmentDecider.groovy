/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackService
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

/**
 * @deprecated class is part of the old workflow system, use {@link de.dkfz.tbi.otp.workflowExecution.decider.Decider} instead
 */
@CompileDynamic
@Deprecated
@SuppressWarnings('AbstractClassName')
abstract class RoddyAlignmentDecider extends AbstractAlignmentDecider {

    // See integration test for explanation in which cases workpackages needs processing
    @Override
    @Deprecated
    void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign) {
        RoddyBamFile latestValidBamFile = getLatestBamFileWhichHasBeenOrCouldBeCopied(workPackage)

        def setNeedsProcessing = {
            workPackage.needsProcessing = true
            assert workPackage.save(flush: true)
            seqTrack.log("Will align{0} for ${workPackage}.")
        }

        if (!latestValidBamFile ||
                !latestValidBamFile.containedSeqTracks.contains(seqTrack) ||
                latestValidBamFile.withdrawn) {
            setNeedsProcessing()
        } else {
            if (forceRealign) {
                seqTrack.log("Will not align{0} for ${workPackage} " +
                        "because the latest bam file already contains the seqtrack. " +
                        "(You can only realign if you set the latest bam file (ID ${latestValidBamFile.id}) to withdrawn).")
            } else {
                seqTrack.log("Will not align{0} for ${workPackage} " +
                        "because the latest bam file already contains the seqtrack.")
            }
        }
    }

    /** this method returns the latest bam file for the work packages which is transferred or not withdrawn */
    @Deprecated
    private static RoddyBamFile getLatestBamFileWhichHasBeenOrCouldBeCopied(MergingWorkPackage workPackage) {
        List<RoddyBamFile> bamFiles = RoddyBamFile.findAllByWorkPackage(workPackage, [sort: "identifier", order: "desc"])

        RoddyBamFile latestValidBamFile = bamFiles.find {
                    !it.withdrawn ||
                    it.fileOperationStatus == AbstractBamFile.FileOperationStatus.INPROGRESS ||
                    it.fileOperationStatus == AbstractBamFile.FileOperationStatus.PROCESSED
        }
        return latestValidBamFile
    }

    @Override
    @Deprecated
    boolean canPipelineAlign(SeqTrack seqTrack) {
        boolean canAlign = SeqTypeService.roddyAlignableSeqTypes.contains(seqTrack.seqType)
        if (canAlign && (RoddyWorkflowConfig.getLatestForProject(seqTrack.project, seqTrack.seqType, getPipeline(seqTrack)) == null)) {
            SeqTrackService.logToSeqTrack(seqTrack, "RoddyWorkflowConfig is missing for ${seqTrack.project} ${seqTrack.seqType} ${getPipeline(seqTrack).name}.")
            return false
        }
        return canAlign
    }
}
