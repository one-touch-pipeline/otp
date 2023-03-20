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
package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflowExecution.Workflow

class CellRangerAlignmentChecker extends AbstractAlignmentChecker {

    final String workflowName = "CellRangerWorkflow"

    // delete this method when the Cell Ranger workflow is migrated to the new system
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

    // delete this method when the Cell Ranger workflow is migrated to the new system
    @Override
    Map mergingWorkPackageForSeqTracks(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [
                    seqTracksWithoutMergingWorkpackage: [],
                    mergingWorkPackages               : [],
            ]
        }
        List list = MergingWorkPackage.executeQuery("""
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
        ])

        List seqTracksWithoutMergingWorkpackage = seqTracks - list.collect {
            it[1]
        }

        List<MergingWorkPackage> mergingWorkPackages = list.collect {
            it[0]
        }.unique()

        return [
                seqTracksWithoutMergingWorkpackage: seqTracksWithoutMergingWorkpackage,
                mergingWorkPackages               : mergingWorkPackages,
        ]
    }

    @Override
    Pipeline.Name getPipeLineName() {
        return Pipeline.Name.CELL_RANGER
    }

    final Workflow workflow = null

    @Override
    Set<SeqType> getSeqTypes() {
        return SeqTypeService.cellRangerAlignableSeqTypes as Set
    }
}
