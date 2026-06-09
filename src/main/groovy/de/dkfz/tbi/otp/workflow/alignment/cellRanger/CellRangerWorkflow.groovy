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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentFragmentJob
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.LinearWorkflow

/**
 * represents the CellRanger Workflow
 */
@Component
@Slf4j
class CellRangerWorkflow extends AlignmentWorkflow implements LinearWorkflow {

    public static final String WORKFLOW = "Cell Ranger"

    @Autowired
    CellRangerWorkFileService cellRangerWorkFileService

    @Override
    List<Class<? extends Job>> getJobList() {
        return [
                SkipForEmptyRawSequenceFileJob,
                AlignmentFragmentJob,
                CellRangerCheckFragmentKeysJob,
                CellRangerConditionalFailJob,
                AttachUuidJob,
                CellRangerPrepareJob,
                CellRangerExecuteJob,
                CellRangerValidationJob,
                CellRangerParseJob,
                CellRangerCleanUpJob,
                CellRangerAclCleanUpJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                CellRangerLinkJob,
                CellRangerFinishJob,
        ]
    }

    @Override
    @SuppressWarnings('ImplicitReturnStatement')
    Artefact createCopyOfArtefact(Artefact artefact) {
        SingleCellBamFile singleCellBamFile = artefact as SingleCellBamFile
        singleCellBamFile.withdrawn = true
        singleCellBamFile.save(flush: true)

        CellRangerMergingWorkPackage cellRangerMergingWorkPackage = singleCellBamFile.mergingWorkPackage
        int identifier = SingleCellBamFile.nextIdentifier(cellRangerMergingWorkPackage)

        SingleCellBamFile outputSingleCellBamFile = new SingleCellBamFile([
                workPackage        : cellRangerMergingWorkPackage,
                identifier         : identifier,
                workDirectoryName  : cellRangerWorkFileService.buildWorkDirectoryName(cellRangerMergingWorkPackage, identifier),
                seqTracks          : singleCellBamFile.seqTracks.collect() as Set,
                numberOfMergedLanes: singleCellBamFile.containedSeqTracks.size(),
        ]).save(flush: true)

        return outputSingleCellBamFile
    }

    @Override
    void reconnectDependencies(Artefact artefact, Artefact newArtefact, String role) {
    }

    final String userDocumentation = "notification.template.references.alignment.cellRanger"
}
