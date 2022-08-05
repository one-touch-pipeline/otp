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
package de.dkfz.tbi.otp.job.jobs.cellRanger

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@Component('cellRangerAlignmentStartJob')
@Scope('singleton')
@Slf4j
class CellRangerStartJob extends AbstractAlignmentStartJob implements RestartableStartJob {

    @Override
    List<SeqType> getSeqTypes() {
        return SeqTypeService.cellRangerAlignableSeqTypes
    }

    @Override
    AbstractMergedBamFile reallyCreateBamFile(
            MergingWorkPackage mergingWorkPackage,
            int identifier,
            Set<SeqTrack> seqTracks,
            ConfigPerProjectAndSeqType config,
            AbstractMergedBamFile baseBamFile = null) {
        return new SingleCellBamFile(
                workPackage        : mergingWorkPackage,
                identifier         : identifier,
                workDirectoryName  : SingleCellBamFile.buildWorkDirectoryName((CellRangerMergingWorkPackage) mergingWorkPackage, identifier),
                seqTracks          : seqTracks,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
        )
    }

    @Override
    ConfigPerProjectAndSeqType getConfig(MergingWorkPackage mwp) {
        CellRangerConfig config = CollectionUtils.<CellRangerConfig> atMostOneElement(
                CellRangerConfig.findAllByProjectAndPipelineAndSeqTypeAndObsoleteDate(
                        mwp.project,
                        mwp.pipeline,
                        mwp.seqType,
                        null,
                )
        )
        assert config: "Could not find a ${CellRangerConfig.simpleName} for ${mwp.project}, ${mwp.seqType} and ${mwp.pipeline}"
        return config
    }

    @Override
    RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
        return null
    }
}
