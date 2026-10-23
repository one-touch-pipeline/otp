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

import de.dkfz.tbi.otp.dataprocessing.SingleCellAlignmentInfo
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerAlignmentInfoService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

class CellRangerCreateNotificationJobSpec extends Specification implements DataTest, CellRangerFactory, WorkflowSystemDomainFactory {

    private static final String PROCESSING_TEXT = "processing text"
    private static final String SINGLE_CELL_TEXT = "single cell text"

    private CellRangerCreateNotificationJob job
    private MessageSourceService messageSourceService
    private CellRangerAlignmentInfoService cellRangerAlignmentInfoService
    private WorkflowRun run
    private WorkflowStep workflowStep
    private SingleCellBamFile bamFile

    private final SingleCellAlignmentInfo alignmentInfo = new SingleCellAlignmentInfo(
            alignmentProgram: "cellranger",
            alignmentParameter: "",
            programVersion: "cellranger/7.1.0",
    )

    @Override
    Class[] getDomainClassesToMock() {
        return [
                CellRangerMergingWorkPackage,
                FastqFile,
                FastqImportInstance,
                ReferenceGenomeProjectSeqType,
                SingleCellBamFile,
                WorkflowRun,
                WorkflowStep,
        ]
    }

    void setup() {
        run = createWorkflowRun([workflow: findOrCreateWorkflow(CellRangerWorkflow.WORKFLOW)])
        workflowStep = createWorkflowStep([workflowRun: run])
        bamFile = createBamFile()

        messageSourceService = Mock(MessageSourceService)
        cellRangerAlignmentInfoService = Mock(CellRangerAlignmentInfoService)

        job = new CellRangerCreateNotificationJob(messageSourceService, cellRangerAlignmentInfoService)
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        }
        job.logService = Mock(LogService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
    }

    void "createNotificationText, should render the alignment info of the run"() {
        when:
        String text = job.createNotificationText(workflowStep)

        then:
        1 * cellRangerAlignmentInfoService.getAlignmentInfo(run) >> alignmentInfo
        1 * messageSourceService.createMessage("notification.template.alignment.processing", [
                seqType           : bamFile.seqType.displayNameWithLibraryLayout,
                referenceGenome   : bamFile.referenceGenome,
                alignmentProgram  : "cellranger",
                alignmentParameter: "",
        ]) >> PROCESSING_TEXT
        1 * messageSourceService.createMessage("notification.template.alignment.processing.singleCell", [
                programVersion: "cellranger/7.1.0",
                localCores    : null,
                localMemory   : null,
        ]) >> SINGLE_CELL_TEXT
        0 * messageSourceService._

        and:
        text == PROCESSING_TEXT + SINGLE_CELL_TEXT
    }

    void "execute, should save the notification text in the workflow run, log it and mark the step as successful"() {
        given:
        cellRangerAlignmentInfoService.getAlignmentInfo(run) >> alignmentInfo
        messageSourceService.createMessage("notification.template.alignment.processing", _) >> PROCESSING_TEXT
        messageSourceService.createMessage("notification.template.alignment.processing.singleCell", _) >> SINGLE_CELL_TEXT

        when:
        job.execute(workflowStep)

        then:
        run.refresh().notificationText == PROCESSING_TEXT + SINGLE_CELL_TEXT

        and:
        1 * job.logService.addSimpleLogEntry(workflowStep, "Notification has been generated")
        1 * job.logService.addSimpleLogEntry(workflowStep, PROCESSING_TEXT + SINGLE_CELL_TEXT)

        then:
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "execute, when the notification text can not be created, then do not mark the step as successful"() {
        given:
        cellRangerAlignmentInfoService.getAlignmentInfo(run) >> { throw new IllegalStateException("no info") }

        when:
        job.execute(workflowStep)

        then:
        thrown(IllegalStateException)
        run.refresh().notificationText == null
        0 * job.workflowStateChangeService._
    }
}
