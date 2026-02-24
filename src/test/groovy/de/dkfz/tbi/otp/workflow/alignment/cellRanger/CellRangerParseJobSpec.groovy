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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerService
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.TableColumn
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.jobs.JobStage
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path

class CellRangerParseJobSpec extends Specification implements DataTest, CellRangerFactory, WorkflowSystemDomainFactory {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                CellRangerQualityAssessment,
                SingleCellBamFile,
                Workflow,
                WorkflowRun,
                WorkflowStep,
        ]
    }

    // Common test dependencies
    CellRangerParseJob cellRangerParseJob
    CellRangerWorkFileService mockWorkFileService
    ConcreteArtefactService mockConcreteArtefactService
    QcTrafficLightService mockQcTrafficLightService
    SingleCellBamFile bamFile
    WorkflowStep workflowStep
    WorkflowRun workflowRun
    Workflow workflow

    void setup() {
        // Create all mock objects in setup method
        mockWorkFileService = Mock(CellRangerWorkFileService)
        mockConcreteArtefactService = Mock(ConcreteArtefactService)
        mockQcTrafficLightService = Mock(QcTrafficLightService)
        bamFile = Mock(SingleCellBamFile)
        workflowStep = Mock(WorkflowStep)
        workflowRun = Mock(WorkflowRun)
        workflow = Mock(Workflow)

        // Create and configure the job instance
        cellRangerParseJob = new CellRangerParseJob()
        cellRangerParseJob.cellRangerService = Mock(CellRangerService)
        cellRangerParseJob.qcTrafficLightService = mockQcTrafficLightService
        cellRangerParseJob.cellRangerWorkFileService = mockWorkFileService
        cellRangerParseJob.concreteArtefactService = mockConcreteArtefactService

        // Setup common workflow chain
        workflowStep.workflowRun >> workflowRun
        workflowRun.workflow >> workflow
        workflow.name >> "Cell Ranger"
    }

    void 'test getQcFileToParse delegates to work file service'() {
        given:
        Path expectedPath = Path.of("expected_qa_file.csv")

        when:
        Path result = cellRangerParseJob.getQcFileToParse(workflowStep)

        then:
        1 * mockConcreteArtefactService.getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        1 * mockWorkFileService.getQualityAssessmentCsvFile(bamFile) >> expectedPath
        result == expectedPath
    }

    void "test getQcFileColumns returns QcColumn collection based on enum size"() {
        when:
        Collection<TableColumn> columns = cellRangerParseJob.qcColumns

        then:
        columns != null
        columns.size() == CellRangerService.MetricsSummaryCsvColumn.values().length
        columns.every { it instanceof TableColumn }
        columns.every { it.columnName != null && it.attributeName != null }
    }

    void "test getQcColumns maps enum values correctly"() {
        when:
        Collection<TableColumn> columns = cellRangerParseJob.qcColumns
        List<TableColumn> columnList = columns.toList()

        then:
        columns.size() == CellRangerService.MetricsSummaryCsvColumn.values().length

        // Verify that enum values are mapped to TsvColumn objects
        CellRangerService.MetricsSummaryCsvColumn.values().eachWithIndex { enumValue, index ->
            assert columnList.any { it.columnName == enumValue.columnName && it.attributeName == enumValue.attributeName }
        }
    }

    void "test processQualityAssessment throws NumberFormatException for invalid data"() {
        given:
        Map<String, String> parsedData = [
                "estimatedNumberOfCells": "not_a_number"
        ]

        when:
        cellRangerParseJob.processQualityAssessment(workflowStep, parsedData)

        then:
        1 * mockConcreteArtefactService.getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        NumberFormatException e = thrown()
        e.message.contains("Failed to parse 'not_a_number' for attribute 'estimatedNumberOfCells'")
    }

    void "test processQualityAssessment asserts bam file is not null"() {
        given:
        Map<String, String> parsedData = ["estimatedNumberOfCells": "10"]

        when:
        cellRangerParseJob.processQualityAssessment(workflowStep, parsedData)

        then:
        1 * mockConcreteArtefactService.getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> null
        AssertionError e = thrown()
        e.message.contains("The SingleCellBamFile associated with the WorkflowStep")
    }

    void "test getJobStage returns PARSE"() {
        expect:
        cellRangerParseJob.jobStage == JobStage.PARSE
    }

    void "test getQcColumns includes all expected CellRanger metrics"() {
        when:
        Collection<TableColumn> columns = cellRangerParseJob.qcColumns
        List<TableColumn> columnList = columns.toList()

        then:
        // Verify specific key CellRanger columns are present
        columnList.find { it.attributeName == "estimatedNumberOfCells" } != null
        columnList.find { it.attributeName == "meanReadsPerCell" } != null
        columnList.find { it.attributeName == "validBarcodes" } != null
        columnList.find { it.attributeName == "sequencingSaturation" } != null
        columnList.find { it.attributeName == "numberOfReads" } != null
        columnList.find { it.attributeName == "totalGenesDetected" } != null
    }
}
