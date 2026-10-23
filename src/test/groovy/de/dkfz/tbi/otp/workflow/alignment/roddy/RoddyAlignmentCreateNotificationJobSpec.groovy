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
package de.dkfz.tbi.otp.workflow.alignment.roddy

import com.fasterxml.jackson.core.JsonParseException
import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

class RoddyAlignmentCreateNotificationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    private static final String PROCESSING_TEXT = "processing text"
    private static final String RODDY_TEXT = "roddy text"

    private RoddyAlignmentCreateNotificationJob job
    private WorkflowRun run
    private WorkflowStep workflowStep
    private RoddyBamFile bamFile

    private final RoddyAlignmentInfo alignmentInfo = new RoddyAlignmentInfo(
            alignmentProgram: "BWA Version 0.7.15",
            alignmentParameter: "-T 0",
            programVersion: "1.2.73-204",
            mergeCommand: "Sambamba Version 0.6.5",
            mergeOptions: "-SOME_OPTS",
            samToolsCommand: "Version 1.0",
    )

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                WorkflowRun,
        ]
    }

    private void setupData(String workflowName, String combinedConfig = "{}") {
        run = createWorkflowRun([
                workflow      : createWorkflow([name: workflowName]),
                combinedConfig: combinedConfig,
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
        bamFile = createBamFile()

        job = new RoddyAlignmentCreateNotificationJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, AlignmentWorkflow.OUTPUT_BAM) >> bamFile
        }
        job.roddyAlignmentInfoService = Mock(RoddyAlignmentInfoService)
        job.messageSourceService = Mock(MessageSourceService)
        job.logService = Mock(LogService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
    }

    @Unroll
    void "createNotificationText, when workflow is #workflowName, then render the alignment info of the run"() {
        given:
        setupData(workflowName)

        when:
        String text = job.createNotificationText(workflowStep)

        then:
        1 * job.roddyAlignmentInfoService.getAlignmentInfo(run, bamFile) >> alignmentInfo
        1 * job.messageSourceService.createMessage("notification.template.alignment.processing", [
                seqType           : bamFile.seqType.displayNameWithLibraryLayout,
                referenceGenome   : bamFile.referenceGenome,
                alignmentProgram  : "BWA Version 0.7.15",
                alignmentParameter: "-T 0",
        ]) >> PROCESSING_TEXT
        1 * job.messageSourceService.createMessage("notification.template.alignment.processing.roddy", [
                mergingProgram  : "Sambamba Version 0.6.5",
                mergingParameter: "-SOME_OPTS",
                samtoolsProgram : "Version 1.0",
                programVersion  : "1.2.73-204",
        ]) >> RODDY_TEXT
        0 * job.messageSourceService._

        and:
        text == PROCESSING_TEXT + RODDY_TEXT

        where:
        workflowName << [
                PanCancerWorkflow.WORKFLOW,
                WgbsWorkflow.WORKFLOW,
                RnaAlignmentWorkflow.WORKFLOW,
        ]
    }

    @Unroll
    void "createNotificationText, when combined config is '#combinedConfig', then throw IllegalArgumentException"() {
        given:
        setupData(PanCancerWorkflow.WORKFLOW, combinedConfig)

        when:
        job.createNotificationText(workflowStep)

        then:
        0 * job.roddyAlignmentInfoService._
        IllegalArgumentException e = thrown()
        e.message.contains("CombinedConfig")

        where:
        combinedConfig << [null, ""]
    }

    void "createNotificationText, when combined config can not be parsed, then throw IllegalArgumentException with cause"() {
        given:
        setupData(PanCancerWorkflow.WORKFLOW)
        JsonParseException cause = new JsonParseException(null, "invalid")

        when:
        job.createNotificationText(workflowStep)

        then:
        1 * job.roddyAlignmentInfoService.getAlignmentInfo(run, bamFile) >> { throw cause }
        IllegalArgumentException e = thrown()
        e.cause == cause
    }

    void "createNotificationText, when the alignment info can not be extracted, then propagate the ParsingException"() {
        given:
        setupData(PanCancerWorkflow.WORKFLOW)

        when:
        job.createNotificationText(workflowStep)

        then:
        1 * job.roddyAlignmentInfoService.getAlignmentInfo(run, bamFile) >> { throw new ParsingException("missing") }
        thrown(ParsingException)
    }

    void "execute, should save the notification text in the workflow run, log it and mark the step as successful"() {
        given:
        setupData(PanCancerWorkflow.WORKFLOW)
        job.roddyAlignmentInfoService.getAlignmentInfo(run, bamFile) >> alignmentInfo
        job.messageSourceService.createMessage("notification.template.alignment.processing", _) >> PROCESSING_TEXT
        job.messageSourceService.createMessage("notification.template.alignment.processing.roddy", _) >> RODDY_TEXT

        when:
        job.execute(workflowStep)

        then:
        run.refresh().notificationText == PROCESSING_TEXT + RODDY_TEXT

        and:
        1 * job.logService.addSimpleLogEntry(workflowStep, "Notification has been generated")
        1 * job.logService.addSimpleLogEntry(workflowStep, PROCESSING_TEXT + RODDY_TEXT)

        then:
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "execute, when the notification text can not be created, then do not mark the step as successful"() {
        given:
        setupData(PanCancerWorkflow.WORKFLOW)
        job.roddyAlignmentInfoService.getAlignmentInfo(run, bamFile) >> { throw new ParsingException("missing") }

        when:
        job.execute(workflowStep)

        then:
        thrown(ParsingException)
        0 * job.workflowStateChangeService._
    }
}
