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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeIndex
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.ToolName
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ToolNameService
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentAdditionalData
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentArtefactDataList
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentDeciderGroup
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentWorkPackageGroup

@Slf4j
@Transactional
@Component
class CellRangerDecider extends AbstractAlignmentDecider {

    final CellRangerWorkFileService cellRangerWorkFileService
    final ReferenceGenomeService referenceGenomeService
    final ToolNameService toolNameService

    CellRangerDecider(CellRangerWorkFileService cellRangerWorkFileService,
                      ReferenceGenomeService referenceGenomeService,
                      ToolNameService toolNameService) {
        this.cellRangerWorkFileService = cellRangerWorkFileService
        this.referenceGenomeService = referenceGenomeService
        this.toolNameService = toolNameService
    }

    @Override
    boolean requiresFastqcResults() {
        return false
    }

    @Override
    boolean allowsUnalignedCram() {
        return false
    }

    @Override
    String getWorkflowName() {
        return CellRangerWorkflow.WORKFLOW
    }

    @Override
    String getInputFastqRole() {
        return CellRangerWorkflow.INPUT_FASTQ
    }

    @Override
    // codenarc-disable-line
    String getInputFastqcRole() {
        return null
    }

    @Override
    String getOutputBamRole() {
        return CellRangerWorkflow.OUTPUT_BAM
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.CELL_RANGER
    }

    @Override
    AlignmentAdditionalData fetchAdditionalData(AlignmentArtefactDataList inputArtefactDataList,
                                                AlignmentArtefactDataList additionalArtefactDataList,
                                                Workflow workflow) {
        AlignmentAdditionalData additionalData = super.fetchAdditionalData(inputArtefactDataList, additionalArtefactDataList, workflow)
        ToolName toolName = toolNameService.findToolNameByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL)
        additionalData.referenceGenomeIndexMap = alignmentArtefactService.fetchReferenceGenomeIndexes(referenceGenomeService.list(), toolName)
        return additionalData
    }

    @Override
    CellRangerMergingWorkPackage findOrCreateMergingWorkPackage(AlignmentAdditionalData additionalData,
                                                                AlignmentWorkPackageGroup alignmentWorkPackageGroup,
                                                                Set<SeqTrack> seqTracks,
                                                                AlignmentDeciderGroup group,
                                                                ReferenceGenome referenceGenome,
                                                                WorkflowVersion version) {
        ReferenceGenomeIndex referenceGenomeIndex = additionalData.referenceGenomeIndexMap.get(referenceGenome).find()

        Set<CellRangerMergingWorkPackage> workPackages = additionalData.mergingWorkPackageMap[alignmentWorkPackageGroup] as Set<CellRangerMergingWorkPackage>

        CellRangerMergingWorkPackage workPackage = workPackages?.find {
            it.referenceGenomeIndex == referenceGenomeIndex &&
                    it.enforcedCells == null &&
                    it.expectedCells == null &&
                    it.programVersion == version.workflowVersion
        }

        if (workPackage) {
            if (workPackage.referenceGenome != referenceGenome) {
                throw new DeciderReferenceGenomeValidationException(workPackage.referenceGenome, referenceGenome, group)
            }
            SeqTrack seqTrack = seqTracks.first()
            Map<String, Entity> properties = MergingWorkPackage.getMergingProperties(seqTrack)
            if (!group.seqPlatformGroup) {
                properties.remove('seqPlatformGroup') //since seqPlatformGroup may be ignored, it should not part of the check
            }
            Map<String, Entity> nonMatchingProperties = properties.findAll { String key, Entity value ->
                value != workPackage[key]
            }
            if (nonMatchingProperties) {
                throw new DeciderMergingWorkPackageValidationException(nonMatchingProperties, group, workPackage, true)
            }
        } else {
            workPackage = new CellRangerMergingWorkPackage([
                    sample               : group.sample,
                    seqType              : group.seqType,
                    seqPlatformGroup     : group.seqPlatformGroup,
                    antibodyTarget       : group.antibodyTarget,
                    libraryPreparationKit: group.libraryPreparationKit,
                    referenceGenome      : referenceGenome,
                    pipeline             : additionalData.pipeline,
                    referenceGenomeIndex : referenceGenomeIndex,
            ])
        }

        return workPackage
    }

    @Override
    SingleCellBamFile createBamFileWithoutFlush(Map properties) {
        int identifier = SingleCellBamFile.nextIdentifier(properties.workPackage as CellRangerMergingWorkPackage)
        properties["identifier"] = identifier
        properties["workDirectoryName"] = cellRangerWorkFileService.buildWorkDirectoryName(properties.workPackage as CellRangerMergingWorkPackage, identifier)
        return new SingleCellBamFile(properties).save(flush: false, deepValidate: false)
    }
}
