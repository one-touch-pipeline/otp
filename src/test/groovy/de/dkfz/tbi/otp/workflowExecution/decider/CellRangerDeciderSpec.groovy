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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentWorkFileServiceFactoryService
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeIndex
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.ToolName
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ToolNameService
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowService
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion
import de.dkfz.tbi.otp.workflowExecution.decider.alignment.AlignmentAdditionalData

import java.nio.file.Path

class CellRangerDeciderSpec extends AbstractAlignmentDeciderSpec implements CellRangerFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                SingleCellBamFile,
        ]
    }

    void setup() {
        decider = new CellRangerDecider(
                Mock(CellRangerWorkFileService),
                Mock(ReferenceGenomeService),
                Mock(ToolNameService)
        )
        useFastqcCount = 0
    }

    @Override
    protected int getFetchReferenceGenomeIndexesCount() {
        return 1
    }

    @Override
    protected Map<ReferenceGenome, Set<ReferenceGenomeIndex>> getReferenceGenomeIndexMap(ReferenceGenome referenceGenome) {
        ReferenceGenomeIndex referenceGenomeIndex = findOrCreateCellRangerReferenceGenomeIndex(referenceGenome)
        return [(referenceGenome): [referenceGenomeIndex] as Set]
    }

    @Override
    protected void configureReferenceGenomeIndexMap(AlignmentAdditionalData additionalData, Collection<ReferenceGenome> referenceGenomes) {
        additionalData.referenceGenomeIndexMap = referenceGenomes.collectEntries { ReferenceGenome referenceGenome ->
            getReferenceGenomeIndexMap(referenceGenome)
        }
    }

    @Override
    protected Map<String, ?> getCreateBamFilePropertiesForCreateWorkflowRunsAndOutputArtefacts() {
        return [:]
    }

    @Override
    protected Map<String, ?> getCreateMergingWorkPackagePropertiesForCreateWorkflowRunsAndOutputArtefacts(
            ReferenceGenome referenceGenome, WorkflowVersion workflowVersion, Map<String, ?> values) {
        return getCreateMergingWorkPackagePropertiesForFindOrCreateMergingWorkPackageTests(referenceGenome, workflowVersion)
    }

    @Override
    protected Map<String, ?> getCreateMergingWorkPackagePropertiesForFindOrCreateMergingWorkPackageTests(
            ReferenceGenome referenceGenome, WorkflowVersion workflowVersion) {
        ReferenceGenomeIndex referenceGenomeIndex = getReferenceGenomeIndexMap(referenceGenome)[referenceGenome].first()
        return [
                referenceGenomeIndex: referenceGenomeIndex,
        ]
    }

    @Override
    protected boolean reusesExistingMergingWorkPackageWithoutBamFile() {
        return false
    }

    @Override
    protected boolean reusesExistingMergingWorkPackageWithOtherBamFileOfDifferentVersion() {
        return false
    }

    @Override
    protected Pipeline getAdditionalDataPipelineForCreateWorkflowRunsAndOutputArtefacts() {
        return findOrCreatePipeline()
    }

    @Override
    protected boolean supportsReferenceGenomeMismatchValidation() {
        return false
    }

    @Override
    protected void configureFetchAdditionalDataDependencies(ReferenceGenome referenceGenome) {
        ToolName toolName = referenceGenome.referenceGenomeIndexes.first().toolName
        1 * decider.referenceGenomeService.list() >> [referenceGenome]
        1 * decider.toolNameService.findToolNameByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL) >> toolName
    }

    @Override
    protected Pipeline findPipeline() {
        return CellRangerFactory.super.findOrCreatePipeline()
    }

    @Override
    void createServicesForCreateWorkflowRunsAndOutputArtefacts(WorkflowVersion workflowVersion, SeqTrack seqTrack) {
        super.createServicesForCreateWorkflowRunsAndOutputArtefacts(workflowVersion, seqTrack)
        _ * decider.cellRangerWorkFileService.buildWorkDirectoryName(_, _) >> "work_directory_${nextId}"
        _ * decider.cellRangerWorkFileService.getDirectoryPath(_) >> Path.of("/tmp", "cellranger")
        decider.alignmentWorkFileServiceFactoryService = Mock(AlignmentWorkFileServiceFactoryService) {
            1 * getService(_) >> decider.cellRangerWorkFileService
        }
    }

    @Override
    void createDataForCreateWorkflowRunsAndOutputArtefactsDomains(Map adaption) {
        super.createDataForCreateWorkflowRunsAndOutputArtefactsDomains(adaption)
        createReferenceGenomeIndex(
                referenceGenome: referenceGenome,
                toolName: createToolName(name: 'CELL_RANGER', type: ToolName.Type.SINGLE_CELL, path: "cellranger")
        )
    }

    void "requiresFastqcResults"() {
        expect:
        decider.requiresFastqcResults() == false
    }

    void "getWorkflowName"() {
        expect:
        decider.workflowName == CellRangerWorkflow.WORKFLOW
    }

    void "getInputFastqRole"() {
        expect:
        decider.inputFastqRole == CellRangerWorkflow.INPUT_FASTQ
    }

    void "getInputFastqcRole"() {
        expect:
        decider.inputFastqcRole == null
    }

    void "getOutputBamRole"() {
        expect:
        decider.outputBamRole == CellRangerWorkflow.OUTPUT_BAM
    }

    void "getPipelineName"() {
        expect:
        decider.pipelineName == Pipeline.Name.CELL_RANGER
    }

    void "getWorkflow"() {
        given:
        decider.workflowService = new WorkflowService()
        createWorkflow(name: CellRangerWorkflow.WORKFLOW)

        expect:
        decider.workflow.name == CellRangerWorkflow.WORKFLOW
    }

    void "getSupportedInputArtefactTypes"() {
        expect:
        TestCase.assertContainSame(decider.supportedInputArtefactTypes, [
                ArtefactType.FASTQ,
        ])
    }
}
