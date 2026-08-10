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
package de.dkfz.tbi.otp.workflow.notification

import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.nio.file.Paths

/**
 * Verifies that each {@link WorkflowNotification} builds the notification content from the projection rows and the
 * batch loaded projects provided by the {@link WorkflowNotificationContentService}. The projection queries and the
 * text building itself are covered by {@link WorkflowNotificationContentServiceSpec} and its integration spec.
 */
class WorkflowNotificationSpec extends Specification {

    private static final String OTP_URL = "http://otp"
    private static final String SEQUENCING_DIR = "/root/project/sequencing"
    private static final long PROJECT_ID = 1L
    private static final String PROJECT_NAME = "projectName"
    private static final long SEQ_TYPE_ID = 42L

    private Project project
    private WorkflowRun workflowRun

    void setup() {
        project = Mock(Project) {
            _ * getName() >> PROJECT_NAME
        }
        workflowRun = Mock(WorkflowRun)
    }

    void "DataInstallationNotification, builds workflow name, text, GUI URL and file pattern from the output sample rows"() {
        given:
        SampleNotificationRow row = new SampleNotificationRow("pid", "tumor", "WGS PAIRED bulk", "sample1", PROJECT_ID, PROJECT_NAME, 11L)

        WorkflowNotificationContentService contentService = Mock(WorkflowNotificationContentService)
        contentService.fetchOutputSampleRows([workflowRun], DataInstallationWorkflow.OUTPUT_FASTQ) >> [row]
        contentService.loadProjectsById([PROJECT_ID]) >> [(PROJECT_ID): project]
        contentService.buildSampleNotificationText([row]) >> (["sample text"] as Set)
        contentService.buildSampleNotificationTextsByRunId([row]) >> ([11L: "sample text"])

        DataInstallationNotification notification = new DataInstallationNotification(
                configService: Stub(ConfigService) { getConfigServerUrl() >> OTP_URL },
                projectService: Stub(ProjectService) { getSequencingDirectory(project) >> Paths.get(SEQUENCING_DIR) },
                workflowNotificationContentService: contentService,
        )

        when:
        NotificationContent content = notification.buildContent([workflowRun])

        then:
        notification.workflowName() == DataInstallationWorkflow.WORKFLOW
        notification.pipelineAcknowledgementTemplate() == null
        content.notificationTexts == ["sample text"] as Set
        content.notificationTextsByRunId == [11L: "sample text"]
        content.guiUrls == ["${OTP_URL}/sampleOverview/index?project=${PROJECT_NAME}".toString()] as Set
        content.filePatterns == ["${SEQUENCING_DIR}/\${SEQUENCING_TYPE_DIR}".toString()] as Set /* codenarc-disable-line GStringExpressionWithinString */
    }

    void "AbstractAlignmentNotification, builds text from the input sample rows and GUI URL and file patterns from the output BAM rows"() {
        given:
        SampleNotificationRow sampleRow = new SampleNotificationRow("pid", "tumor", "WGS PAIRED bulk", "sample1", PROJECT_ID, PROJECT_NAME, 11L)
        AlignmentBamNotificationRow bamRow = new AlignmentBamNotificationRow(PROJECT_ID, PROJECT_NAME, SEQ_TYPE_ID, "whole_genome_sequencing", false, "paired")

        WorkflowNotificationContentService contentService = Mock(WorkflowNotificationContentService)
        contentService.fetchInputSampleRows([workflowRun], AlignmentWorkflow.INPUT_FASTQ) >> [sampleRow]
        contentService.fetchOutputBamRows([workflowRun], AlignmentWorkflow.OUTPUT_BAM) >> [bamRow]
        contentService.loadProjectsById([PROJECT_ID]) >> [(PROJECT_ID): project]
        contentService.buildSampleNotificationText([sampleRow]) >> (["sample text"] as Set)
        contentService.buildSampleNotificationTextsByRunId([sampleRow]) >> ([11L: "sample text"])

        PanCancerAlignmentNotification notification = new PanCancerAlignmentNotification(
                configService: Stub(ConfigService) { getConfigServerUrl() >> OTP_URL },
                projectService: Stub(ProjectService) { getSequencingDirectory(project) >> Paths.get(SEQUENCING_DIR) },
                workflowNotificationContentService: contentService,
        )

        when:
        NotificationContent content = notification.buildContent([workflowRun])

        then:
        notification.workflowName() == PanCancerWorkflow.WORKFLOW
        notification.pipelineAcknowledgementTemplate() == "notification.template.references.alignment.pancancer"
        content.notificationTexts == ["sample text"] as Set
        content.notificationTextsByRunId == [11L: "sample text"]
        content.guiUrls == ["${OTP_URL}/alignmentQualityOverview/index?project=${PROJECT_NAME}&seqType=${SEQ_TYPE_ID}".toString()] as Set
        content.filePatterns == ["${SEQUENCING_DIR}/whole_genome_sequencing/view-by-pid/\${PID}/\${SAMPLE_TYPE}/paired/merged-alignment".toString()] as Set /* codenarc-disable-line GStringExpressionWithinString */
    }

    void "AbstractAnalysisNotification, builds text, GUI URL and file pattern from the output sample pair rows"() {
        given:
        SamplePairNotificationRow row = new SamplePairNotificationRow("pid", "tumor", "control", "WGS PAIRED bulk", PROJECT_ID, PROJECT_NAME, 11L)

        WorkflowNotificationContentService contentService = Mock(WorkflowNotificationContentService)
        contentService.fetchSamplePairRows([workflowRun], AbstractAnalysisWorkflow.ANALYSIS_OUTPUT) >> [row]
        contentService.loadProjectsById([PROJECT_ID]) >> [(PROJECT_ID): project]
        contentService.buildSamplePairNotificationText([row]) >> (["sample pair text"] as Set)
        contentService.buildSamplePairNotificationTextsByRunId([row]) >> ([11L: "sample pair text"])

        RoddySnvCallingNotification notification = new RoddySnvCallingNotification(
                configService: Stub(ConfigService) { getConfigServerUrl() >> OTP_URL },
                projectService: Stub(ProjectService) { getSequencingDirectory(project) >> Paths.get(SEQUENCING_DIR) },
                workflowNotificationContentService: contentService,
        )

        when:
        NotificationContent content = notification.buildContent([workflowRun])

        then:
        notification.workflowName() == SnvWorkflow.WORKFLOW
        notification.pipelineAcknowledgementTemplate() == "notification.template.references.snv"
        content.notificationTexts == ["sample pair text"] as Set
        content.notificationTextsByRunId == [11L: "sample pair text"]
        content.guiUrls == ["${OTP_URL}/snv/results?project=${PROJECT_NAME}".toString()] as Set
        content.filePatterns == ["${SEQUENCING_DIR}/\${SEQUENCING_TYPE_DIR}/view-by-pid/\${PID}/snv_results/paired/\${SAMPLE_TYPE1}_\${SAMPLE_TYPE2}".toString()] as Set /* codenarc-disable-line GStringExpressionWithinString */
    }
}
