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

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

/**
 * {@link WorkflowNotification} for the data installation (fastq import) workflow.
 *
 * All content sections are derived from a single projection of the output seq tracks.
 * As for the workflow jobs, the methods rely on the surrounding transaction of the caller.
 */
@Component
class DataInstallationNotification extends AbstractWorkflowNotification {

    @Override
    String workflowName() {
        return DataInstallationWorkflow.WORKFLOW
    }

    @Override
    String pipelineAcknowledgementTemplate() {
        return null
    }

    @Override
    NotificationContent buildContent(Collection<WorkflowRun> workflowRuns) {
        List<SampleNotificationRow> rows = workflowNotificationContentService.fetchOutputSampleRows(
                workflowRuns, DataInstallationWorkflow.OUTPUT_FASTQ)
        Map<Long, Project> projects = workflowNotificationContentService.loadProjectsById(rows*.projectId)

        return new NotificationContent(
                notificationTexts: workflowNotificationContentService.buildSampleNotificationText(rows),
                notificationTextsByRunId: workflowNotificationContentService.buildSampleNotificationTextsByRunId(rows),
                guiUrls: buildGuiUrls(rows),
                filePatterns: buildFilePatterns(rows, projects),
        )
    }

    private Set<String> buildGuiUrls(Collection<SampleNotificationRow> rows) {
        return rows.collect { SampleNotificationRow row ->
            "${otpUrl}/sampleOverview/index?project=${StringUtils.encodeForUrl(row.projectName)}".toString()
        } as Set
    }

    @SuppressWarnings('GStringExpressionWithinString')
    private Set<String> buildFilePatterns(Collection<SampleNotificationRow> rows, Map<Long, Project> projects) {
        String seqTypeDir = '${SEQUENCING_TYPE_DIR}'
        return rows*.projectId.unique().collect { Long projectId ->
            "${projectService.getSequencingDirectory(projects[projectId])}/${seqTypeDir}".toString()
        } as Set
    }
}
