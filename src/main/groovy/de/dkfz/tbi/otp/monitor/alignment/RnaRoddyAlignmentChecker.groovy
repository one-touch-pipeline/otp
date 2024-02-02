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
package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.Workflow

class RnaRoddyAlignmentChecker extends AbstractRoddyAlignmentChecker {

    final String workflowName = 'RnaAlignmentWorkflow'

    // delete this method when the RNA workflow is migrated to the new system
    @Override
    List<SeqTrack> getSeqTracksWithoutCorrespondingAlignmentConfig(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return []
        }
        return SamplePair.executeQuery("""
                    select
                        seqTrack
                    from
                        SeqTrack seqTrack
                    where
                        seqTrack in (:seqTracks)
                        and not exists (
                            select
                                config
                            from
                                ConfigPerProjectAndSeqType config
                            where
                                config.project = seqTrack.sample.individual.project
                                and config.seqType = seqTrack.seqType
                                and config.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                                and config.pipeline.name = :pipeLineName
                                and config.obsoleteDate is null
                        )
                """.toString(), [
                seqTracks   : seqTracks,
                pipeLineName: pipeLineName,
        ])
    }

    // delete this method when the RNA workflow is migrated to the new system
    @Override
    Map mergingWorkPackageForSeqTracks(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [
                    seqTracksWithoutMergingWorkpackage: [],
                    mergingWorkPackages               : [],
            ]
        }
        List<List<?>> list = (MergingWorkPackage.executeQuery("""
                    select
                        mergingWorkPackage,
                        seqTrack
                    from
                        MergingWorkPackage mergingWorkPackage
                        join mergingWorkPackage.seqTracks seqTrack
                    where
                        seqTrack in (:seqTracks)
                        and mergingWorkPackage.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                        and mergingWorkPackage.pipeline.name = :pipeLineName
                """.toString(), [
                seqTracks                                    : seqTracks,
                pipeLineName                                 : pipeLineName,
        ]) as List<List<?>>)

        List<SeqTrack> seqTracksWithoutMergingWorkpackage = seqTracks - list.collect {
            it[1] as SeqTrack
        }

        List<MergingWorkPackage> mergingWorkPackages = list.collect {
            it[0] as MergingWorkPackage
        }.unique()

        return [
                seqTracksWithoutMergingWorkpackage: seqTracksWithoutMergingWorkpackage,
                mergingWorkPackages               : mergingWorkPackages,
        ]
    }

    // delete this method when the RNA workflow is migrated to the new system
    @Override
    List<AbstractBamFile> getBamFileForMergingWorkPackage(List<MergingWorkPackage> mergingWorkPackages, boolean showFinished, boolean showWithdrawn) {
        if (!mergingWorkPackages) {
            return []
        }

        String filterFinished = showFinished ? '' :
                "and bamFile.fileOperationStatus != '${AbstractBamFile.FileOperationStatus.PROCESSED}'"
        String filterWithdrawnFinished = showWithdrawn ? '' :
                "and bamFile.withdrawn = false"

        return AbstractBamFile.executeQuery("""
                    select
                        bamFile
                    from
                        AbstractBamFile bamFile
                    where
                        bamFile.workPackage in (:mergingWorkPackage)
                        ${filterFinished}
                        ${filterWithdrawnFinished}
                        and bamFile.config.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                        and bamFile.config.pipeline.name = :pipeLineName
                        and bamFile.id = (
                            select
                                max(bamFile1.id)
                            from
                                AbstractBamFile bamFile1
                            where
                                bamFile1.workPackage = bamFile.workPackage
                        )
                """.toString(), [
                mergingWorkPackage: mergingWorkPackages,
                pipeLineName      : pipeLineName,
        ])
    }

    @Override
    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.RODDY_RNA_ALIGNMENT
    }

    final Workflow workflow = null

    @Override
    Set<SeqType> getSeqTypes() {
        return SeqTypeService.rnaAlignableSeqTypes as Set
    }
}
