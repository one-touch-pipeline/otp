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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Files
import java.nio.file.Path

@Integration
@Rollback
class CellRangerParseJobIntegrationSpec extends Specification implements DomainFactoryCore, CellRangerFactory, WorkflowSystemDomainFactory {

    CellRangerParseJob cellRangerParseJob

    WorkflowRun workflowRun
    WorkflowStep workflowStep
    SingleCellBamFile bamFile

    @TempDir
    Path tempDir

    void setupData() {
        Workflow existingWorkflow = Workflow.findByName(CellRangerWorkflow.WORKFLOW) ?: createWorkflow([
                name: CellRangerWorkflow.WORKFLOW
        ])
        workflowRun = createWorkflowRun([
                workflow: existingWorkflow
        ])
        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                producedBy: workflowRun,
                outputRole: AlignmentWorkflow.OUTPUT_BAM,
        ])

        workflowStep = createWorkflowStep([
                workflowRun: workflowRun
        ])
        bamFile = createBamFile([
                workflowArtefact: workflowArtefact
        ])

        // Attach a real work folder under tempDir so work file service resolves paths there
        BaseFolder baseFolder = new BaseFolder(path: tempDir.toString(), writable: true)
        baseFolder.save(flush: true)
        FilestoreService filestoreService = cellRangerParseJob.cellRangerWorkFileService.filestoreService
        WorkFolder workFolder = filestoreService.createWorkFolder(baseFolder)
        filestoreService.attachWorkFolder(workflowRun, workFolder)
    }

    void "test processQualityAssessment saves QA data correctly to database"() {
        given:
        setupData()
        Path qaPath = cellRangerParseJob.cellRangerWorkFileService.getQualityAssessmentCsvFile(bamFile)
        Files.createDirectories(qaPath.parent)
        CreateFileHelper.createFile(qaPath, createValidCsvContent())
        assert Files.exists(qaPath): "Test CSV file was not created successfully."

        when:
        cellRangerParseJob.parseOutputs(workflowStep)

        then:
        CellRangerQualityAssessment qa = CellRangerQualityAssessment.findByAbstractBamFile(bamFile)
        assert qa != null
        assert qa.estimatedNumberOfCells == 1500.0
        assert qa.meanReadsPerCell == 25000.0
        assert qa.validBarcodes == 90.5

        assert qa.bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.UNKNOWN

        when:
        cellRangerParseJob.parseOutputs(workflowStep)

        then:
        assert CellRangerQualityAssessment.findAllByAbstractBamFile(bamFile).size() == 1
    }

    void "test parseOutputs with malformed file throws exception"() {
        given:
        setupData()
        Path qaPath = cellRangerParseJob.cellRangerWorkFileService.getQualityAssessmentCsvFile(bamFile)
        Files.createDirectories(qaPath.parent)
        CreateFileHelper.createFile(qaPath, "Wrong,Headers,Here\n1,2,3")

        when:
        cellRangerParseJob.parseOutputs(workflowStep)

        then:
        AssertionError e = thrown()
        assert e.message.contains("can not be found in")
    }

    void "test parseOutputs with invalid numeric data throws NumberFormatException"() {
        given:
        setupData()
        Path qaPath = cellRangerParseJob.cellRangerWorkFileService.getQualityAssessmentCsvFile(bamFile)
        Files.createDirectories(qaPath.parent)
        CreateFileHelper.createFile(qaPath, createInvalidNumericCsvContent())
        assert Files.exists(qaPath): "Test CSV file was not created successfully."

        when:
        cellRangerParseJob.parseOutputs(workflowStep)

        then:
        NumberFormatException e = thrown()
        assert e.message.contains("Failed to parse")
    }

    void "test parseOutputs with empty file throws exception and no qa created"() {
        given:
        setupData()
        Path qaPath = cellRangerParseJob.cellRangerWorkFileService.getQualityAssessmentCsvFile(bamFile)
        Files.createDirectories(qaPath.parent)
        CreateFileHelper.createFile(qaPath, "")
        assert Files.exists(qaPath): "Test CSV file was not created successfully."

        when:
        cellRangerParseJob.parseOutputs(workflowStep)

        then:
        AssertionError e = thrown()
        e.message.contains("No data rows found")
        CellRangerQualityAssessment.findByAbstractBamFile(bamFile) == null
    }

    private String createValidCsvContent() {
        // Create CSV content with all required CellRanger columns
        StringBuilder header = new StringBuilder()
        StringBuilder values = new StringBuilder()

        CellRangerService.MetricsSummaryCsvColumn.values().eachWithIndex { column, index ->
            if (index > 0) {
                header.append(",")
                values.append(",")
            }
            header.append(column.columnName)

            // Provide test values for key metrics - Note: CellRanger CSV uses commas in numbers
            switch (column) {
                case CellRangerService.MetricsSummaryCsvColumn.ESTIMATED_NUMBER_OF_CELLS:
                    values.append("\"1,500\"")  // Quoted to handle comma in number
                    break
                case CellRangerService.MetricsSummaryCsvColumn.MEAN_READS_PER_CELL:
                    values.append("\"25,000\"")  // Quoted to handle comma in number
                    break
                case CellRangerService.MetricsSummaryCsvColumn.VALID_BARCODES:
                    values.append("90.5%")  // Percentage format
                    break
                case CellRangerService.MetricsSummaryCsvColumn.SEQUENCING_SATURATION:
                    values.append("75.2%")  // Percentage format
                    break
                case CellRangerService.MetricsSummaryCsvColumn.Q30_BASES_IN_BARCODE:
                case CellRangerService.MetricsSummaryCsvColumn.Q30_BASES_IN_RNA_READ:
                case CellRangerService.MetricsSummaryCsvColumn.Q30_BASES_IN_UMI:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_TO_GENOME:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_CONFIDENTLY_TO_GENOME:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_CONFIDENTLY_TO_INTERGENIC_REGIONS:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_CONFIDENTLY_TO_INTRONIC_REGIONS:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_CONFIDENTLY_TO_EXONIC_REGIONS:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_CONFIDENTLY_TO_TRANSCRIPTOME:
                case CellRangerService.MetricsSummaryCsvColumn.READS_MAPPED_ANTISENSE_TO_GENE:
                case CellRangerService.MetricsSummaryCsvColumn.FRACTION_READS_IN_CELLS:
                    values.append("85.5%")  // Percentage format for mapping rates
                    break
                case CellRangerService.MetricsSummaryCsvColumn.NUMBER_OF_READS:
                    values.append("\"50,000,000\"")  // Large number with commas
                    break
                case CellRangerService.MetricsSummaryCsvColumn.TOTAL_GENES_DETECTED:
                    values.append("\"18,500\"")  // Number with comma
                    break
                default:
                    values.append("100.0")  // Default numeric value
                    break
            }
        }

        return "${header}\n${values}"
    }

    private String createInvalidNumericCsvContent() {
        // Create CSV with valid headers but invalid numeric data
        StringBuilder header = new StringBuilder()
        StringBuilder values = new StringBuilder()

        CellRangerService.MetricsSummaryCsvColumn.values().eachWithIndex { column, index ->
            if (index > 0) {
                header.append(",")
                values.append(",")
            }
            header.append(column.columnName)

            // First column gets invalid data to trigger NumberFormatException
            if (index == 0) {
                values.append("not_a_number")
            } else {
                values.append("100.0")
            }
        }

        return "${header}\n${values}"
    }
}
